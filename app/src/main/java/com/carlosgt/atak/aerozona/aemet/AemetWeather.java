package com.carlosgt.atak.aerozona.aemet;

import com.carlosgt.atak.aerozona.Lang;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

/**
 * What AEMET says about the weather where the operator is: the hourly forecast of the
 * nearest municipality and the latest reading of the nearest station (real wind and gust,
 * which is what decides a drone flight).
 *
 * <p>Both are cached on disk: the forecast an hour per municipality, the national
 * observation file half an hour. Plain Java, no ATAK types.
 */
public final class AemetWeather {

    public static final long FORECAST_AGE_MS = 60L * 60L * 1000L;
    public static final long OBS_AGE_MS = 30L * 60L * 1000L;
    /** The station list barely changes. */
    public static final long INVENTORY_AGE_MS = 30L * 24L * 3600L * 1000L;
    /** Stations tried, nearest first, until one has recent data. */
    static final int STATIONS_TRIED = 3;

    /** One municipality from assets/municipios.csv (Wikidata, CC0). */
    public static final class Municipio {
        public final String ine, name;
        public final double lat, lon;

        Municipio(String ine, double lat, double lon, String name) {
            this.ine = ine;
            this.lat = lat;
            this.lon = lon;
            this.name = name;
        }
    }

    /** One forecast hour. NaN / empty when AEMET did not give the value. */
    public static final class Hour {
        public long timeMs;
        public double tempC = Double.NaN, windKmh = Double.NaN, gustKmh = Double.NaN;
        public double precipMm = Double.NaN, probPrecip = Double.NaN, probStorm = Double.NaN;
        public String windDir = "", sky = "";
    }

    /** The nearest station's latest reading. */
    public static final class Obs {
        public String id = "", name = "", time = "";
        public double lat, lon, distKm;
        public double tempC = Double.NaN, windMs = Double.NaN, gustMs = Double.NaN;
        public double dirDeg = Double.NaN, visKm = Double.NaN, precMm = Double.NaN;
        public double hr = Double.NaN;
    }

    public static final class Report {
        public Municipio municipio;
        public double municipioKm;
        public final List<Hour> hours = new ArrayList<>();
        public Obs obs;
        public String forecastError, obsError;
        public List<AemetAlerts.Alert> alerts;
        public String alertsError;
    }

    private static List<Municipio> municipios;

    private AemetWeather() {
    }

