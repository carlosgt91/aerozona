package com.carlosgt.atak.aerozona;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One shelf of airspace: a boundary, a floor and a ceiling, and what it is.
 *
 * <p><b>A shelf, not an airspace.</b> The FAA publishes Class B as one row per step of the
 * upside-down wedding cake, each with its own floor: Los Angeles comes back as twelve rows,
 * {@code SFC/10000} over the airport and then 2,000, 2,500, 4,000, 5,000, 6,000, 7,000,
 * 8,000 and 9,000 further out, all topping at 10,000 MSL. That is exactly the unit a
 * 3D render needs, so the shelf is the object here and nothing tries to reassemble the
 * cake.
 *
 * <p>The vertical fields are encoded the way the TFR XNOTAM encodes them -- a value, a
 * unit and a code saying what it is measured from -- so this reuses {@link TfrArea.Vert}
 * rather than growing a second vocabulary for the same idea.
 *
 * <p>No ATAK or Android types here on purpose: like {@link Tfr} and {@link TfrParser},
 * this can be run against the real feed on a desktop JVM without a device.
 */
public class Airspace {

    /** The two layers these shelves are switched by, on the front page. */
    public static final String LAYER_CLASSES = "airspace";
    public static final String LAYER_SUA = "sua";

    /** {@code OBJECTID}: stable for the life of a publication cycle, which is all we need. */
    public String id = "";
    /** {@code IDENT_TXT}, usually the airport -- KLAX, KONT. Often empty on en-route airspace. */
    public String ident = "";
    /** {@code NAME_TXT}, e.g. "LOS ANGELES CLASS B". */
    public String name = "";
    /** {@code CLASS_CODE}: A through G, "Other", or empty on things that have no class. */
    public String classCode = "";
    /** {@code TYPE_CODE}: CLASS, R, P, W, MOA, A, ARTCC, MODE-C and others. */
    public String typeCode = "";
    /** {@code LOCALTYPE_TXT}, the source's own words when it has any. */
    public String localType = "";
    /** {@code WORKHR_CODE}: when it is active. Empty means continuous. */
    public String workHours = "";

    /**
     * Where the shelf came from. Empty for the original FAA feed, {@link #SOURCE_ENAIRE_UAS}
     * for a Spanish UAS geographic zone (RD 517/2024, ED-318 fields).
     */
    public String source = "";
    public static final String SOURCE_ENAIRE_UAS = "enaire-uas";
    /** An ENAIRE UAS NOTAM area: {@link #uasType} holds the Q code, times are items B/C. */
    public static final String SOURCE_ENAIRE_NOTAM = "enaire-notam";
    /** The NOTAM layer key, the same string TfrManager.LAYER_NOTAMS uses. */
    public static final String LAYER_NOTAMS = "notams";

    public boolean isNotam() {
        return SOURCE_ENAIRE_NOTAM.equals(source);
    }

    /** NOTAM kind from the Q code: UAS activity, restricted/danger area, or other. */
    public String notamKind() {
        final String q = uasType == null ? "" : uasType.trim().toUpperCase(Locale.US);
        if (q.startsWith("WU"))
            return "UAS";
        if (q.startsWith("R"))
            return "AREA";
        return "OTHER";
    }

    /** Whether the NOTAM window holds now (the item D schedule is not evaluated). */
    public boolean inWindow(long nowMs) {
        return (validFromMs == 0 || validFromMs <= nowMs) && (validToMs == 0 || nowMs <= validToMs);
    }

    /** ED-318 {@code type}: PROHIBITED, REQ_AUTHORIZATION, CONDITIONAL, NO_RESTRICTION. */
    public String uasType = "";
    /** ED-318 {@code reasons}: AIR_TRAFFIC, SENSITIVE, PRIVACY, POPULATION, NATURE, OTHER... */
    public String reasons = "";
    /** Which ENAIRE layer: Infraestructuras, Aero or Urbano. */
    public String uasLayer = "";
    /** The zone's own message, HTML stripped, as ENAIRE wrote it. */
    public String message = "";
    /** Who to ask: authority name, e-mail, phone and web, when the zone gives them. */
    public String contactName = "", email = "", phone = "", siteUrl = "";
    /** ENAIRE validity window, epoch ms; 0 when open. */
    public long validFromMs, validToMs;

