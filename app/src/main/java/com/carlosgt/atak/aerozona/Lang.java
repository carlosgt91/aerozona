package com.carlosgt.atak.aerozona;

import java.util.Locale;

/**
 * Spanish or English for text built in code. Spanish when the device is in Spanish,
 * English otherwise; resources in res/values-es follow the same rule on their own.
 */
public final class Lang {

    private Lang() {
    }

    public static boolean es() {
        return "es".equals(Locale.getDefault().getLanguage());
    }

    public static String t(String es, String en) {
        return es() ? es : en;
    }
}
