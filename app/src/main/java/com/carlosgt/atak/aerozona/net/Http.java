package com.carlosgt.atak.aerozona.net;

import com.atakmap.coremap.log.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Small HTTPS GET client: bounded time, bounded response size, HTTPS only.
 *
 * <p>Comms' helper, with the callback form dropped. A sync sweep is the wrong place for
 * callbacks: a first sync is the list plus one detail per TFR, over a hundred requests that
 * must run in order on one worker so the plugin never has a hundred sockets open on a phone.
 * {@link com.carlosgt.atak.aerozona.TfrManager} owns that worker and calls this from it.
 *
 * <p>Anonymous classes rather than lambdas anywhere this grows: the SDK documents lambdas
 * breaking under release proguard, and this ships in release builds.
 */
public final class Http {

    private static final String TAG = "TfrHttp";

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    /** The list is 24 KB and the largest detail in the national picture is 78 KB. */
    private static final int MAX_BYTES = 4 * 1024 * 1024;

    private Http() {
    }

    /**
     * Fetch a URL on the calling thread.
     *
     * @throws IOException with a message already phrased for the operator
     */
    public static byte[] get(String url) throws IOException {
        return get(url, MAX_BYTES);
    }

    /** The same with a larger cap, for the few national documents (AEMET observations). */
    public static byte[] get(String url, int maxBytes) throws IOException {
        final URL parsed = new URL(url);
        if (!"https".equalsIgnoreCase(parsed.getProtocol()))
            throw new IOException("refusing a non-https request");

        HttpsURLConnection conn = null;
        InputStream in = null;
        try {
            conn = (HttpsURLConnection) parsed.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "AeroZona-ATAK-plugin");
            conn.setRequestProperty("Accept-Encoding", "identity");

            final int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK)
                throw new IOException("server returned HTTP " + status);

            in = conn.getInputStream();
            return read(in, maxBytes);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // Already have the body or the failure.
                }
            }
            if (conn != null)
                conn.disconnect();
        }
    }

    /**
     * Streams a URL to a file (written as {@code dest.tmp}, then renamed), so a big
     * document never sits whole in ATAK's heap. Same rules as {@link #get(String)}.
     */
    public static void toFile(String url, java.io.File dest, long maxBytes) throws IOException {
        final URL parsed = new URL(url);
        if (!"https".equalsIgnoreCase(parsed.getProtocol()))
            throw new IOException("refusing a non-https request");
        final java.io.File tmp = new java.io.File(dest.getPath() + ".tmp");
        HttpsURLConnection conn = null;
        try {
            conn = (HttpsURLConnection) parsed.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "AeroZona-ATAK-plugin");
            conn.setRequestProperty("Accept-Encoding", "identity");
            final int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK)
                throw new IOException("server returned HTTP " + status);
            long total = 0;
            try (InputStream in = conn.getInputStream();
                    java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
                final byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > maxBytes)
                        throw new IOException("response larger than "
                                + (maxBytes / 1024) + " KB");
                    out.write(buf, 0, n);
                }
            }
            replace(tmp, dest);
        } finally {
            if (tmp.isFile() && !tmp.delete())
                debug("could not delete " + tmp.getName());
            if (conn != null)
                conn.disconnect();
        }
    }

    /**
     * Moves {@code from} over {@code to}. rename(2) replaces on Android, but
     * File.renameTo does not promise it on every filesystem: if it refuses, the old
     * file goes first, so a stale cache is never kept for ever.
     */
    public static void replace(java.io.File from, java.io.File to) throws IOException {
        if (from.renameTo(to))
            return;
        if (to.exists() && !to.delete())
            throw new IOException("cannot replace " + to.getName());
        if (!from.renameTo(to))
            throw new IOException("cannot store " + to.getName());
    }

    private static byte[] read(InputStream in, int maxBytes) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        final byte[] buf = new byte[16384];
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > maxBytes)
                throw new IOException("response larger than "
                        + (maxBytes / (1024 * 1024)) + " MB");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** What to put in front of an operator when a fetch fails. */
    public static String describe(Exception e) {
        if (e instanceof java.net.SocketTimeoutException)
            return "timed out";
        if (e instanceof java.net.UnknownHostException)
            return "no route to tfr.faa.gov";
        if (e instanceof javax.net.ssl.SSLException)
            return "TLS failed";
        final String message = e.getMessage();
        return message == null ? "network error" : message;
    }

    static void debug(String message) {
        Log.d(TAG, message);
    }
}