    /** Loads the municipality list once from the plugin's asset. */
    public static synchronized void loadMunicipios(InputStream in) throws IOException {
        if (municipios != null)
            return;
        final List<Municipio> out = new ArrayList<>(8200);
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(in, Charset.forName("UTF-8")))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty())
                    continue;
                final String[] p = line.split(";", 4);
                if (p.length < 4)
                    continue;
                try {
                    out.add(new Municipio(p[0], Double.parseDouble(p[1]),
                            Double.parseDouble(p[2]), p[3]));
                } catch (NumberFormatException ignored) {
                    // a bad row is skipped, not fatal
                }
            }
        }
        municipios = out;
    }

    static Municipio nearestMunicipio(double lat, double lon) {
        Municipio best = null;
        double bestD = Double.MAX_VALUE;
        if (municipios == null)
            return null;
        for (Municipio m : municipios) {
            final double d = km(lat, lon, m.lat, m.lon);
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    /** Everything for one point. Worker thread only; never throws, errors go in the report. */
    public static Report forPoint(AemetClient client, File dir, double lat, double lon) {
        final Report r = new Report();
        r.municipio = nearestMunicipio(lat, lon);
        if (r.municipio != null) {
            r.municipioKm = km(lat, lon, r.municipio.lat, r.municipio.lon);
            try {
                final String json = cached(client, dir, "horaria_" + r.municipio.ine + ".json.gz",
                        "prediccion/especifica/municipio/horaria/" + r.municipio.ine,
                        FORECAST_AGE_MS, 4 * 1024 * 1024);
                parseHourly(json, System.currentTimeMillis(), r.hours);
            } catch (Exception e) {
                r.forecastError = describe(e);
            }
        }
        try {
            r.obs = nearestStationObs(client, dir, lat, lon);
        } catch (Exception e) {
            r.obsError = describe(e);
        }
        try {
            r.alerts = AemetAlerts.forPoint(client, dir, lat, lon);
        } catch (Exception e) {
            r.alertsError = describe(e);
        }
        return r;
    }

    static String describe(Exception e) {
        if (e instanceof AemetClient.KeyProblem)
            return Lang.t("Falta la clave de AEMET o no es válida (Preferencias de AeroZona).",
                    "AEMET key missing or not valid (AeroZona preferences).");
        final String m = e.getMessage();
        return m == null ? e.getClass().getSimpleName() : m;
    }

    /** The data for an endpoint, from disk when fresh, else from AEMET; stale disk on failure. */
    private static String cached(AemetClient client, File dir, String file, String path,
            long maxAge, int maxBytes) throws IOException {
        final File f = new File(dir, file);
        final boolean fresh = f.isFile() && System.currentTimeMillis() - f.lastModified() < maxAge;
        if (fresh)
            return new String(com.carlosgt.atak.aerozona.EnaireUasFeed.readGz(f),
                    Charset.forName("UTF-8"));
        try {
            final String body = client.get(path, maxBytes);
            if (!dir.isDirectory() && !dir.mkdirs())
                throw new IOException("cannot create " + dir);
            final File tmp = new File(dir, file + ".tmp");
            try (OutputStream out = new GZIPOutputStream(new FileOutputStream(tmp))) {
                out.write(body.getBytes(Charset.forName("UTF-8")));
            }
            if (!tmp.renameTo(f))
                throw new IOException("cannot store " + f);
            return body;
        } catch (IOException e) {
            if (f.isFile() && !(e instanceof AemetClient.KeyProblem))
                return new String(com.carlosgt.atak.aerozona.EnaireUasFeed.readGz(f),
                        Charset.forName("UTF-8"));
            throw e;
        }
    }

    /** The next 12 hours of the hourly municipal forecast (wind and gust in km/h). */
    static void parseHourly(String json, long nowMs, List<Hour> out) throws Exception {
        final JSONArray root = new JSONArray(json);
        final JSONObject pred = root.getJSONObject(0).getJSONObject("prediccion");
        final JSONArray dias = pred.getJSONArray("dia");
        final TimeZone madrid = TimeZone.getTimeZone("Europe/Madrid");
        final SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        day.setTimeZone(madrid);
        for (int d = 0; d < dias.length(); d++) {
            final JSONObject dia = dias.getJSONObject(d);
            final String fecha = dia.optString("fecha", "");
            if (fecha.length() < 10)
                continue;
            final Date base = day.parse(fecha.substring(0, 10));
            for (int h = 0; h < 24; h++) {
                final String per = String.format(Locale.US, "%02d", h);
                final Calendar c = Calendar.getInstance(madrid);
                c.setTime(base);
                c.set(Calendar.HOUR_OF_DAY, h);
                final long t = c.getTimeInMillis();
                if (t < nowMs - 30L * 60L * 1000L || t > nowMs + 12L * 3600L * 1000L)
                    continue;
                final Hour hr = new Hour();
                hr.timeMs = t;
                hr.tempC = num(value(dia.optJSONArray("temperatura"), per));
                hr.precipMm = num(value(dia.optJSONArray("precipitacion"), per));
                hr.probPrecip = num(rangeValue(dia.optJSONArray("probPrecipitacion"), h));
                hr.probStorm = num(rangeValue(dia.optJSONArray("probTormenta"), h));
                final JSONObject sky = entry(dia.optJSONArray("estadoCielo"), per);
                if (sky != null)
                    hr.sky = sky.optString("descripcion", "");
                windAt(dia.optJSONArray("vientoAndRachaMax"), per, hr);
                if (!Double.isNaN(hr.tempC) || !Double.isNaN(hr.windKmh))
                    out.add(hr);
            }
        }
    }

    /**
     * vientoAndRachaMax mixes two kinds of entry for the same hour: one with
     * direccion/velocidad (wind) and one with only value (gust).
     */
    private static void windAt(JSONArray a, String per, Hour hr) {
        if (a == null)
            return;
        for (int i = 0; i < a.length(); i++) {
            final JSONObject o = a.optJSONObject(i);
            if (o == null || !per.equals(o.optString("periodo", "")))
                continue;
            final JSONArray dir = o.optJSONArray("direccion");
            final JSONArray vel = o.optJSONArray("velocidad");
            if (dir != null && dir.length() > 0)
                hr.windDir = dir.optString(0, "");
            if (vel != null && vel.length() > 0)
                hr.windKmh = num(vel.optString(0, ""));
            else if (o.has("value"))
                hr.gustKmh = num(o.optString("value", ""));
        }
    }

    private static JSONObject entry(JSONArray a, String per) {
        if (a == null)
            return null;
        for (int i = 0; i < a.length(); i++) {
            final JSONObject o = a.optJSONObject(i);
            if (o != null && per.equals(o.optString("periodo", "")))
                return o;
        }
        return null;
    }

    private static String value(JSONArray a, String per) {
        final JSONObject o = entry(a, per);
        return o == null ? "" : o.optString("value", "");
    }

    /** Probabilities come in 6-hour periods written "0208", "0814"...: the one holding h. */
    private static String rangeValue(JSONArray a, int h) {
        if (a == null)
            return "";
        for (int i = 0; i < a.length(); i++) {
            final JSONObject o = a.optJSONObject(i);
            if (o == null)
                continue;
            final String p = o.optString("periodo", "");
            if (p.length() == 4) {
                try {
                    final int from = Integer.parseInt(p.substring(0, 2));
                    final int to = Integer.parseInt(p.substring(2, 4));
                    final boolean in = from < to ? (h >= from && h < to) : (h >= from || h < to);
                    if (in)
                        return o.optString("value", "");
                } catch (NumberFormatException ignored) {
                    // skip
                }
            } else if (p.length() == 2 && p.equals(String.format(Locale.US, "%02d", h))) {
                return o.optString("value", "");
            }
        }
        return "";
    }

    /** The nearest station (within 60 km) with its most recent reading. */
    /**
     * The latest observation of the nearest station with data. Downloads the station list
     * (~170 KB, kept a month) and one station's last 24 h (~4 KB), not the whole country
     * (~7 MB, which ran ATAK out of memory).
     */
    static Obs nearestStationObs(AemetClient client, File dir, double lat, double lon)
            throws Exception {
        // Left by 1.0.x, which downloaded the whole country; no longer read.
        final File legacy = new File(dir, "obs_todas.json.gz");
        if (legacy.isFile() && !legacy.delete())
            legacy.deleteOnExit();
        final String inv = cached(client, dir, "inventario.json.gz",
                "valores/climatologicos/inventarioestaciones/todasestaciones",
                INVENTORY_AGE_MS, 2 * 1024 * 1024);
        final List<double[]> cerca = new ArrayList<>();
        final List<String> ids = new ArrayList<>();
        nearestStations(inv, lat, lon, STATIONS_TRIED, ids, cerca);
        Exception last = null;
        for (String id : ids) {
            if (!id.matches("[0-9A-Z]{4,6}"))
                continue;
            try {
                final String json = cached(client, dir, "obs_" + id + ".json.gz",
                        "observacion/convencional/datos/estacion/" + id, OBS_AGE_MS,
                        512 * 1024);
                final Obs o = nearestObs(json, lat, lon);
                if (o != null)
                    return o;
            } catch (Exception e) {
                last = e; // a climatological-only station has no current data: try the next
            }
        }
        if (last != null)
            throw last;
        return null;
    }

    /** The n stations nearest to a point (within 60 km), nearest first. */
    static void nearestStations(String json, double lat, double lon, int n, List<String> ids,
            List<double[]> where) throws Exception {
        final JSONArray a = new JSONArray(json);
        final List<Object[]> all = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            final JSONObject o = a.optJSONObject(i);
            if (o == null)
                continue;
            final double la = dms(o.optString("latitud", "")), lo = dms(o.optString("longitud", ""));
            if (Double.isNaN(la) || Double.isNaN(lo))
                continue;
            final double d = km(lat, lon, la, lo);
            if (d <= 60)
                all.add(new Object[] {
                        d, o.optString("indicativo", ""), la, lo
                });
        }
        java.util.Collections.sort(all, new java.util.Comparator<Object[]>() {
            @Override
            public int compare(Object[] x, Object[] y) {
                return Double.compare((Double) x[0], (Double) y[0]);
            }
        });
        for (int i = 0; i < all.size() && ids.size() < n; i++) {
            ids.add((String) all.get(i)[1]);
            where.add(new double[] {
                    (Double) all.get(i)[2], (Double) all.get(i)[3]
            });
        }
    }

    /** AEMET inventory coordinates: "394924N" / "025309W" = degrees, minutes, seconds. */
    static double dms(String s) {
        if (s == null || !s.matches("[0-9]{6,7}[NSEW]"))
            return Double.NaN;
        final int k = s.length() - 7; // 0 for DDMMSSx, 1 for DDDMMSSx
        final double g = Integer.parseInt(s.substring(0, 2 + k));
        final double m = Integer.parseInt(s.substring(2 + k, 4 + k));
        final double sec = Integer.parseInt(s.substring(4 + k, 6 + k));
        final double v = g + m / 60 + sec / 3600;
        final char h = s.charAt(s.length() - 1);
        return h == 'S' || h == 'W' ? -v : v;
    }

    static Obs nearestObs(String json, double lat, double lon) throws Exception {
        final JSONArray a = new JSONArray(json);
        Obs best = null;
        for (int i = 0; i < a.length(); i++) {
            final JSONObject o = a.optJSONObject(i);
            if (o == null || !o.has("lat") || !o.has("lon"))
                continue;
            final double la = o.optDouble("lat", Double.NaN), lo = o.optDouble("lon", Double.NaN);
            if (Double.isNaN(la) || Double.isNaN(lo))
                continue;
            final double d = km(lat, lon, la, lo);
            if (d > 60)
                continue;
            final String time = o.optString("fint", "");
            final boolean better = best == null || d < best.distKm - 0.01
                    || (Math.abs(d - best.distKm) < 0.01 && time.compareTo(best.time) > 0);
            if (!better)
                continue;
            final Obs b = new Obs();
            b.id = o.optString("idema", "");
            b.name = o.optString("ubi", "");
            b.time = time;
            b.lat = la;
            b.lon = lo;
            b.distKm = d;
            b.tempC = o.optDouble("ta", Double.NaN);
            b.windMs = o.optDouble("vv", Double.NaN);
            b.gustMs = o.optDouble("vmax", Double.NaN);
            b.dirDeg = o.optDouble("dv", Double.NaN);
            b.visKm = o.optDouble("vis", Double.NaN);
            b.precMm = o.optDouble("prec", Double.NaN);
            b.hr = o.optDouble("hr", Double.NaN);
            best = b;
        }
        return best;
    }

    /** The page text. Wind and gust in km/h and m/s; a gust of 10 m/s or more is flagged. */
    public static String text(Report r) {
        final StringBuilder b = new StringBuilder();
        final SimpleDateFormat hh = new SimpleDateFormat("EEE HH:mm", Locale.getDefault());
        hh.setTimeZone(TimeZone.getTimeZone(canary(r) ? "Atlantic/Canary" : "Europe/Madrid"));
        alertsText(r, b);
        if (r.obs != null) {
            final Obs o = r.obs;
            b.append(Lang.t("OBSERVADO", "OBSERVED")).append(" · ").append(o.name)
                    .append(String.format(Locale.getDefault(), " (%.0f km)", o.distKm))
                    .append('\n');
            if (!o.time.isEmpty())
                b.append(obsTime(o.time, hh)).append('\n');
            if (!Double.isNaN(o.windMs))
                b.append(Lang.t("Viento: ", "Wind: ")).append(ms(o.windMs))
                        .append(Double.isNaN(o.dirDeg) ? "" : String.format(Locale.US, " %03.0f°", o.dirDeg))
                        .append('\n');
            if (!Double.isNaN(o.gustMs))
                b.append(Lang.t("Racha: ", "Gust: ")).append(ms(o.gustMs))
                        .append(o.gustMs >= 10 ? "  ⚠" : "").append('\n');
            if (!Double.isNaN(o.tempC))
                b.append(Lang.t("Temperatura: ", "Temperature: "))
                        .append(String.format(Locale.getDefault(), "%.1f °C", o.tempC)).append('\n');
            if (!Double.isNaN(o.visKm))
                b.append(Lang.t("Visibilidad: ", "Visibility: "))
                        .append(String.format(Locale.getDefault(), "%.0f km", o.visKm)).append('\n');
            if (!Double.isNaN(o.precMm) && o.precMm > 0)
                b.append(Lang.t("Precipitación: ", "Precipitation: "))
                        .append(String.format(Locale.getDefault(), "%.1f mm", o.precMm)).append('\n');
        } else if (r.obsError != null) {
            b.append(Lang.t("Observación no disponible: ", "Observation not available: "))
                    .append(r.obsError).append('\n');
        }
        b.append('\n');
        if (r.municipio != null)
            b.append(Lang.t("PREDICCIÓN · ", "FORECAST · ")).append(r.municipio.name)
                    .append(String.format(Locale.getDefault(), " (%.0f km)", r.municipioKm))
                    .append('\n');
        if (!r.hours.isEmpty()) {
            for (Hour h : r.hours) {
                b.append(hh.format(new Date(h.timeMs))).append("  ");
                if (!Double.isNaN(h.tempC))
                    b.append(String.format(Locale.getDefault(), "%.0f°", h.tempC)).append("  ");
                if (!Double.isNaN(h.windKmh))
                    b.append(h.windDir).append(' ').append(String.format(Locale.US, "%.0f", h.windKmh));
                if (!Double.isNaN(h.gustKmh))
                    b.append(Lang.t(" (racha ", " (gust ")).append(String.format(Locale.US, "%.0f", h.gustKmh))
                            .append(')').append(h.gustKmh >= 36 ? "⚠" : "");
                b.append(" km/h");
                if (!Double.isNaN(h.probPrecip) && h.probPrecip > 0)
                    b.append(String.format(Locale.US, "  💧%.0f%%", h.probPrecip));
                if (!Double.isNaN(h.probStorm) && h.probStorm > 0)
                    b.append(String.format(Locale.US, "  ⚡%.0f%%", h.probStorm));
                if (!h.sky.isEmpty())
                    b.append("  ").append(Lang.es() ? h.sky : skyEn(h.sky));
                b.append('\n');
            }
        } else if (r.forecastError != null) {
            b.append(Lang.t("Predicción no disponible: ", "Forecast not available: "))
                    .append(r.forecastError).append('\n');
        }
        b.append('\n').append(Lang.t("Fuente: AEMET (© AEMET).", "Source: AEMET (© AEMET)."));
        if (gusty(r))
            b.append(' ').append(Lang.t(
                    "⚠ racha de 10 m/s (36 km/h) o más: por encima del límite habitual de drones"
                            + " pequeños; compruebe el de su aeronave.",
                    "⚠ gust of 10 m/s (36 km/h) or more: above the usual limit of small drones;"
                            + " check your aircraft's."));
        return b.toString();
    }

    /** Meteoalerta first: a warning matters more than the hourly numbers. */
    static void alertsText(Report r, StringBuilder b) {
        final SimpleDateFormat dh = new SimpleDateFormat("EEE d HH:mm", Locale.getDefault());
        dh.setTimeZone(TimeZone.getTimeZone(canary(r) ? "Atlantic/Canary" : "Europe/Madrid"));
        if (r.alerts != null && !r.alerts.isEmpty()) {
            b.append(Lang.t("AVISOS AEMET (Meteoalerta)", "AEMET WARNINGS (Meteoalerta)"))
                    .append('\n');
            for (AemetAlerts.Alert a : r.alerts) {
                b.append(a.rank() == 3 ? "🟥 " : a.rank() == 2 ? "🟧 " : "🟨 ")
                        .append(a.headline.isEmpty() ? a.phenomenon : a.headline).append('\n');
                if (a.onsetMs > 0 || a.expiresMs > 0)
                    b.append("   ").append(a.onsetMs > 0 ? dh.format(new Date(a.onsetMs)) : "")
                            .append(" → ")
                            .append(a.expiresMs > 0 ? dh.format(new Date(a.expiresMs)) : "")
                            .append('\n');
                if (!a.description.isEmpty())
                    b.append("   ").append(a.description).append('\n');
            }
            b.append('\n');
        } else if (r.alerts != null) {
            b.append(Lang.t("Sin avisos de AEMET en esta posición.",
                    "No AEMET warnings at this position.")).append("\n\n");
        } else if (r.alertsError != null) {
            b.append(Lang.t("Avisos no disponibles: ", "Warnings not available: "))
                    .append(r.alertsError).append("\n\n");
        }
    }

    /** Maps official AEMET sky descriptions (codes 11-82 and variants) to English. */
    static String skyEn(String es) {
        if (es == null || es.isEmpty())
            return es;
        final String trimmed = es.trim();
        final String lower = trimmed.toLowerCase(Locale.ROOT);
        final boolean night;
        final String key;
        if (lower.endsWith(" noche")) {
            night = true;
            key = lower.substring(0, lower.length() - 6).trim();
        } else {
            night = false;
            key = lower;
        }
        final String en;
        switch (key) {
            case "despejado":
                en = "Clear";
                break;
            case "poco nuboso":
                en = "Partly cloudy";
                break;
            case "intervalos nubosos":
                en = "Cloudy intervals";
                break;
            case "nuboso":
                en = "Cloudy";
                break;
            case "muy nuboso":
                en = "Very cloudy";
                break;
            case "cubierto":
                en = "Overcast";
                break;
            case "nubes altas":
                en = "High clouds";
                break;
            case "intervalos nubosos con lluvia":
                en = "Cloudy intervals with rain";
                break;
            case "nuboso con lluvia":
                en = "Cloudy with rain";
                break;
            case "muy nuboso con lluvia":
                en = "Very cloudy with rain";
                break;
            case "cubierto con lluvia":
                en = "Overcast with rain";
                break;
            case "intervalos nubosos con nieve":
                en = "Cloudy intervals with snow";
                break;
            case "nuboso con nieve":
                en = "Cloudy with snow";
                break;
            case "muy nuboso con nieve":
                en = "Very cloudy with snow";
                break;
            case "cubierto con nieve":
                en = "Overcast with snow";
                break;
            case "intervalos nubosos con lluvia escasa":
                en = "Cloudy intervals with light rain";
                break;
            case "nuboso con lluvia escasa":
                en = "Cloudy with light rain";
                break;
            case "muy nuboso con lluvia escasa":
                en = "Very cloudy with light rain";
                break;
            case "cubierto con lluvia escasa":
                en = "Overcast with light rain";
                break;
            case "intervalos nubosos con tormenta":
                en = "Cloudy intervals with thunderstorm";
                break;
            case "nuboso con tormenta":
                en = "Cloudy with thunderstorm";
                break;
            case "muy nuboso con tormenta":
                en = "Very cloudy with thunderstorm";
                break;
            case "cubierto con tormenta":
                en = "Overcast with thunderstorm";
                break;
            case "intervalos nubosos con tormenta y lluvia escasa":
                en = "Cloudy intervals with thunderstorm and light rain";
                break;
            case "nuboso con tormenta y lluvia escasa":
                en = "Cloudy with thunderstorm and light rain";
                break;
            case "muy nuboso con tormenta y lluvia escasa":
                en = "Very cloudy with thunderstorm and light rain";
                break;
            case "cubierto con tormenta y lluvia escasa":
                en = "Overcast with thunderstorm and light rain";
                break;
            case "intervalos nubosos con nieve escasa":
                en = "Cloudy intervals with light snow";
                break;
            case "nuboso con nieve escasa":
                en = "Cloudy with light snow";
                break;
            case "muy nuboso con nieve escasa":
                en = "Very cloudy with light snow";
                break;
            case "cubierto con nieve escasa":
                en = "Overcast with light snow";
                break;
            case "niebla":
                en = "Fog";
                break;
            case "bruma":
                en = "Mist";
                break;
            case "calima":
                en = "Haze";
                break;
            default:
                return es;
        }
        return night ? en + " night" : en;
    }

    /** Canarias keep their own clock: west of 12° W is the islands. */
    static boolean canary(Report r) {
        final double lon = r.municipio != null ? r.municipio.lon
                : r.obs != null ? r.obs.lon : 0d;
        return lon < -12d;
    }

    /** Whether any gust reaches the warning threshold, observed or forecast. */
    static boolean gusty(Report r) {
        if (r.obs != null && !Double.isNaN(r.obs.gustMs) && r.obs.gustMs >= 10)
            return true;
        for (Hour h : r.hours)
            if (!Double.isNaN(h.gustKmh) && h.gustKmh >= 36)
                return true;
        return false;
    }

    /** "2026-11-07T13:00:00+0000" as local time, or the text itself if it does not parse. */
    static String obsTime(String t, SimpleDateFormat local) {
        try {
            final SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
            return local.format(in.parse(t)) + Lang.t(" (hora local)", " (local time)");
        } catch (Exception e) {
            return t.replace('T', ' ') + " UTC";
        }
    }

    private static String ms(double ms) {
        return String.format(Locale.getDefault(), "%.0f km/h (%.1f m/s)", ms * 3.6d, ms);
    }

    static double num(String s) {
        if (s == null)
            return Double.NaN;
        final String t = s.trim();
        if (t.isEmpty() || "Ip".equalsIgnoreCase(t))
            return "Ip".equalsIgnoreCase(t) ? 0d : Double.NaN;
        try {
            return Double.parseDouble(t.replace(',', '.'));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    static double km(double la1, double lo1, double la2, double lo2) {
        final double r = 6371d, p1 = Math.toRadians(la1), p2 = Math.toRadians(la2);
        final double dp = p2 - p1, dl = Math.toRadians(lo2 - lo1);
        final double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
