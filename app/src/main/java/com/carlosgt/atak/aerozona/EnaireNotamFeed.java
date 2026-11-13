package com.carlosgt.atak.aerozona;

import com.carlosgt.atak.aerozona.net.ArcGisStream;
import com.carlosgt.atak.aerozona.net.Http;

import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * UAS-relevant NOTAM areas from ENAIRE's servAIS (DINAMIC/UAS_NOTAM_SRV_DATA_V1).
 *
 * <p>National: 780 areas, 332 KB on 2026-11-07 and ~1 MB on 2026-11-13, so the whole
 * country is fetched in one query, kept on disk, and refreshed every
 * {@link #MAX_AGE_MS}. ENAIRE allows this use but names Ícaro XXI as the official NOTAM
 * source, which every details page says.
 *
 * <p>No ATAK types; the stream reader is Android's {@code android.util.JsonReader}.
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

    /**
     * Every NOTAM area not yet expired. Network when stale, else disk; disk on failure.
     *
     * <p>Read in parts: the answer is downloaded to disk and walked one feature at a time,
     * never held whole. It grew from 332 KB to ~1 MB (2026-11-13), and as bytes + String +
     * JSON tree it ran ATAK out of memory on a loaded device.
     */
    public static List<Airspace> load(File dir, boolean allowNetwork) throws IOException {
        final File f = new File(dir, FILE);
        if (allowNetwork && stale(dir)) {
            final File raw = new File(dir, "uas_notam.json.part");
            final File gz = new File(dir, FILE + ".tmp");
            try {
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new IOException("cannot create " + dir);
                if (raw.isFile() && !raw.delete()) // left by a killed process
                    throw new IOException("cannot clear " + raw.getName());
                Http.toFile(url(), raw, MAX_BYTES);
                final List<Airspace> out;
                try (InputStream in = new FileInputStream(raw)) {
                    out = parse(in, System.currentTimeMillis());
                }
                try (InputStream in = new FileInputStream(raw);
                        OutputStream o = new GZIPOutputStream(new FileOutputStream(gz))) {
                    final byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0)
                        o.write(buf, 0, n);
                }
                Http.replace(gz, f);
                return out;
            } catch (IOException e) {
                if (!f.isFile())
                    throw e;
            } finally {
                if (raw.isFile() && !raw.delete())
                    raw.deleteOnExit();
                if (gz.isFile() && !gz.delete())
                    gz.deleteOnExit();
            }
        }
        if (!f.isFile())
            throw new IOException("not downloaded");
        try (InputStream in = new GZIPInputStream(new FileInputStream(f), 16384)) {
            return parse(in, System.currentTimeMillis());
        }
    }

    /** Far more than the national answer (~1 MB), to refuse anything absurd. */
    private static final long MAX_BYTES = 32L * 1024 * 1024;

    static String url() {
        return URL + "?f=json&where=1%3D1&outSR=4326&returnGeometry=true"
                + "&maxAllowableOffset=0.0001&outFields=" + FIELDS.replace(",", "%2C");
    }

    /** The areas in an ArcGIS query answer, read as a stream (see ArcGisStream). */
    static List<Airspace> parse(InputStream in, final long nowMs) throws IOException {
        final List<Airspace> out = new ArrayList<>();
        try (JsonReader r = new JsonReader(new InputStreamReader(
                new java.io.BufferedInputStream(in, 16384), Charset.forName("UTF-8")))) {
            ArcGisStream.answer(r, new ArcGisStream.Visitor() {
                @Override
                public void feature(JSONObject f) {
                    final Airspace a = one(f);
                    if (a == null || !a.drawable())
                        return;
                    if (a.validToMs > 0 && a.validToMs < nowMs)
                        return;
                    out.add(a);
                }
            });
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
