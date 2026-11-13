package com.carlosgt.atak.aerozona.net;

import android.util.JsonReader;
import android.util.JsonToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * ArcGIS query answers read as a stream: only one feature at a time is a JSON tree. The
 * NOTAM (~1 MB) and the airspace (several layers) answers ran ATAK out of memory when read
 * as bytes + String + one big JSONObject (2026-11-11 and 2026-11-13).
 */
public final class ArcGisStream {

    private ArcGisStream() {
    }

    /** What is done with each feature; it is dropped after. */
    public interface Visitor {
        void feature(JSONObject f) throws IOException;
    }

    /**
     * Reads one ArcGIS answer object at the reader's position: every element of
     * {@code features} goes to the visitor. An {@code error} object, or no
     * {@code features}, is an IOException.
     */
    public static void answer(JsonReader r, Visitor v) throws IOException {
        boolean features = false;
        try {
            r.beginObject();
            while (r.hasNext()) {
                final String key = r.nextName();
                if ("features".equals(key) && r.peek() == JsonToken.BEGIN_ARRAY) {
                    features = true;
                    r.beginArray();
                    while (r.hasNext()) {
                        final Object f = value(r);
                        if (f instanceof JSONObject)
                            v.feature((JSONObject) f);
                    }
                    r.endArray();
                } else if ("error".equals(key) && r.peek() == JsonToken.BEGIN_OBJECT) {
                    final Object e = value(r);
                    throw new IOException(((JSONObject) e).optString("message", "ENAIRE refused the query"));
                } else {
                    r.skipValue();
                }
            }
            r.endObject();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {   // malformed JSON, or org.json refusing a value
            throw new IOException("ENAIRE answer not readable");
        }
        if (!features)
            throw new IOException("ENAIRE answer not readable");
    }

    /** One JSON value from the stream, as org.json objects. */
    public static Object value(JsonReader r) throws IOException, JSONException {
        switch (r.peek()) {
            case BEGIN_OBJECT: {
                final JSONObject o = new JSONObject();
                r.beginObject();
                while (r.hasNext()) {
                    final String k = r.nextName();
                    o.put(k, value(r));
                }
                r.endObject();
                return o;
            }
            case BEGIN_ARRAY: {
                final JSONArray a = new JSONArray();
                r.beginArray();
                while (r.hasNext())
                    a.put(value(r));
                r.endArray();
                return a;
            }
            case NUMBER: {
                final String n = r.nextString();
                try {
                    return Long.parseLong(n);
                } catch (NumberFormatException notLong) {
                    return Double.parseDouble(n);
                }
            }
            case BOOLEAN:
                return r.nextBoolean();
            case NULL:
                r.nextNull();
                return JSONObject.NULL;
            default:
                return r.nextString();
        }
    }
}
