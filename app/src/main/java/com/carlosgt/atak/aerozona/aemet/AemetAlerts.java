package com.carlosgt.atak.aerozona.aemet;

import com.carlosgt.atak.aerozona.Lang;
import com.carlosgt.atak.aerozona.terrain.Polygon;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * Meteoalerta warnings (AEMET, CAP 1.2) that cover a point.
 *
 * <p>{@code avisos_cap/ultimoelaborado/area/esp} hands back a tar of CAP XML files, one
 * per warning and zone, each with the zone polygon in WGS 84. A warning counts when its
 * level is not green, it has not expired, no later message in the same tar replaces or
 * cancels it, and the point is inside one of its polygons. Plain Java, no ATAK types.
 */
public final class AemetAlerts {

    public static final long AGE_MS = 15L * 60L * 1000L;
    static final String PATH = "avisos_cap/ultimoelaborado/area/esp";
    static final String FILE = "avisos_esp.tar";

    public static final class Alert {
        public String id = "", level = "", phenomenon = "", headline = "", description = "";
        public String area = "";
        public long onsetMs, expiresMs;

        /** amarillo 1, naranja 2, rojo 3. */
        public int rank() {
            switch (level) {
                case "rojo":
                    return 3;
                case "naranja":
                    return 2;
                case "amarillo":
                    return 1;
                default:
                    return 0;
            }
        }
    }

    private AemetAlerts() {
    }

    /** Warnings covering the point, worst first. Worker thread only. */
    public static List<Alert> forPoint(AemetClient client, File dir, double lat, double lon)
            throws IOException {
        try (InputStream in = new FileInputStream(cached(client, dir))) {
            return covering(entries(in), lat, lon, System.currentTimeMillis(), Lang.es());
        }
    }

    /**
     * The tar on disk, refreshed from AEMET when old; the stale copy on failure. Streamed
     * to disk: held in memory whole it took ~18 MB at peak and ran ATAK out of memory.
     */
    private static File cached(AemetClient client, File dir) throws IOException {
        final File f = new File(dir, FILE);
        if (f.isFile() && System.currentTimeMillis() - f.lastModified() < AGE_MS)
            return f;
        try {
            if (!dir.isDirectory() && !dir.mkdirs())
                throw new IOException("cannot create " + dir);
            client.toFile(PATH, f, 32L * 1024 * 1024);
            return f;
        } catch (IOException e) {
            if (f.isFile() && !(e instanceof AemetClient.KeyProblem))
                return f;
            throw e;
        }
    }

    /** The files in a ustar/GNU tar (gzip-wrapped or not). Only regular files. */
    static List<byte[]> entries(byte[] tar) throws IOException {
        return entries(new ByteArrayInputStream(tar));
    }

    /** As {@link #entries(byte[])}, reading the stream once: only the files are kept. */
    static List<byte[]> entries(InputStream raw) throws IOException {
        InputStream in = new java.io.BufferedInputStream(raw, 65536);
        in.mark(2);
        final int b0 = in.read(), b1 = in.read();
        in.reset();
        if (b0 == 0x1f && b1 == 0x8b)
            in = new GZIPInputStream(in, 65536);
        try {
            return tarFiles(in);
        } finally {
            in.close(); // frees the gzip Inflater now, not at some later GC
        }
    }

    private static List<byte[]> tarFiles(InputStream in) throws IOException {
        final List<byte[]> out = new ArrayList<>();
        final byte[] head = new byte[512];
        while (lleno(in, head, 512)) {
            if (head[0] == 0)
                break;
            long size = 0;
            for (int i = 124; i < 136; i++) {
                final int c = head[i];
                if (c >= '0' && c <= '7')
                    size = size * 8 + (c - '0');
                else if (size > 0 || (c != ' ' && c != 0))
                    break;
            }
            if (size < 0 || size > 16L * 1024 * 1024)
                throw new IOException("tar entry too big");
            final byte type = head[156];
            final long padded = (size + 511) / 512 * 512;
            if (type == '0' || type == 0) {
                final byte[] e = new byte[(int) size];
                if (!lleno(in, e, (int) size))
                    throw new IOException("tar truncated");
                out.add(e);
                saltar(in, padded - size);
            } else {
                saltar(in, padded);
            }
        }
        return out;
    }

