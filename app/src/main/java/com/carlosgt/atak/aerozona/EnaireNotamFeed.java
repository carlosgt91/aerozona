package com.carlosgt.atak.aerozona;

import com.carlosgt.atak.aerozona.net.Http;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * UAS-relevant NOTAM areas from ENAIRE's servAIS (DINAMIC/UAS_NOTAM_SRV_DATA_V1).
 *
 * <p>National and small: 780 areas, 332 KB, half a second (2026-11-07), so the whole
 * country is fetched in one query, kept on disk, and refreshed every
 * {@link #MAX_AGE_MS}. ENAIRE allows this use but names Ícaro XXI as the official NOTAM
 * source, which every details page says.
 *
 * <p>Plain Java, no ATAK types: runs on a desktop JVM.
 */
public final class EnaireNotamFeed {

    static final String URL = "https://servais.enaire.es/insigniads/rest/services/DINAMIC/"
            + "UAS_NOTAM_SRV_DATA_V1/MapServer/1/query";
    private static final String FIELDS = "OBJECTID,notamId,itemA,itemB,itemC,itemD,itemE,"
            + "itemF,itemG,qcode,areaSactaName,DESCRIPTION";
    public static final long MAX_AGE_MS = 30L * 60L * 1000L;
    private static final String FILE = "uas_notam.json.gz";

    private EnaireNotamFeed() {
    }

    public static boolean stale(File dir) {
        final File f = new File(dir, FILE);
        return !f.isFile() || System.currentTimeMillis() - f.lastModified() >= MAX_AGE_MS;
    }

    /** Every NOTAM area not yet expired. Network when stale, else disk; disk on failure. */
    public static List<Airspace> load(File dir, boolean allowNetwork) throws IOException {
        final File f = new File(dir, FILE);
        if (allowNetwork && stale(dir)) {
            try {
                final byte[] body = fetch();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new IOException("cannot create " + dir);
                final File tmp = new File(dir, FILE + ".tmp");
                try (OutputStream out = new GZIPOutputStream(new FileOutputStream(tmp))) {
                    out.write(body);
                }
                if (!tmp.renameTo(f))
                    throw new IOException("cannot store " + f);
                return parse(body, System.currentTimeMillis());
            } catch (IOException e) {
                if (!f.isFile())
                    throw e;
            }
        }
        if (!f.isFile())
            throw new IOException("not downloaded");
        return parse(EnaireUasFeed.readGz(f), System.currentTimeMillis());
    }

    static byte[] fetch() throws IOException {
        final String u = URL + "?f=json&where=1%3D1&outSR=4326&returnGeometry=true"
                + "&maxAllowableOffset=0.0001&outFields=" + FIELDS.replace(",", "%2C");
        final byte[] body = Http.get(u);
        try {
            final JSONObject root = new JSONObject(new String(body, Charset.forName("UTF-8")));
            if (root.optJSONObject("error") != null)
                throw new IOException(root.optJSONObject("error").optString("message",
                        "ENAIRE refused the NOTAM query"));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("ENAIRE NOTAM answer not readable");
        }
        return body;
    }

    static List<Airspace> parse(byte[] body, long nowMs) throws IOException {
        final JSONObject root;
        try {
            root = new JSONObject(new String(body, Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new IOException("stored NOTAMs not readable");
        }
        final List<Airspace> out = new ArrayList<>();
        final JSONArray fs = root.optJSONArray("features");
        if (fs == null)
            return out;
        for (int i = 0; i < fs.length(); i++) {
            final Airspace a = one(fs.optJSONObject(i));
            if (a == null || !a.drawable())
                continue;
            if (a.validToMs > 0 && a.validToMs < nowMs)
                continue;
            out.add(a);
        }
        return out;
    }

    static Airspace one(JSONObject f) {
        if (f == null)
            return null;
        final JSONObject at = f.optJSONObject("attributes");
        final JSONObject geom = f.optJSONObject("geometry");
        final JSONArray rings = geom == null ? null : geom.optJSONArray("rings");
        if (at == null || rings == null)
            return null;
        final Airspace a = new Airspace();
        a.source = Airspace.SOURCE_ENAIRE_NOTAM;
        a.id = "notam:" + text(at, "OBJECTID");
        a.ident = text(at, "notamId");
        a.name = text(at, "areaSactaName");
        a.uasType = text(at, "qcode");
        a.validFromMs = number(at, "itemB");
        a.validToMs = number(at, "itemC");
        a.workHours = text(at, "itemD");
        a.message = text(at, "itemE");
        a.localType = text(at, "itemA");
        limit(a.floor, text(at, "itemF"));
        limit(a.ceiling, text(at, "itemG"));
        AirspaceFeed.readRings(a.parts, rings);
        return a;
    }

    private static final Pattern LIMIT = Pattern.compile(
            "^(\\d+)\\s*(M|FT)\\s*(AGL|AMSL|MSL)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FL = Pattern.compile("^FL\\s*(\\d+)$",
            Pattern.CASE_INSENSITIVE);

    /** NOTAM items F/G: SFC, GND, 00120M AGL, 05000FT AMSL, FL100, UNL. */
    static void limit(TfrArea.Vert v, String s) {
        final String t = s == null ? "" : s.trim().toUpperCase(Locale.US);
        if (t.isEmpty() || "UNL".equals(t))
            return;
        v.raw = t;
        if ("SFC".equals(t) || "GND".equals(t)) {
            v.present = true;
            v.agl = true;
            v.surface = true;
            return;
        }
        final Matcher fl = FL.matcher(t);
        if (fl.matches()) {
            v.present = true;
            v.flightLevel = true;
            v.feet = Integer.parseInt(fl.group(1)) * 100;
            return;
        }
        final Matcher m = LIMIT.matcher(t);
        if (m.matches()) {
            EnaireUasFeed.vert(v, Double.parseDouble(m.group(1)), m.group(2), m.group(3));
        }
    }

    private static long number(JSONObject at, String key) {
        if (at.isNull(key))
            return 0L;
        try {
            return Long.parseLong(at.optString(key, "0").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String text(JSONObject at, String key) {
        if (at.isNull(key))
            return "";
        final String s = at.optString(key, "").trim();
        return "null".equals(s) ? "" : s;
    }
}
