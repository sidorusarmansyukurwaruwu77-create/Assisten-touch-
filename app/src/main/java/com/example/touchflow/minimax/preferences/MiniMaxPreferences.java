package com.example.touchflow.minimax.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.touchflow.minimax.models.AutomationLogEntry;
import com.example.touchflow.minimax.models.MiniMaxConfig;
import com.example.touchflow.minimax.models.MiniWindowPosition;
import com.example.touchflow.minimax.models.MiniWindowSize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MiniMaxPreferences {

    public static final String PREFS_NAME = "touchflow_minimax_prefs";
    public static final int MAX_HISTORY_ENTRIES = 30;

    public static final String KEY_AUTO_MINI_ENABLED = "mm_auto_mini_enabled";
    public static final String KEY_FLOATING_ASSISTANT_ENABLED = "mm_floating_assistant_enabled";
    public static final String KEY_AUTO_MINI_ON_HOME = "mm_auto_mini_on_home";
    public static final String KEY_AUTO_MINI_ON_APP_SWITCH = "mm_auto_mini_on_app_switch";
    public static final String KEY_ONLY_SUPPORTED_MODE = "mm_only_supported_mode";
    public static final String KEY_SUPPORTED_PACKAGES = "mm_supported_packages";
    public static final String KEY_EXCLUDED_PACKAGES = "mm_excluded_packages";
    public static final String KEY_WINDOW_POSITION = "mm_window_position";
    public static final String KEY_WINDOW_SIZE = "mm_window_size";
    public static final String KEY_HISTORY_JSON = "mm_history_json";
    public static final String KEY_LAST_ACTIVE_APP = "mm_last_active_app";
    public static final String KEY_DOCK_X_RATIO = "mm_dock_x_ratio";
    public static final String KEY_DOCK_Y_RATIO = "mm_dock_y_ratio";

    private static final List<String> DEFAULT_SUPPORTED_PACKAGES = Arrays.asList(
            "com.facebook.katana",
            "com.facebook.orca",
            "com.instagram.android",
            "com.whatsapp",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.android.chrome"
    );

    private static final List<String> DEFAULT_EXCLUDED_PACKAGES = Arrays.asList(
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.camera",
            "com.transsion.camera"
    );

    private final SharedPreferences prefs;

    public MiniMaxPreferences(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ensureDefaults();
    }

    private void ensureDefaults() {
        if (!prefs.contains(KEY_SUPPORTED_PACKAGES)) {
            prefs.edit()
                    .putStringSet(KEY_SUPPORTED_PACKAGES, new HashSet<>(DEFAULT_SUPPORTED_PACKAGES))
                    .apply();
        }
        if (!prefs.contains(KEY_EXCLUDED_PACKAGES)) {
            prefs.edit()
                    .putStringSet(KEY_EXCLUDED_PACKAGES, new HashSet<>(DEFAULT_EXCLUDED_PACKAGES))
                    .apply();
        }
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public MiniMaxConfig loadConfig() {
        MiniMaxConfig config = new MiniMaxConfig();
        config.autoMiniEnabled = prefs.getBoolean(KEY_AUTO_MINI_ENABLED, false);
        config.floatingAssistantEnabled = prefs.getBoolean(KEY_FLOATING_ASSISTANT_ENABLED, true);
        config.autoMiniOnHome = prefs.getBoolean(KEY_AUTO_MINI_ON_HOME, true);
        config.autoMiniOnAppSwitch = prefs.getBoolean(KEY_AUTO_MINI_ON_APP_SWITCH, false);
        config.onlySupportedAppsMode = prefs.getBoolean(KEY_ONLY_SUPPORTED_MODE, false);

        Set<String> sup = prefs.getStringSet(KEY_SUPPORTED_PACKAGES, null);
        config.supportedPackages = sup != null ? new HashSet<>(sup) : new HashSet<>(DEFAULT_SUPPORTED_PACKAGES);

        Set<String> exc = prefs.getStringSet(KEY_EXCLUDED_PACKAGES, null);
        config.excludedPackages = exc != null ? new HashSet<>(exc) : new HashSet<>(DEFAULT_EXCLUDED_PACKAGES);

        config.windowPosition = MiniWindowPosition.fromId(
                prefs.getString(KEY_WINDOW_POSITION, MiniWindowPosition.TOP_RIGHT.getId())
        );
        config.windowSize = MiniWindowSize.fromId(
                prefs.getString(KEY_WINDOW_SIZE, MiniWindowSize.BALANCED.getId())
        );
        return config;
    }

    public void setAutoMiniEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_MINI_ENABLED, enabled).apply();
    }

    public void setFloatingAssistantEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FLOATING_ASSISTANT_ENABLED, enabled).apply();
    }

    public void setAutoMiniOnHome(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_MINI_ON_HOME, enabled).apply();
    }

    public void setAutoMiniOnAppSwitch(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_MINI_ON_APP_SWITCH, enabled).apply();
    }

    public void setOnlySupportedAppsMode(boolean onlySupported) {
        prefs.edit().putBoolean(KEY_ONLY_SUPPORTED_MODE, onlySupported).apply();
    }

    public void setSupportedPackages(Set<String> packages) {
        prefs.edit().putStringSet(KEY_SUPPORTED_PACKAGES, new HashSet<>(packages)).apply();
    }

    public void setExcludedPackages(Set<String> packages) {
        prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, new HashSet<>(packages)).apply();
    }

    public void setWindowPosition(MiniWindowPosition position) {
        prefs.edit().putString(KEY_WINDOW_POSITION, position.getId()).apply();
    }

    public void setWindowSize(MiniWindowSize size) {
        prefs.edit().putString(KEY_WINDOW_SIZE, size.getId()).apply();
    }

    public void setLastActiveAppPackage(String pkg) {
        prefs.edit().putString(KEY_LAST_ACTIVE_APP, pkg != null ? pkg : "").apply();
    }

    public String getLastActiveAppPackage() {
        return prefs.getString(KEY_LAST_ACTIVE_APP, "");
    }

    public void saveDockPositionRatio(float xRatio, float yRatio) {
        prefs.edit()
                .putFloat(KEY_DOCK_X_RATIO, Math.max(0f, Math.min(1f, xRatio)))
                .putFloat(KEY_DOCK_Y_RATIO, Math.max(0f, Math.min(1f, yRatio)))
                .apply();
    }

    public float getDockXRatio() {
        return prefs.getFloat(KEY_DOCK_X_RATIO, 0.92f);
    }

    public float getDockYRatio() {
        return prefs.getFloat(KEY_DOCK_Y_RATIO, 0.22f);
    }

    public List<AutomationLogEntry> getHistory() {
        List<AutomationLogEntry> list = new ArrayList<>();
        String json = prefs.getString(KEY_HISTORY_JSON, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new AutomationLogEntry(
                        obj.optLong("ts", 0L),
                        obj.optString("pkg", ""),
                        obj.optString("label", ""),
                        obj.optString("trigger", ""),
                        obj.optString("mech", ""),
                        obj.optBoolean("ok", false),
                        obj.optString("detail", "")
                ));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void addHistoryEntry(AutomationLogEntry entry) {
        List<AutomationLogEntry> current = getHistory();
        current.add(0, entry);
        if (current.size() > MAX_HISTORY_ENTRIES) {
            current = current.subList(0, MAX_HISTORY_ENTRIES);
        }
        JSONArray array = new JSONArray();
        for (AutomationLogEntry item : current) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("ts", item.timestampMs);
                obj.put("pkg", item.packageName);
                obj.put("label", item.appLabel);
                obj.put("trigger", item.triggerLabel);
                obj.put("mech", item.mechanism);
                obj.put("ok", item.success);
                obj.put("detail", item.detail);
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_HISTORY_JSON, array.toString()).apply();
    }

    public void clearHistory() {
        prefs.edit().putString(KEY_HISTORY_JSON, "[]").apply();
    }
}