    public boolean isUasZone() {
        return SOURCE_ENAIRE_UAS.equals(source);
    }

    public final TfrArea.Vert floor = new TfrArea.Vert();
    public final TfrArea.Vert ceiling = new TfrArea.Vert();

    /**
     * The boundary, one entry per disjoint part, each part's first ring the outside and
     * the rest holes. {lat, lon} per vertex, matching {@link TfrArea#ring}.
     */
    public final List<Part> parts = new ArrayList<>();

    /** One closed area: an outer ring and the holes punched in it. */
    public static class Part {
        public final List<double[]> outer = new ArrayList<>();
        public final List<List<double[]>> holes = new ArrayList<>();
    }

    public boolean drawable() {
        for (Part p : parts)
            if (p.outer.size() >= 3)
                return true;
        return false;
    }

    /** Which front-page layer this shelf belongs to, or empty when we do not draw it. */
    public String layer() {
        if (isNotam())
            return LAYER_NOTAMS;
        if (isClass())
            return LAYER_CLASSES;
        if (isSpecialUse())
            return LAYER_SUA;
        return "";
    }

    public boolean isClass() {
        // UAS zones ride on the classes layer: same switch, same pipeline.
        return isUasZone() || "CLASS".equalsIgnoreCase(typeCode);
    }

    /**
     * Special Use Airspace, as the FAA defines the set: Prohibited, Restricted, Warning,
     * Alert and Military Operations Areas. Controlled Firing Areas are in the definition
     * too but are not charted and are not published here.
     */
    public boolean isSpecialUse() {
        final String t = typeCode == null ? "" : typeCode.trim().toUpperCase(Locale.US);
        return "P".equals(t) || "R".equals(t) || "W".equals(t) || "A".equals(t)
                || "MOA".equals(t);
    }

    /**
     * The feature set this lands in. One per class and one per special-use kind, so a
     * single kind can be switched without touching the others -- the same shape the
     * restriction types already use.
     */
    public String setKey() {
        if (isNotam())
            return "enotam:" + notamKind();
        if (isUasZone())
            return "uas:" + (uasType == null || uasType.trim().isEmpty()
                    ? "?" : uasType.trim().toUpperCase(Locale.US));
        if (isClass())
            return "as:" + (classCode == null || classCode.trim().isEmpty()
                    ? "?" : classCode.trim().toUpperCase(Locale.US));
        if (isSpecialUse())
            return "sua:" + typeCode.trim().toUpperCase(Locale.US);
        return "";
    }

    /** What Overlay Manager and the type picker show for {@link #setKey()}. */
    public static String setName(String key) {
        if (key == null)
            return "";
        if (key.startsWith("uas:"))
            return uasTypeName(key.substring(4));
        if (key.startsWith("enotam:")) {
            final String k = key.substring(7);
            if ("UAS".equals(k))
                return Lang.t("NOTAM: actividad con UAS", "NOTAM: UAS activity");
            if ("AREA".equals(k))
                return Lang.t("NOTAM: zona restringida o peligrosa",
                        "NOTAM: restricted or danger area");
            return Lang.t("NOTAM: otros", "NOTAM: other");
        }
        if (key.startsWith("as:"))
            return "Class " + key.substring(3) + " airspace";
        if (key.startsWith("sua:")) {
            final String t = key.substring(4);
            if ("P".equals(t))
                return "Prohibited areas";
            if ("R".equals(t))
                return "Restricted areas";
            if ("W".equals(t))
                return "Warning areas";
            if ("A".equals(t))
                return "Alert areas";
            if ("MOA".equals(t))
                return "Military operations areas";
            return t;
        }
        return key;
    }

