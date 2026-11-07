package com.carlosgt.atak.aerozona;

import java.util.Locale;

/**
 * The FAA's type strings, in words an operator reads.
 *
 * <p>"AIR SHOWS/SPORTS" and "UAS PUBLIC GATHERING" are how the feed writes them and not
 * how anybody says them. The label is also the feature set's name, so it is what Overlay
 * Manager lists.
 */
public final class TfrTypes {

    private TfrTypes() {
    }

    public static String label(String raw) {
        if (raw == null || raw.trim().isEmpty())
            return Lang.t("Otros", "Other");
        // Not an FAA restriction type: the airfield chips switch through the same filter.
        if (MetarFeatures.SET_KEY.equals(raw))
            return MetarFeatures.setName();
        final String r = raw.trim().toUpperCase(Locale.US);
        if ("SECURITY".equals(r))
            return Lang.t("Seguridad", "Security");
        if ("HAZARDS".equals(r))
            return Lang.t("Peligros", "Hazards");
        if ("AIR SHOWS/SPORTS".equals(r))
            return Lang.t("Exhibiciones aéreas y deportes", "Air shows and sports");
        if ("UAS PUBLIC GATHERING".equals(r))
            return Lang.t("Concentraciones de personas", "Public gatherings");
        if ("SPACE OPERATIONS".equals(r))
            return Lang.t("Operaciones espaciales", "Space operations");
        if ("VIP".equals(r))
            return Lang.t("Movimiento VIP", "VIP movement");
        if ("SPECIAL".equals(r))
            return Lang.t("Especial", "Special");
        final String s = r.toLowerCase(Locale.US).replace('/', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