    /** Fills n bytes; false on a clean end of stream before the first byte. */
    private static boolean lleno(InputStream in, byte[] b, int n) throws IOException {
        int off = 0;
        while (off < n) {
            final int r = in.read(b, off, n - off);
            if (r < 0) {
                if (off == 0)
                    return false;
                throw new IOException("tar truncated");
            }
            off += r;
        }
        return true;
    }

    private static void saltar(InputStream in, long n) throws IOException {
        while (n > 0) {
            final long s = in.skip(n);
            if (s <= 0) {
                if (in.read() < 0)
                    throw new IOException("tar truncated");
                n--;
            } else {
                n -= s;
            }
        }
    }

    /** One CAP file, reduced to what is used. */
    static final class Cap {
        String identifier = "", status = "", msgType = "", references = "";
        final List<Info> infos = new ArrayList<>();
    }

    static final class Info {
        String language = "", onset = "", expires = "", headline = "", description = "";
        String areaDesc = "";
        final java.util.Map<String, String> named = new java.util.HashMap<>();
        final List<String> polygons = new ArrayList<>();
    }

    /**
     * Pull parser, not DOM: the platform's own (KXmlParser on Android), which does not
     * process DOCTYPE declarations unless asked and never fetches external entities.
     */
    static Cap parse(byte[] xml) throws Exception {
        final XmlPullParserFactory pf = XmlPullParserFactory.newInstance();
        pf.setNamespaceAware(false);
        final XmlPullParser x = pf.newPullParser();
        x.setInput(new ByteArrayInputStream(xml), null);
        final Cap cap = new Cap();
        final List<String> path = new ArrayList<>();
        Info info = null;
        String valueName = "";
        for (int ev = x.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = x.next()) {
            if (ev == XmlPullParser.START_TAG) {
                final String tag = local(x.getName());
                path.add(tag);
                if ("info".equals(tag) && path.size() == 2) {
                    info = new Info();
                    cap.infos.add(info);
                } else if ("parameter".equals(tag) || "eventCode".equals(tag)
                        || "geocode".equals(tag)) {
                    valueName = "";
                }
            } else if (ev == XmlPullParser.TEXT) {
                if (path.isEmpty())
                    continue;
                final String t = x.getText().trim();
                final String tag = path.get(path.size() - 1);
                if (path.size() == 2) {
                    if ("identifier".equals(tag))
                        cap.identifier = t;
                    else if ("status".equals(tag))
                        cap.status = t;
                    else if ("msgType".equals(tag))
                        cap.msgType = t;
                    else if ("references".equals(tag))
                        cap.references = t;
                } else if (info != null && path.size() == 3) {
                    if ("language".equals(tag))
                        info.language = t;
                    else if ("onset".equals(tag))
                        info.onset = t;
                    else if ("expires".equals(tag))
                        info.expires = t;
                    else if ("headline".equals(tag))
                        info.headline = t;
                    else if ("description".equals(tag))
                        info.description = t;
                } else if (info != null && path.size() == 4) {
                    final String parent = path.get(2);
                    if ("valueName".equals(tag))
                        valueName = t;
                    else if ("value".equals(tag)
                            && ("parameter".equals(parent) || "eventCode".equals(parent)))
                        info.named.put(valueName.toLowerCase(Locale.ROOT), t);
                    else if ("area".equals(parent) && "polygon".equals(tag))
                        info.polygons.add(t);
                    else if ("area".equals(parent) && "areaDesc".equals(tag)
                            && info.areaDesc.isEmpty())
                        info.areaDesc = t;
                }
            } else if (ev == XmlPullParser.END_TAG) {
                if (!path.isEmpty())
                    path.remove(path.size() - 1);
                if (path.size() < 2)
                    info = null;
            }
        }
        return cap;
    }

    private static String local(String name) {
        final int c = name.indexOf(':');
        return c < 0 ? name : name.substring(c + 1);
    }

