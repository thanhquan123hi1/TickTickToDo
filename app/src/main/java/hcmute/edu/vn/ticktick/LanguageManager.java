package hcmute.edu.vn.ticktick;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Manages app language selection and locale configuration.
 *
 * Supported language codes:
 *   - {@link #LANG_SYSTEM}  : Follow the device system language.
 *   - {@link #LANG_VI}      : Vietnamese (default fallback).
 *   - {@link #LANG_EN}      : English.
 *   - {@link #LANG_ZH}      : Chinese (Simplified).
 */
public class LanguageManager {

    public static final String LANG_SYSTEM = "system";
    public static final String LANG_VI     = "vi";
    public static final String LANG_EN     = "en";
    public static final String LANG_ZH     = "zh";

    private static final String PREFS_NAME  = "app_prefs";
    private static final String KEY_LANG    = "selected_language";

    // -------------------------------------------------------------------------
    // Preference helpers
    // -------------------------------------------------------------------------

    /**
     * Saves the user's language preference.
     *
     * @param context Application context.
     * @param langCode One of {@link #LANG_SYSTEM}, {@link #LANG_VI}, {@link #LANG_EN}, {@link #LANG_ZH}.
     */
    public static void saveLanguage(Context context, String langCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, langCode).apply();
    }

    /**
     * Returns the saved language preference.
     * Defaults to {@link #LANG_VI} if nothing has been saved yet.
     */
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, LANG_VI);
    }

    // -------------------------------------------------------------------------
    // Locale resolution
    // -------------------------------------------------------------------------

    /**
     * Returns the effective {@link Locale} to use based on the saved preference.
     * When the setting is {@link #LANG_SYSTEM}, returns the system default locale
     * only if the device language is one of the supported languages; otherwise
     * falls back to Vietnamese.
     *
     * @param context Any context.
     */
    public static Locale resolveLocale(Context context) {
        String saved = getSavedLanguage(context);
        switch (saved) {
            case LANG_EN:
                return Locale.ENGLISH;
            case LANG_ZH:
                return Locale.SIMPLIFIED_CHINESE;
            case LANG_SYSTEM:
                return resolveSystemLocale();
            case LANG_VI:
            default:
                return Locale.forLanguageTag("vi");
        }
    }

    /**
     * Returns the system locale if supported (vi / en / zh*), otherwise Vietnamese.
     */
    private static Locale resolveSystemLocale() {
        Locale systemLocale = android.os.LocaleList.getDefault().get(0);
        String lang = systemLocale.getLanguage();
        if (lang.equals("vi") || lang.equals("en") || lang.startsWith("zh")) {
            return systemLocale;
        }
        // Default to Vietnamese for unsupported system locales.
        return Locale.forLanguageTag("vi");
    }

    // -------------------------------------------------------------------------
    // Context wrapping
    // -------------------------------------------------------------------------

    /**
     * Wraps the given context with the correct locale configuration.
     * Call this in {@code attachBaseContext()} of every Activity.
     *
     * @param context The base context to wrap.
     * @return A context configured for the resolved locale.
     */
    public static Context wrapContext(Context context) {
        Locale locale = resolveLocale(context);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
}



