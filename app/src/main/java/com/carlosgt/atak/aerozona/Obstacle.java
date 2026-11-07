package com.carlosgt.atak.aerozona;

import java.util.Locale;

/** One record of the FAA Digital Obstacle File, as the plugin uses it. */
public final class Obstacle {

    /** FAA's number for it, e.g. "06-012345". */
    public final String oas;
    /** FAA's type code, trimmed: "TOWER", "T-L TWR", "CATENARY". */
    public final String typeCode;
    public final double lat;
    public final double lon;
    /** Height above the ground, feet. */
    public final double aglFt;
    /** Height of the top above sea level, feet. */
    public final double amslFt;
    /** FAA lighting code. */
    public final String lighting;
    /** "O" verified, "U" unverified. */
    public final String verified;
    public final int quantity;
    public final String city;
    public final String state;
    /** Distance from the point the list measures from, meters; re-measured as that moves. */
    public double distanceM;
    /** True bearing from that point to the obstacle, degrees 0 to 360; re-measured with it. */
    public double bearingDeg = Double.NaN;

    private static final String[] POINTS = {
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW" };

    private static final String[] POINTS_ES = {
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO" };

    /** The bearing as a 16-point compass word: "SSW". Empty when there is none. */
    public String compassPoint() {
        if (Double.isNaN(bearingDeg))
            return "";
        final int i = (int) Math.floor(((bearingDeg % 360d + 360d) % 360d + 11.25d) / 22.5d) % 16;
        return Lang.es() ? POINTS_ES[i] : POINTS[i];
    }

    /** "1.5 mi SSW", the distance already formatted. */
    public String distanceAndPoint(String distance) {
        final String pt = compassPoint();
        return pt.isEmpty() ? distance : distance + " " + pt;
    }

    public Obstacle(String oas, String typeCode, double lat, double lon, double aglFt,
            double amslFt, String lighting, String verified, int quantity, String city,
            String state, double distanceM) {
        this.oas = oas;
        this.typeCode = typeCode;
        this.lat = lat;
        this.lon = lon;
        this.aglFt = aglFt;
        this.amslFt = amslFt;
        this.lighting = lighting;
        this.verified = verified;
        this.quantity = quantity;
        this.city = city;
        this.state = state;
        this.distanceM = distanceM;
    }

    /**
     * The group a type code belongs to, for the Types setting. FAA has about 30
     * codes; a pilot picks from a handful of kinds. The key is what the setting
     * stores, so it never changes once shipped.
     */
    public String group() {
        switch (typeCode) {
            case "T-L TWR":
                return "transmission";
            case "CATENARY":
                return "wires";
            case "WINDMILL":
                return "turbines";
            case "TOWER":
            case "CTRL TWR":
            case "BLDG-TWR":
            case "NAVAID":
            case "SPIRE":
            case "MONUMENT":
                return "towers";
            case "STACK":
            case "TANK":
            case "REFINERY":
            case "POWER PLANT":
            case "RIG":
            case "CRANE":
            case "DOME":
            case "ELEC SYS":
            case "GEN UTIL":
            case "DAM":
            case "BRIDGE":
                return "structures";
            case "BLDG":
                return "buildings";
            case "POLE":
            case "UTILITY POLE":
                return "poles";
            case "SOLAR PANELS":
            case "FENCE":
            case "SIGN":
                return "low";
            default:
                return "other";
        }
    }

    /**
     * Nothing under this height above ground is shown, by default.
     *
     * <p>The FAA file lists every obstruction-study filing, down to a 20 ft solar array
     * on a ballfield. Operator, 2026-10-06: "remove the 20 ft shit". 148 obstacles
     * became 31 at Corona with this and the group defaults applied.
     */
    public static final double DEFAULT_MIN_AGL_FT = 50d;

    /** What the floor can be set to. */
    public static final double[] MIN_AGL_PRESETS_FT = { 0d, 50d, 100d, 200d };

    /** Every group, in the order the tiles show them. */
    public static final String[] GROUPS = {
            "transmission", "wires", "towers", "turbines", "structures",
            "buildings", "poles", "low", "other" };

    /** On by default: what an aircraft can hit. Buildings, poles and solar panels are not. */
    public static final java.util.Set<String> DEFAULT_ON = new java.util.HashSet<>(
            java.util.Arrays.asList("transmission", "wires", "towers", "turbines",
                    "structures", "other"));

    public static String groupName(String group) {
        switch (group) {
            case "transmission":
                return Lang.t("Torres de transmisión", "Transmission towers");
            case "wires":
                return Lang.t("Tramos de cable", "Wire spans");
            case "towers":
                return Lang.t("Torres", "Towers");
            case "turbines":
                return Lang.t("Aerogeneradores", "Wind turbines");
            case "structures":
                return Lang.t("Chimeneas y tanques", "Stacks and tanks");
            case "buildings":
                return Lang.t("Edificios", "Buildings");
            case "poles":
                return Lang.t("Postes", "Poles");
            case "low":
                return Lang.t("Solar, vallas, señales", "Solar, fences, signs");
            default:
                return Lang.t("Otros", "Other");
        }
    }

    /** True when the top is at or above the ceiling: it sticks up through it. */
    public boolean aboveCeiling(double ceilingMslFt) {
        return amslFt >= ceilingMslFt;
    }

    /** The kind, in words a pilot uses, from FAA's code. */
    public String kind() {
        switch (typeCode) {
            case "TOWER":
                return Lang.t("Torre", "Tower");
            case "T-L TWR":
                return Lang.t("Torre de transmisión", "Transmission tower");
            case "CATENARY":
                return Lang.t("Tramo de cable", "Wire span");
            case "POLE":
                return Lang.t("Poste", "Pole");
            case "UTILITY POLE":
                return Lang.t("Poste de servicios", "Utility pole");
            case "WINDMILL":
                return Lang.t("Aerogenerador", "Wind turbine");
            case "BLDG":
                return Lang.t("Edificio", "Building");
            case "STACK":
                return Lang.t("Chimenea", "Stack");
            case "TANK":
                return Lang.t("Tanque", "Tank");
            case "FENCE":
                return Lang.t("Valla", "Fence");
            case "SIGN":
                return Lang.t("Señal", "Sign");
            case "SOLAR PANELS":
                return Lang.t("Paneles solares", "Solar panels");
            case "NAVAID":
                return Lang.t("Ayuda a la navegación", "Navaid");
            case "GEN UTIL":
                return Lang.t("Estructura de servicios", "Utility structure");
            case "ELEC SYS":
                return Lang.t("Estructura eléctrica", "Electrical structure");
            case "CRANE":
                return Lang.t("Grúa", "Crane");
            case "BRIDGE":
                return Lang.t("Puente", "Bridge");
            case "DAM":
                return Lang.t("Presa", "Dam");
            case "POWER PLANT":
                return Lang.t("Central eléctrica", "Power plant");
            case "REFINERY":
                return Lang.t("Refinería", "Refinery");
            case "BLDG-TWR":
                return Lang.t("Edificio con torre", "Building with tower");
            case "CTRL TWR":
                return Lang.t("Torre de control", "Control tower");
            case "MONUMENT":
                return Lang.t("Monumento", "Monument");
            case "SPIRE":
                return Lang.t("Aguja", "Spire");
            case "DOME":
                return Lang.t("Cúpula", "Dome");
            case "RIG":
                return Lang.t("Plataforma", "Rig");
            case "VERTICAL STRUCTURE":
                return Lang.t("Estructura vertical", "Vertical structure");
            default:
                return titleCase(typeCode);
        }
    }

    /** "lit", "unlit" or "lighting unknown", from FAA's lighting code. */
    public String lightingWords() {
        if (lighting == null || lighting.isEmpty() || "U".equals(lighting))
            return Lang.t("balizamiento desconocido", "lighting unknown");
        if ("N".equals(lighting))
            return Lang.t("sin balizamiento", "unlit");
        return Lang.t("con balizamiento", "lit");
    }

    /**
     * What the FAA's lighting code means, in words, for the details page.
     *
     * <p>{@link #lightingWords()} answers "lit or not" for a list row. This answers the
     * question a pilot actually has at low level, which is what they are looking for in
     * the dark: a steady red is not a white strobe. The codes are the ones in the FAA's
     * DOF readme, column 96.
     *
     * <p>An unknown code is reported as itself rather than guessed at. The FAA has added
     * codes before, and inventing a meaning for one would be worse than saying so.
     */
    public String lightingDetail() {
        final String c = lighting == null ? "" : lighting.trim().toUpperCase(Locale.US);
        if (c.isEmpty())
            return Lang.t("no registrado", "not recorded");
        switch (c) {
            case "N": return Lang.t("ninguno", "none");
            case "U": return Lang.t("no registrado", "not recorded");
            case "R": return Lang.t("rojo", "red");
            case "W": return Lang.t("rojo sincronizado", "synchronized red");
            case "D": return Lang.t("destellos blancos y rojo, media intensidad", "white strobe and red, medium intensity");
            case "H": return Lang.t("destellos blancos y rojo, alta intensidad", "white strobe and red, high intensity");
            case "M": return Lang.t("destellos blancos, media intensidad", "white strobe, medium intensity");
            case "S": return Lang.t("destellos blancos, alta intensidad", "white strobe, high intensity");
            case "F": return Lang.t("iluminado por proyectores", "floodlit");
            case "C": return Lang.t("catenaria media dual, en tramo de cable", "dual medium catenary, on the wire span");
            case "L": return Lang.t("con balizamiento, tipo no registrado", "lit, type not recorded");
            default:  return Lang.t("código ", "code ") + c + Lang.t(", no en lista publicada por FAA", ", not in the FAA's published list");
        }
    }

    public boolean isVerified() {
        return "O".equals(verified);
    }

    private static String titleCase(String s) {
        if (s == null || s.isEmpty())
            return Lang.t("Obstáculo", "Obstacle");
        final String lower = s.toLowerCase(Locale.US);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
