package com.carlosgt.atak.aerozona.aemet;

import com.carlosgt.atak.aerozona.Lang;
import com.carlosgt.atak.aerozona.terrain.Polygon;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
        return covering(entries(cached(client, dir)), lat, lon, System.currentTimeMillis(),
                Lang.es());
    }

    /** The tar, from disk when fresh, else from AEMET; stale disk on failure. */
    private static byte[] cached(AemetClient client, File dir) throws IOException {
        final File f = new File(dir, FILE);
        if (f.isFile() && System.currentTimeMillis() - f.lastModified() < AGE_MS)
            return read(f);
        try {
            final byte[] body = client.getBytes(PATH, 32 * 1024 * 1024);
            if (!dir.isDirectory() && !dir.mkdirs())
                throw new IOException("cannot create " + dir);
            final File tmp = new File(dir, FILE + ".tmp");
            try (OutputStream out = new FileOutputStream(tmp)) {
                out.write(body);
            }
            if (!tmp.renameTo(f))
                throw new IOException("cannot store " + f);
            return body;
        } catch (IOException e) {
            if (f.isFile() && !(e instanceof AemetClient.KeyProblem))
                return read(f);
            throw e;
        }
    }

    private static byte[] read(File f) throws IOException {
        final byte[] b = new byte[(int) f.length()];
        try (InputStream in = new FileInputStream(f)) {
            int off = 0;
            while (off < b.length) {
                final int n = in.read(b, off, b.length - off);
                if (n < 0)
                    throw new IOException("short read " + f);
                off += n;
            }
        }
        return b;
    }

    /** The files in a ustar/GNU tar (gzip-wrapped or not). Only regular files. */
    static List<byte[]> entries(byte[] tar) throws IOException {
        if (tar.length > 2 && (tar[0] & 0xff) == 0x1f && (tar[1] & 0xff) == 0x8b) {
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(tar))) {
                final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                final byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0)
                    out.write(buf, 0, n);
                tar = out.toByteArray();
            }
        }
        final List<byte[]> out = new ArrayList<>();
        int pos = 0;
        while (pos + 512 <= tar.length) {
            if (tar[pos] == 0)
                break;
            long size = 0;
            for (int i = 124; i < 136; i++) {
                final int c = tar[pos + i];
                if (c >= '0' && c <= '7')
                    size = size * 8 + (c - '0');
                else if (size > 0 || (c != ' ' && c != 0))
                    break;
            }
            final byte type = tar[pos + 156];
            final int start = pos + 512;
            if (size < 0 || start + size > tar.length)
                throw new IOException("tar truncated");
            if (type == '0' || type == 0) {
                final byte[] e = new byte[(int) size];
                System.arraycopy(tar, start, e, 0, (int) size);
                out.add(e);
            }
            pos = start + (int) ((size + 511) / 512 * 512);
        }
        return out;
    }

    /** The warnings in the files that cover the point at nowMs, worst first. */
    static List<Alert> covering(List<byte[]> files, double lat, double lon, long nowMs,
            boolean spanish) {
        final DocumentBuilder db;
        try {
            final DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            f.setExpandEntityReferences(false);
            db = f.newDocumentBuilder();
        } catch (Exception e) {
            return new ArrayList<>();
        }
        final List<Element> alerts = new ArrayList<>();
        final Set<String> superseded = new HashSet<>();
        for (byte[] x : files) {
            try {
                final Document d = db.parse(new ByteArrayInputStream(x));
                final Element a = d.getDocumentElement();
                if (!"Actual".equals(child(a, "status")))
                    continue;
                alerts.add(a);
                // "sender,identifier,sent" triples, space-separated: those are replaced.
                for (String ref : child(a, "references").split("\\s+")) {
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
        for (Element a : alerts) {
            final String id = child(a, "identifier");
            if (superseded.contains(id) || "Cancel".equals(child(a, "msgType")))
                continue;
            final Element info = pickInfo(a, spanish);
            if (info == null)
                continue;
            final Alert al = new Alert();
            al.id = id;
            al.level = param(info, "AEMET-Meteoalerta nivel").toLowerCase(Locale.ROOT);
            if (al.rank() == 0)
                continue;
            al.expiresMs = time(child(info, "expires"));
            al.onsetMs = time(child(info, "onset"));
            if (al.expiresMs > 0 && al.expiresMs < nowMs)
                continue;
            if (!inside(info, lat, lon))
                continue;
            final String fen = eventCode(info, "AEMET-Meteoalerta fenomeno");
            al.phenomenon = fen.contains(";") ? fen.substring(fen.indexOf(';') + 1) : fen;
            al.headline = child(info, "headline");
            al.description = child(info, "description");
            final NodeList areas = info.getElementsByTagName("areaDesc");
            al.area = areas.getLength() > 0 ? areas.item(0).getTextContent().trim() : "";
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

    private static Element pickInfo(Element alert, boolean spanish) {
        Element first = null;
        for (Node n = alert.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element) || !"info".equals(((Element) n).getTagName()))
                continue;
            final Element e = (Element) n;
            if (first == null)
                first = e;
            final String lang = child(e, "language").toLowerCase(Locale.ROOT);
            if (spanish ? lang.startsWith("es") : lang.startsWith("en"))
                return e;
        }
        return first;
    }

    static boolean inside(Element info, double lat, double lon) {
        final NodeList polys = info.getElementsByTagName("polygon");
        for (int i = 0; i < polys.getLength(); i++) {
            final String[] pts = polys.item(i).getTextContent().trim().split("\\s+");
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

    private static String child(Element e, String tag) {
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element && tag.equals(((Element) n).getTagName()))
                return n.getTextContent().trim();
        return "";
    }

    private static String param(Element info, String name) {
        return named(info, "parameter", name);
    }

    private static String eventCode(Element info, String name) {
        return named(info, "eventCode", name);
    }

    private static String named(Element info, String tag, String name) {
        final NodeList l = info.getElementsByTagName(tag);
        for (int i = 0; i < l.getLength(); i++) {
            final Element p = (Element) l.item(i);
            if (name.equalsIgnoreCase(child(p, "valueName")))
                return child(p, "value");
        }
        return "";
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
