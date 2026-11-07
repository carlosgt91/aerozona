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
 * ENAIRE airspace that matters at drone heights, with the service frequency of each zone
 * (INSIGNIA_VIGOR/Aero_APP_VIGOR_Airspace_V_5).
 *
 * <p>CTR, ATZ/FIZ, RMZ, TMZ, D/P/R/TRA/TSA areas and the ENR 5.5 sport-activity zones
 * (model aircraft, parachuting, gliders, RPAS). The whole country is about 750 KB
 * (2026-11-07), so it is fetched in one go per layer and kept for {@link #MAX_AGE_MS}:
 * the data follows the AIRAC cycle. TMA/CTA are left out: their floors are far above a
 * drone.
 *
 * <p>Plain Java, no ATAK types: runs on a desktop JVM.
 */
public final class EnaireAirspaceFeed {

    static final String BASE = "https://servais.enaire.es/insignias/rest/services/"
            + "INSIGNIA_VIGOR/Aero_APP_VIGOR_Airspace_V_5/MapServer/";
    /** Layer ids: CTR, ATZ/FIZ, D/P/R, sport activities, RMZ, TMZ. */
    static final int[] LAYERS = { 6, 5, 4, 22, 2, 3 };
    private static final String FIELDS = "OBJECTID,IDENT_TXT,NAME_TXT,TYPE_CODE,CLASS,"
            + "NIVEL_INF,NIVEL_SUP,TYPE_CODE_SERVICE,FREQTRANS_VAL,FREQ_UOM,WORKHR_CODE,"
            + "WORKHRRMK_TXT,REMARKS_TXT,TYPE_CODE_ACTIVITY,OPERATOR_ACTIVITY,"
            + "IDENT_TXT_ACTIVITY";
    public static final long MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final String FILE = "airspace_es.json.gz";

    private EnaireAirspaceFeed() {
    }

    public static boolean stale(File dir) {
        final File f = new File(dir, FILE);
        return !f.isFile() || System.currentTimeMillis() - f.lastModified() >= MAX_AGE_MS;
    }

    public static List<Airspace> load(File dir, boolean allowNetwork) throws IOException {
        final File f = new File(dir, FILE);
        if (allowNetwork && stale(dir)) {
            try {
                final byte[] body = fetch().toString().getBytes(Charset.forName("UTF-8"));
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new IOException("cannot create " + dir);
                final File tmp = new File(dir, FILE + ".tmp");
                try (OutputStream out = new GZIPOutputStream(new FileOutputStream(tmp))) {
                    out.write(body);
                }
                if (!tmp.renameTo(f))
                    throw new IOException("cannot store " + f);
                return parse(body);
            } catch (IOException e) {
                if (!f.isFile())
                    throw e;
            }
        }
        if (!f.isFile())
            throw new IOException("not downloaded");
        return parse(EnaireUasFeed.readGz(f));
    }

    static JSONObject fetch() throws IOException {
        final JSONObject layers = new JSONObject();
        for (int layer : LAYERS) {
            final String u = BASE + layer + "/query?f=json&where=1%3D1&outSR=4326"
                    + "&returnGeometry=true&maxAllowableOffset=0.0003&outFields="
                    + FIELDS.replace(",", "%2C");
            final byte[] body = Http.get(u);
            try {
                final JSONObject root = new JSONObject(
                        new String(body, Charset.forName("UTF-8")));
                if (root.optJSONObject("error") != null)
                    throw new IOException("ENAIRE refused airspace layer " + layer);
                layers.put(Integer.toString(layer), root);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("ENAIRE airspace answer not readable");
            }
        }
        try {
            return new JSONObject().put("layers", layers);
        } catch (Exception e) {
            throw new IOException("cannot build the airspace file");
        }
    }

    static List<Airspace> parse(byte[] body) throws IOException {
        final JSONObject root;
        try {
            root = new JSONObject(new String(body, Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new IOException("stored airspace not readable");
        }
        final List<Airspace> out = new ArrayList<>();
        final JSONObject layers = root.optJSONObject("layers");
        if (layers == null)
            return out;
        for (int layer : LAYERS) {
            final JSONObject l = layers.optJSONObject(Integer.toString(layer));
            final JSONArray fs = l == null ? null : l.optJSONArray("features");
            if (fs == null)
                continue;
            for (int i = 0; i < fs.length(); i++) {
                final Airspace a = one(layer, fs.optJSONObject(i));
                if (a != null && a.drawable() && reachable(a))
                    out.add(a);
            }
        }
        return out;
    }

    static Airspace one(int layer, JSONObject f) {
        if (f == null)
            return null;
        final JSONObject at = f.optJSONObject("attributes");
        final JSONObject geom = f.optJSONObject("geometry");
        final JSONArray rings = geom == null ? null : geom.optJSONArray("rings");
        if (at == null || rings == null)
            return null;
        final Airspace a = new Airspace();
        a.source = Airspace.SOURCE_ENAIRE_AIRSPACE;
        // The same zone is published in more than one layer (a FIZ that is also an RMZ):
        // keyed by layer and id it would draw twice, so the id is the OBJECTID alone.
        a.id = "eas:" + (layer == 22 ? "s" : "") + text(at, "OBJECTID");
        a.ident = text(at, "IDENT_TXT");
        a.name = text(at, "NAME_TXT");
        a.typeCode = text(at, "TYPE_CODE");
        a.classCode = text(at, "CLASS");
        a.activity = text(at, "TYPE_CODE_ACTIVITY");
        if (layer == 22) {
            a.typeCode = "SPORT";
            a.ident = text(at, "IDENT_TXT_ACTIVITY");
            a.name = text(at, "OPERATOR_ACTIVITY");
        }
        a.service = text(at, "TYPE_CODE_SERVICE");
        a.frequency = frequency(text(at, "FREQTRANS_VAL"), text(at, "FREQ_UOM"));
        a.workHours = join(text(at, "WORKHR_CODE"), text(at, "WORKHRRMK_TXT"));
        a.message = Html.toText(text(at, "REMARKS_TXT"));
        a.floorText = text(at, "NIVEL_INF");
        a.ceilingText = text(at, "NIVEL_SUP");
        level(a.floor, a.floorText);
        level(a.ceiling, a.ceilingText);
        AirspaceFeed.readRings(a.parts, rings);
        return a;
    }

    /**
     * Whether a drone can reach the zone at all. Danger areas from FL355 up and the like
     * only clutter the map: anything floored at a flight level or above 5,000 ft is left out.
     */
    static boolean reachable(Airspace a) {
        if (!a.floor.present || a.floor.surface)
            return true;
        if (a.floor.flightLevel)
            return false;
        return a.floor.feet <= 5000;
    }

    /**
     * The frequency to tune. ENAIRE marks 8.33 kHz channels with unit "C": the published
     * channel name is not the carrier (128.955 is tuned as 128.950), so both are given.
     */
    static String frequency(String value, String uom) {
        if (value.isEmpty())
            return "";
        if (!"C".equalsIgnoreCase(uom))
            return value + " MHz";
        try {
            final double ch = Double.parseDouble(value);
            final long khz = Math.round(ch * 1000d);
            final long block = (khz / 25L) * 25L;
            final long rem = khz - block;
            final double carrier;
            if (rem == 0)
                carrier = khz / 1000d;
            else if (rem == 5)
                carrier = block / 1000d;
            else if (rem == 10)
                carrier = (block + 25d / 3d) / 1000d;
            else if (rem == 15)
                carrier = (block + 50d / 3d) / 1000d;
            else
                return value + " (" + Lang.t("canal 8,33", "8.33 channel") + ")";
            return String.format(Locale.US, "%s (%s %.4f MHz)", value,
                    Lang.t("canal 8,33 →", "8.33 channel →"), carrier);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static final Pattern H = Pattern.compile(
            "^\\s*(\\d+(?:[.,]\\d+)?)\\s*(ft|m)\\s*(AGL|AMSL|MSL|ALT)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FL = Pattern.compile("^\\s*FL\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);

    /** NIVEL_INF / NIVEL_SUP: "SFC", "3500ft AMSL", "1000ft AGL ; 1650 ft AMSL", "FL100". */
    static void level(TfrArea.Vert v, String s) {
        final String t = s == null ? "" : s.trim();
        if (t.isEmpty() || t.toUpperCase(Locale.US).startsWith("UNL"))
            return;
        v.raw = t;
        final String u = t.toUpperCase(Locale.US);
        if (u.startsWith("SFC") || u.startsWith("GND")) {
            v.present = true;
            v.agl = true;
            v.surface = true;
            return;
        }
        final Matcher fl = FL.matcher(t);
        if (fl.find()) {
            v.present = true;
            v.flightLevel = true;
            v.feet = Integer.parseInt(fl.group(1)) * 100;
            return;
        }
        final Matcher m = H.matcher(t);
        if (m.find()) {
            final String ref = m.group(3) == null ? "AMSL" : m.group(3).toUpperCase(Locale.US);
            EnaireUasFeed.vert(v, Double.parseDouble(m.group(1).replace(',', '.')),
                    m.group(2).equalsIgnoreCase("m") ? "M" : "FT",
                    "AGL".equals(ref) ? "AGL" : "AMSL");
            v.raw = t;
        }
    }

    private static String join(String a, String b) {
        if (a.isEmpty())
            return b;
        return b.isEmpty() ? a : a + " · " + b;
    }

    private static String text(JSONObject at, String key) {
        if (at.isNull(key))
            return "";
        final String s = at.optString(key, "").trim();
        return "null".equals(s) ? "" : s;
    }
}
