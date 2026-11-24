package com.example.touchflow.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.touchflow.preferences.TouchFlowPreferences;

import java.util.Locale;

public final class LocaleHelper {

    public static final String LANG_INDONESIAN = "id";
    public static final String LANG_ENGLISH = "en";

    private LocaleHelper() {}

    private static Context resolveSafeContext(Context context) {
        Context appCtx = context.getApplicationContext();
        return appCtx != null ? appCtx : context;
    }

    public static String getSavedLanguage(Context context) {
        if (context == null) {
            return LANG_INDONESIAN;
        }
        SharedPreferences prefs = resolveSafeContext(context)
                .getSharedPreferences(TouchFlowPreferences.PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs == null) {
            return LANG_INDONESIAN;
        }
        String code = prefs.getString(TouchFlowPreferences.KEY_APP_LANGUAGE, LANG_INDONESIAN);
        return LANG_ENGLISH.equals(code) ? LANG_ENGLISH : LANG_INDONESIAN;
    }

    public static boolean isIndonesian(Context context) {
        return !LANG_ENGLISH.equals(getSavedLanguage(context));
    }

    public static Context wrap(Context base) {
        if (base == null) {
            return null;
        }
        String lang = getSavedLanguage(base);
        Locale locale = LANG_ENGLISH.equals(lang)
                ? Locale.ENGLISH
                : new Locale("id", "ID");
        Locale.setDefault(locale);

        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }

    public static void setLanguageAndApply(Context context, String langCode) {
        String normalized = LANG_ENGLISH.equals(langCode) ? LANG_ENGLISH : LANG_INDONESIAN;
        TouchFlowPreferences prefs = new TouchFlowPreferences(context);
        prefs.setAppLanguage(normalized);

        LocaleListCompat locales = LocaleListCompat.forLanguageTags(normalized);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    public static void syncAppCompatLocale(Context context) {
        String normalized = getSavedLanguage(context);
        LocaleListCompat target = LocaleListCompat.forLanguageTags(normalized);
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        if (current.isEmpty() || !normalized.equals(current.toLanguageTags())) {
            AppCompatDelegate.setApplicationLocales(target);
        }
    }
}
