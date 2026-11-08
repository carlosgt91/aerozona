package com.carlosgt.atak.aerozona.aemet;

import com.carlosgt.atak.aerozona.net.Http;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * AEMET OpenData, the two-step way it works: the first call (with the user's own key)
 * answers {estado, descripcion, datos, metadatos}, and "datos" is a short-lived URL that
 * holds the actual data. That second document is served in ISO-8859-15, not UTF-8.
 *
 * <p>The key is the operator's own (AEMET gives one per person) and is only ever sent to
 * opendata.aemet.es. Plain Java, no ATAK types.
 */
public final class AemetClient {

    public static final String BASE = "https://opendata.aemet.es/opendata/api/";

    /** The key is missing or AEMET refused it: the pane says so in words. */
    public static class KeyProblem extends IOException {
        public KeyProblem(String m) {
            super(m);
        }
    }

    private final String apiKey;

    public AemetClient(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean hasKey() {
        return !apiKey.isEmpty();
    }

    /** The data document behind one endpoint, decoded. Worker thread only. */
    public String get(String path) throws IOException {
        return get(path, 4 * 1024 * 1024);
    }

    public String get(String path, int maxBytes) throws IOException {
        return decode(getBytes(path, maxBytes));
    }

    /** The data document behind one endpoint, as AEMET sent it (a tar for the warnings). */
    public byte[] getBytes(String path, int maxBytes) throws IOException {
        if (!hasKey())
            throw new KeyProblem("no AEMET key");
        final String url = BASE + path + (path.contains("?") ? "&" : "?") + "api_key="
                + URLEncoder.encode(apiKey, "UTF-8");
        final byte[] first;
        try {
            first = Http.get(url);
        } catch (IOException e) {
            final String m = String.valueOf(e.getMessage());
            if (m.contains("401") || m.contains("403"))
                throw new KeyProblem("AEMET refused the key");
            throw e;
        }
        if (first.length == 0)
            throw new KeyProblem("AEMET answered nothing (key not valid?)");
        final JSONObject head;
        try {
            head = new JSONObject(decode(first));
        } catch (Exception e) {
            throw new IOException("AEMET answer not readable");
        }
        final int estado = head.optInt("estado", 0);
        if (estado == 401)
            throw new KeyProblem(head.optString("descripcion", "AEMET refused the key"));
        if (estado != 200)
            throw new IOException("AEMET " + estado + ": " + head.optString("descripcion", ""));
        final String datos = head.optString("datos", "");
        if (datos.isEmpty())
            throw new IOException("AEMET gave no data URL");
        return Http.get(datos, maxBytes);
    }

    /**
     * AEMET declares ISO-8859-15 for the data documents. Valid UTF-8 is taken as UTF-8
     * (some endpoints have switched), anything else as ISO-8859-15.
     */
    static String decode(byte[] b) {
        final String utf8 = new String(b, Charset.forName("UTF-8"));
        if (!utf8.contains("�"))
            return utf8;
        return new String(b, Charset.forName("ISO-8859-15"));
    }
}