    /** ED-318 zone type in words. */
    public static String uasTypeName(String type) {
        final String t = type == null ? "" : type.trim().toUpperCase(Locale.US);
        final boolean es = Lang.es();
        switch (t) {
            case "PROHIBITED":
                return es ? "Vuelo prohibido" : "Flight prohibited";
            case "REQ_AUTHORIZATION":
                return es ? "Requiere autorización" : "Authorization required";
            case "CONDITIONAL":
                return es ? "Vuelo con condiciones" : "Conditions apply";
            case "NO_RESTRICTION":
                return es ? "Sin restricción" : "No restriction";
            default:
                return es ? "Zona UAS" : "UAS zone";
        }
    }

    /** ENAIRE layer in words. */
    public static String uasLayerName(String layer) {
        final boolean es = Lang.es();
        if ("Aero".equals(layer))
            return es ? "Zona aeronáutica" : "Aeronautical zone";
        if ("Urbano".equals(layer))
            return es ? "Zona urbana" : "Urban zone";
        if ("Infraestructuras".equals(layer))
            return es ? "Infraestructura protegida" : "Protected infrastructure";
        return layer == null ? "" : layer;
    }

    /**
     * What the operator is told this is, in a few words.
     *
     * <p>The source's {@code NAME_TXT} is shouted and repeats the class ("LOS ANGELES
     * CLASS B"), so it is title-cased and the class is not said twice.
     */
    public String title() {
        if (isNotam()) {
            final String n = name == null ? "" : name.trim();
            return n.isEmpty() ? ident : n + "  (" + ident + ")";
        }
        if (isUasZone()) {
            final String n = name == null ? "" : name.trim();
            if (!n.isEmpty())
                return n;
            return uasLayerName(uasLayer);
        }
        final String n = titleCase(name);
        if (!n.isEmpty())
            return n;
        if (!ident.isEmpty())
            return ident;
        return setName(setKey());
    }

    /**
     * The one line that answers "what am I under".
     *
     * <p>An unstated ceiling is said as "and up" rather than as "unstated". Class E
     * transition areas have no published top -- they run up to whatever is above them --
     * and printing the absence as a missing field reads as a gap in our data instead of
     * what the airspace actually is.
     */
    public String heights() {
        if (isUasZone() || isNotam()) {
            final boolean es = Lang.es();
            final String lo = !floor.present || floor.surface ? (es ? "superficie" : "surface")
                    : floor.label();
            if (!ceiling.present)
                return lo + (es ? " y por encima" : " and up");
            return lo + (es ? " a " : " to ") + ceiling.label();
        }
        if (!ceiling.present)
            return (floor.present ? floor.label() : "surface") + " and up";
        if (!floor.present)
            return "up to " + ceiling.label();
        return floor.label() + " to " + ceiling.label();
    }

    /** Whether a height in feet MSL is inside this shelf. Surface floors count as zero. */
    public boolean containsAltitudeFt(double mslFeet, double groundFeetMsl) {
        final double lo = heightFt(floor, groundFeetMsl, 0d);
        final double hi = heightFt(ceiling, groundFeetMsl, Double.MAX_VALUE);
        return mslFeet >= lo && mslFeet <= hi;
    }

    private static double heightFt(TfrArea.Vert v, double groundFeetMsl, double absent) {
        if (!v.present)
            return absent;
        if (v.surface)
            return groundFeetMsl;
        return v.agl ? groundFeetMsl + v.feet : v.feet;
    }

    static String titleCase(String s) {
        if (s == null)
            return "";
        final String t = s.trim();
        if (t.isEmpty())
            return "";
        final StringBuilder b = new StringBuilder(t.length());
        boolean start = true;
        for (int i = 0; i < t.length(); i++) {
            final char ch = t.charAt(i);
            if (start)
                b.append(Character.toUpperCase(ch));
            else
                b.append(Character.toLowerCase(ch));
            start = ch == ' ' || ch == '-' || ch == '/' || ch == '(';
        }
        // A one-letter class or a four-letter identifier should not come out as "Class b".
        return fixCaps(b.toString());
    }

    private static String fixCaps(String s) {
        final String[] words = s.split(" ");
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0)
                b.append(' ');
            final String w = words[i];
            if (w.length() == 1 && Character.isLetter(w.charAt(0)))
                b.append(Character.toUpperCase(w.charAt(0)));
            else
                b.append(w);
        }
        return b.toString();
    }
}