    /** The warnings in the files that cover the point at nowMs, worst first. */
    static List<Alert> covering(List<byte[]> files, double lat, double lon, long nowMs,
            boolean spanish) {
        final List<Cap> caps = new ArrayList<>();
        final Set<String> superseded = new HashSet<>();
        for (byte[] f : files) {
            try {
                final Cap c = parse(f);
                if (!"Actual".equals(c.status))
                    continue;
                caps.add(c);
                // "sender,identifier,sent" triples, space-separated: those are replaced.
                for (String ref : c.references.split("\\s+")) {
                    final String[] p = ref.split(",");
                    if (p.length >= 2)
                        superseded.add(p[1]);
                }
            } catch (Exception ignored) {
                // one unreadable file does not hide the others
            }
        }
        final List<Alert> out = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (Cap c : caps) {
            if (superseded.contains(c.identifier) || "Cancel".equals(c.msgType))
                continue;
            final Info info = pickInfo(c, spanish);
            if (info == null)
                continue;
            final Alert al = new Alert();
            al.id = c.identifier;
            al.level = value(info, "AEMET-Meteoalerta nivel").toLowerCase(Locale.ROOT);
            if (al.rank() == 0)
                continue;
            al.expiresMs = time(info.expires);
            al.onsetMs = time(info.onset);
            if (al.expiresMs > 0 && al.expiresMs < nowMs)
                continue;
            if (!inside(info.polygons, lat, lon))
                continue;
            final String fen = value(info, "AEMET-Meteoalerta fenomeno");
            al.phenomenon = fen.contains(";") ? fen.substring(fen.indexOf(';') + 1) : fen;
            al.headline = info.headline;
            al.description = info.description;
            al.area = info.areaDesc;
            // the same phenomenon, zone and window can come in more than one file
            if (seen.add(al.level + '|' + al.headline + '|' + al.onsetMs + '|' + al.expiresMs))
                out.add(al);
        }
        java.util.Collections.sort(out, new java.util.Comparator<Alert>() {
            @Override
            public int compare(Alert x, Alert y) {
                if (x.rank() != y.rank())
                    return y.rank() - x.rank();
                return Long.compare(x.onsetMs, y.onsetMs);
            }
        });
        return out;
    }

    private static String value(Info info, String name) {
        final String v = info.named.get(name.toLowerCase(Locale.ROOT));
        return v == null ? "" : v;
    }

    private static Info pickInfo(Cap c, boolean spanish) {
        for (Info i : c.infos) {
            final String lang = i.language.toLowerCase(Locale.ROOT);
            if (spanish ? lang.startsWith("es") : lang.startsWith("en"))
                return i;
        }
        return c.infos.isEmpty() ? null : c.infos.get(0);
    }

    static boolean inside(List<String> polygons, double lat, double lon) {
        for (String poly : polygons) {
            final String[] pts = poly.trim().split("\\s+");
            final double[] la = new double[pts.length], lo = new double[pts.length];
            int n = 0;
            for (String p : pts) {
                final int c = p.indexOf(',');
                if (c < 0)
                    continue;
                try {
                    la[n] = Double.parseDouble(p.substring(0, c));
                    lo[n] = Double.parseDouble(p.substring(c + 1));
                    n++;
                } catch (NumberFormatException ignored) {
                }
            }
            if (n < 3)
                continue;
            if (new Polygon(java.util.Arrays.copyOf(lo, n), java.util.Arrays.copyOf(la, n))
                    .contains(lon, lat))
                return true;
        }
        return false;
    }

    /** CAP times: 2026-11-07T12:00:00-00:00 (any offset). 0 when unreadable. */
    static long time(String s) {
        if (s.length() < 19)
            return 0;
        try {
            final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            long t = f.parse(s.substring(0, 19)).getTime();
            final String z = s.substring(19);
            if (z.length() >= 6 && (z.charAt(0) == '+' || z.charAt(0) == '-')) {
                final int h = Integer.parseInt(z.substring(1, 3));
                final int m = Integer.parseInt(z.substring(4, 6));
                final long off = (h * 60L + m) * 60000L;
                t += z.charAt(0) == '+' ? -off : off;
            }
            return t;
        } catch (Exception e) {
            return 0;
        }
    }
}
