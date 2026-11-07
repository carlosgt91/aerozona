package com.carlosgt.atak.aerozona;

import com.carlosgt.atak.aerozona.net.Http;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Spanish UAS geographic zones (RD 517/2024) from ENAIRE's servAIS ArcGIS service.
 *
 * <p>Public and keyless. ENAIRE's terms (servAIS_APIDOC §3) allow the use provided it is
 * stated that ENAIRE holds the rights, so every details page says so.
 *
 * <p><b>Tiles of our own, on disk.</b> The view is cut into fixed {@link #TILE_DEG}
 * squares; each square is asked for once and kept as gzipped JSON exactly as the service
 * answered, so a zone seen once stays on the phone with no network. The data changes on
 * the AIRAC cycle (28 days), so a square older than {@link #MAX_AGE_MS} is asked again and
 * the old answer is kept if the new one fails. Measured 2026-11-07: a 0.35 x 0.2 degree box
 * over Zaragoza took under a second and 100 KB for the three layers.
 *
 * <p>No ATAK types here: like {@link AirspaceFeed} this runs on a desktop JVM.
 */
public final class EnaireUasFeed {

    static final String BASE = "https://servais.enaire.es/insigniads/rest/services/NSF_SRV/"
            + "SRV_UAS_ZG_data_V2/MapServer/";

    /** Layer id and ENAIRE's own layer name. */
    static final int[] LAYERS = { 0, 2, 3 };
    static final String[] LAYER_NAMES = { "Infraestructuras", "Aero", "Urbano" };

    private static final String FIELDS = "OBJECTID,identifier,name,type,reasons,message,"
            + "lower,lowerReference,upper,upperReference,uom,contactName,email,phone,siteURL,"
            + "validFrom,validTo";

    public static final double TILE_DEG = 0.25d;
    /** A week: well inside the 28-day cycle, so a change is never more than a week late. */
    public static final long MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L;
    /** The service's page size. */
    private static final int PAGE = 1000;
    /** Spain, Canarias included, with a margin. Outside this nothing is asked. */
    private static final double S = 27.0, W = -18.8, N = 44.5, E = 4.9;

    private EnaireUasFeed() {
    }

    /** The squares a box needs, as "lat_lon" of their south-west corner. */
    public static List<String> keysFor(double south, double west, double north, double east,
            int max) {
        final List<String> out = new ArrayList<>();
        final double s = Math.max(south, S), w = Math.max(west, W);
        final double n = Math.min(north, N), e = Math.min(east, E);
        if (s >= n || w >= e)
            return out;
        for (double la = floor(s); la < n && out.size() < max; la += TILE_DEG)
            for (double lo = floor(w); lo < e && out.size() < max; lo += TILE_DEG)
                out.add(key(la, lo));
        return out;
    }

    private static double floor(double v) {
        return Math.floor(v / TILE_DEG) * TILE_DEG;
    }

    static String key(double lat, double lon) {
        return String.format(Locale.US, "%.2f_%.2f", lat, lon);
    }

    /**
     * Every zone in one square: from disk when fresh, else from ENAIRE (and saved).
     * When the network fails a stale square is still returned; with nothing on disk the
     * failure is thrown so the caller can say the area is not downloaded.
     */
    public static List<Airspace> tile(File dir, String key, boolean allowNetwork)
            throws IOException {
        final File f = new File(dir, "zg_" + key + ".json.gz");
        final boolean fresh = f.isFile()
                && System.currentTimeMillis() - f.lastModified() < MAX_AGE_MS;
        if (fresh || (f.isFile() && !allowNetwork))
            return parseTile(readGz(f));
        if (!allowNetwork)
            throw new IOException("not downloaded");
        try {
            final String[] parts = key.split("_");
            final double la = Double.parseDouble(parts[0]);
            final double lo = Double.parseDouble(parts[1]);
            final JSONObject tile = fetch(la, lo, la + TILE_DEG, lo + TILE_DEG);
            final byte[] body = tile.toString().getBytes(Charset.forName("UTF-8"));
            if (!dir.isDirectory() && !dir.mkdirs())
                throw new IOException("cannot create " + dir);
            final File tmp = new File(dir, f.getName() + ".tmp");
            writeGz(tmp, body);
            if (!tmp.renameTo(f))
                throw new IOException("cannot store " + f);
            return parseTile(body);
        } catch (IOException e) {
            if (f.isFile())
                return parseTile(readGz(f));
            throw e;
        }
    }

    /** One square, all three layers, as {"layers":{"Aero":{...service answer...},...}}. */
    static JSONObject fetch(double south, double west, double north, double east)
            throws IOException {
        final JSONObject layers = new JSONObject();
        for (int i = 0; i < LAYERS.length; i++) {
            final JSONArray all = new JSONArray();
            int offset = 0;
            while (true) {
                final JSONObject page = page(LAYERS[i], south, west, north, east, offset);
                final JSONArray fs = page.optJSONArray("features");
                if (fs == null || fs.length() == 0)
                    break;
                for (int j = 0; j < fs.length(); j++)
                    all.put(fs.opt(j));
                if (!page.optBoolean("exceededTransferLimit", false) || all.length() > 20000)
                    break;
                offset += fs.length();
            }
            try {
                layers.put(LAYER_NAMES[i], new JSONObject().put("features", all));
            } catch (Exception e) {
                throw new IOException("cannot build the tile");
            }
        }
        try {
            return new JSONObject().put("layers", layers)
                    .put("fetched", System.currentTimeMillis());
        } catch (Exception e) {
            throw new IOException("cannot build the tile");
        }
    }

    private static JSONObject page(int layer, double south, double west, double north,
            double east, int offset) throws IOException {
        final String envelope = String.format(Locale.US,
                "{\"xmin\":%.5f,\"ymin\":%.5f,\"xmax\":%.5f,\"ymax\":%.5f,"
                        + "\"spatialReference\":{\"wkid\":4326}}",
                west, south, east, north);
        final StringBuilder u = new StringBuilder(BASE).append(layer).append("/query");
        u.append("?f=json&inSR=4326&outSR=4326&returnGeometry=true&where=1%3D1");
        u.append("&geometryType=esriGeometryEnvelope&spatialRel=esriSpatialRelIntersects");
        u.append("&geometry=").append(enc(envelope));
        u.append("&outFields=").append(enc(FIELDS));
        // About 10 m: a zone boundary is a legal line, so it is not generalized much.
        u.append("&maxAllowableOffset=0.0001");
        u.append("&resultRecordCount=").append(PAGE);
        if (offset > 0)
            u.append("&resultOffset=").append(offset);
        final byte[] body = Http.get(u.toString());
        final JSONObject root;
        try {
            root = new JSONObject(new String(body, Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new IOException("ENAIRE answer not readable");
        }
        final JSONObject error = root.optJSONObject("error");
        if (error != null)
            throw new IOException(error.optString("message", "ENAIRE refused the query"));
        return root;
    }

    /** Every drawable zone in a stored tile. */
    public static List<Airspace> parseTile(byte[] body) throws IOException {
        final JSONObject root;
        try {
            root = new JSONObject(new String(body, Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new IOException("stored tile not readable");
        }
        final List<Airspace> out = new ArrayList<>();
        final JSONObject layers = root.optJSONObject("layers");
        if (layers == null)
            return out;
        // ENAIRE publishes some zones twice: same identifier, limits and geometry under two
        // OBJECTIDs (INF0224 near Zaragoza, 2026-11-07). Both drew and both came up on a
        // tap. One copy is kept, the lowest OBJECTID, so every square keeps the same one
        // and the merge by id across squares still holds.
        final java.util.Map<String, Integer> bySig = new java.util.HashMap<>();
        for (String layer : LAYER_NAMES) {
            final JSONObject l = layers.optJSONObject(layer);
            final JSONArray fs = l == null ? null : l.optJSONArray("features");
            if (fs == null)
                continue;
            for (int i = 0; i < fs.length(); i++) {
                final JSONObject f = fs.optJSONObject(i);
                final Airspace a = one(layer, f);
                if (a == null || !a.drawable())
                    continue;
                final String sig = signature(layer, f);
                final Integer at = bySig.get(sig);
                if (at == null) {
                    bySig.put(sig, out.size());
                    out.add(a);
                } else if (objectId(f) < objectId(out.get(at))) {
                    out.set(at, a);
                }
            }
        }
        return out;
    }

    /** What makes two published zones the same zone: everything but the OBJECTID. */
    static String signature(String layer, JSONObject f) {
        final JSONObject at = f.optJSONObject("attributes");
        final StringBuilder b = new StringBuilder(layer);
        if (at != null)
            for (String k : new String[] { "identifier", "type", "lower", "lowerReference",
                    "upper", "upperReference", "uom", "validFrom", "validTo" })
                b.append('|').append(at.opt(k));
        final JSONObject g = f.optJSONObject("geometry");
        b.append('|').append(g == null ? "" : String.valueOf(g.optJSONArray("rings")));
        return b.toString();
    }

    private static long objectId(JSONObject f) {
        final JSONObject at = f == null ? null : f.optJSONObject("attributes");
        return at == null ? Long.MAX_VALUE : at.optLong("OBJECTID", Long.MAX_VALUE);
    }

    private static long objectId(Airspace a) {
        final int c = a.id.lastIndexOf(':');
        try {
            return Long.parseLong(a.id.substring(c + 1));
        } catch (RuntimeException e) {
            return Long.MAX_VALUE;
        }
    }

    static Airspace one(String layer, JSONObject f) {
        if (f == null)
            return null;
        final JSONObject at = f.optJSONObject("attributes");
        final JSONObject geom = f.optJSONObject("geometry");
        if (at == null || geom == null)
            return null;
        final JSONArray rings = geom.optJSONArray("rings");
        if (rings == null)
            return null;
        final Airspace a = new Airspace();
        a.source = Airspace.SOURCE_ENAIRE_UAS;
        a.uasLayer = layer;
        // OBJECTID is per layer, so the layer goes in the id or two zones collide.
        a.id = layer + ":" + (at.isNull("OBJECTID") ? text(at, "identifier")
                : Long.toString(at.optLong("OBJECTID", 0L)));
        a.ident = text(at, "identifier");
        a.name = text(at, "name");
        a.uasType = text(at, "type");
        a.reasons = text(at, "reasons");
        a.message = Html.toText(text(at, "message"));
        a.contactName = text(at, "contactName");
        a.email = text(at, "email");
        a.phone = text(at, "phone");
        a.siteUrl = text(at, "siteURL");
        a.validFromMs = at.optLong("validFrom", 0L);
        a.validToMs = at.optLong("validTo", 0L);
        final String uom = text(at, "uom");
        vert(a.floor, at.optDouble("lower", Double.NaN), uom, text(at, "lowerReference"));
        vert(a.ceiling, at.optDouble("upper", Double.NaN), uom, text(at, "upperReference"));
        AirspaceFeed.readRings(a.parts, rings);
        return a;
    }

    /** One ED-318 limit: value, unit (M or FT) and reference (AGL or AMSL). */
    static void vert(TfrArea.Vert v, double value, String uom, String ref) {
        if (Double.isNaN(value))
            return;
        final boolean metres = !"FT".equalsIgnoreCase(uom);
        v.present = true;
        v.agl = !"AMSL".equalsIgnoreCase(ref) && !"MSL".equalsIgnoreCase(ref);
        v.metric = metres;
        v.metres = metres ? value : value * 0.3048d;
        v.feet = (int) Math.round(metres ? value / 0.3048d : value);
        v.surface = v.agl && value == 0d;
        v.raw = (trim(value) + " " + (metres ? "m" : "ft") + " " + (v.agl ? "AGL" : "AMSL"));
    }

    private static String trim(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : String.format(Locale.US, "%.1f", d);
    }

    /** Not optString: on Android it turns a JSON null into the text "null". */
    private static String text(JSONObject at, String key) {
        if (at.isNull(key))
            return "";
        final String s = at.optString(key, "").trim();
        return "null".equals(s) ? "" : s;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    public static byte[] readGz(File f) throws IOException {
        try (InputStream in = new GZIPInputStream(new FileInputStream(f))) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0)
                out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static void writeGz(File f, byte[] body) throws IOException {
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(f))) {
            out.write(body);
        }
    }

    /** Distinct keys, in order. */
    static Set<String> set(List<String> keys) {
        return new LinkedHashSet<>(keys);
    }
}
