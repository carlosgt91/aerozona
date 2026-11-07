package com.carlosgt.atak.aerozona.plugin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;

import com.atakmap.android.preference.PluginPreferenceFragment;
import com.carlosgt.atak.aerozona.Lang;
import com.atakmap.android.util.PdfHelper;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;

/**
 * The plugin's entry under ATAK's Tool Preferences, and the only way to reach the
 * user manual.
 *
 * <p>The manual is built by {@code gradle/typst.gradle} into
 * {@code assets/usermanual.pdf}, but an asset is not reachable by anyone: ATAK
 * surfaces a plugin's documentation through this screen, so without it the PDF
 * ships inside the APK and no pilot can open it. That has shipped undetected
 * once, because the PDF really was in the APK.
 *
 * <p>Nothing else lives on this screen on purpose. Everything the operator sets
 * is on the pane, next to what it affects.
 */
public class AirAwarePreferenceFragment extends PluginPreferenceFragment {

    private static final String USER_GUIDE = "usermanual.pdf";

    /**
     * Where the PDF is extracted before a viewer is handed it. Named for what it
     * is rather than for the asset, because this is the name a pilot sees in a
     * file picker.
     */
    private static final String USER_GUIDE_PATH = FileSystemUtils.getRoot()
            + File.separator + "tools" + File.separator + "aerozona"
            + File.separator + "AeroZona User Guide.pdf";

    /** The AEMET sign-up guide, built from docs/ALTA_AEMET_OPENDATA into assets. */
    private static final String AEMET_GUIDE = "alta_aemet.pdf";
    private static final String AEMET_GUIDE_PATH = FileSystemUtils.getRoot()
            + File.separator + "tools" + File.separator + "aerozona"
            + File.separator + "Clave AEMET - guia.pdf";

    /** Where the AEMET key lives: ATAK's default preferences, on this device only. */
    public static final String PREF_AEMET_KEY = "aerozona_aemet_key";

    /**
     * The AEMET key, wherever the preference screen saved it.
     *
     * <p>ATAK inflates this screen with a PreferenceManager built on the plugin
     * context, so on some builds (the tak.gov-signed 1.0 on Play ATAK) the value
     * landed outside ATAK's default preferences and the pane never saw it. The
     * change listener below now copies it there; this also picks up a key saved
     * by 1.0 and moves it across.
     */
    public static String aemetKey(Context atakContext) {
        final android.content.SharedPreferences atak = android.preference.PreferenceManager
                .getDefaultSharedPreferences(atakContext);
        final String key = atak.getString(PREF_AEMET_KEY, "");
        if (key != null && !key.trim().isEmpty() || pluginContext == null)
            return key == null ? "" : key.trim();
        try {
            final String old = android.preference.PreferenceManager
                    .getDefaultSharedPreferences(pluginContext)
                    .getString(PREF_AEMET_KEY, "");
            if (old != null && !old.trim().isEmpty()) {
                atak.edit().putString(PREF_AEMET_KEY, old.trim()).apply();
                return old.trim();
            }
        } catch (RuntimeException unreadable) {
            // A plugin context that cannot open preferences just means no old key.
        }
        return "";
    }

    private static Context pluginContext;

    /**
     * A number that changes with every release, for PdfHelper's cache.
     *
     * <p>{@code versionName} carries PLUGIN_VERSION and the ATAK target and is
     * correct in a tak.gov-signed build, where the git hash is blank. Hashed
     * rather than parsed, because a rebuild for a different ATAK target should
     * refresh the manual too.
     */
    private static long manualVersion() {
        try {
            final String name = pluginContext.getString(R.string.versionName);
            return name.hashCode() & 0xFFFFFFFFL;
        } catch (RuntimeException noResource) {
            // Never let the manual fail to open over its own cache key.
            return System.currentTimeMillis();
        }
    }

    public AirAwarePreferenceFragment() {
        super(pluginContext, R.xml.preferences);
    }

    @SuppressLint("ValidFragment")
    public AirAwarePreferenceFragment(Context context) {
        super(context, R.xml.preferences);
        pluginContext = context;
    }

    @Override
    public String getSubTitle() {
        return getSubTitle(Lang.t("Preferencias de herramientas", "Tool Preferences"), "AeroZona");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Preference aemet = findPreference(PREF_AEMET_KEY);
        if (aemet != null)
            aemet.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    android.preference.PreferenceManager
                            .getDefaultSharedPreferences(getActivity())
                            .edit().putString(PREF_AEMET_KEY,
                                    value == null ? "" : value.toString().trim())
                            .apply();
                    return true;
                }
            });

        final Preference guide = findPreference("aemet_guide");
        if (guide != null)
            guide.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    PdfHelper.extractAndShow(pluginContext, getActivity(), AEMET_GUIDE,
                            manualVersion(), AEMET_GUIDE_PATH, true);
                    return true;
                }
            });

        final Preference manual = findPreference("manual");
        if (manual == null)
            return;
        manual.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        PdfHelper.extractAndShow(pluginContext, getActivity(),
                                USER_GUIDE, manualVersion(), USER_GUIDE_PATH,
                                true);
                        return true;
                    }
                });
    }
}
