package com.example.touchflow.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import com.example.touchflow.models.ActionItem;
import com.example.touchflow.models.AppShortcut;
import com.example.touchflow.models.MenuLayoutType;
import com.example.touchflow.models.OrbIconStyle;
import com.example.touchflow.models.OverlayTheme;
import com.example.touchflow.models.TouchFlowConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TouchFlowPreferences {
    public static final String PREFS_NAME = "touchflow_prefs";
    public static final int MAX_SHORTCUTS = 8;

    public static final String KEY_SERVICE_ENABLED = "service_enabled";
    public static final String KEY_BUTTON_SIZE_DP = "button_size_dp";
    public static final String KEY_OPACITY_PERCENT = "opacity_percent";
    public static final String KEY_BUTTON_COLOR = "button_color";
    public static final String KEY_ICON_COLOR = "icon_color";
    public static final String KEY_SHADOW_INTENSITY = "shadow_intensity";
    public static final String KEY_ANIMATION_SPEED = "animation_speed";
    public static final String KEY_ORB_ICON_STYLE = "orb_icon_style";
    public static final String KEY_OVERLAY_THEME = "overlay_theme";
    public static final String KEY_MENU_LAYOUT = "menu_layout";
    public static final String KEY_CUSTOM_THEME_COLOR = "custom_theme_color";

    public static final String KEY_SINGLE_TAP_ACTION = "single_tap_action";
    public static final String KEY_DOUBLE_TAP_ACTION = "double_tap_action";
    public static final String KEY_LONG_PRESS_ACTION = "long_press_action";
    public static final String KEY_SWIPE_ACTION = "swipe_action";

    public static final String KEY_HAPTIC_ENABLED = "haptic_enabled";
    public static final String KEY_EDGE_SNAP_ENABLED = "edge_snap_enabled";
    public static final String KEY_IDLE_DIM_ENABLED = "idle_dim_enabled";

    public static final String KEY_POS_X_RATIO = "pos_x_ratio";
    public static final String KEY_POS_Y_RATIO = "pos_y_ratio";
    public static final String KEY_SHORTCUTS_JSON = "shortcuts_json";

    private final SharedPreferences prefs;

    public TouchFlowPreferences(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public SharedPreferences getRawPreferences() {
        return prefs;
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public TouchFlowConfig loadConfig() {
        TouchFlowConfig config = new TouchFlowConfig();
        config.serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false);
        config.buttonSizeDp = clamp(prefs.getInt(KEY_BUTTON_SIZE_DP, 56), 35, 80);
        config.opacityPercent = clamp(prefs.getInt(KEY_OPACITY_PERCENT, 92), 10, 100);
        config.buttonColor = prefs.getInt(KEY_BUTTON_COLOR, Color.parseColor("#7C5CFC"));
        config.iconColor = prefs.getInt(KEY_ICON_COLOR, Color.parseColor("#FFFFFF"));
        config.shadowIntensity = clamp(prefs.getInt(KEY_SHADOW_INTENSITY, 70), 0, 100);
        config.animationSpeed = clamp(prefs.getInt(KEY_ANIMATION_SPEED, 1), 0, 2);
        config.orbIconStyle = OrbIconStyle.fromId(prefs.getString(KEY_ORB_ICON_STYLE, OrbIconStyle.RING.getId()));
        config.overlayTheme = OverlayTheme.fromId(prefs.getString(KEY_OVERLAY_THEME, OverlayTheme.DARK_MODE.getId()));
        config.menuLayout = MenuLayoutType.fromId(prefs.getString(KEY_MENU_LAYOUT, MenuLayoutType.GRID.getId()));
        config.customThemeColor = prefs.getInt(KEY_CUSTOM_THEME_COLOR, Color.parseColor("#7C5CFC"));

        config.singleTapAction = ActionItem.fromId(
                prefs.getString(KEY_SINGLE_TAP_ACTION, ActionItem.OPEN_MENU.getId()),
                ActionItem.OPEN_MENU
        );
        config.doubleTapAction = ActionItem.fromId(
                prefs.getString(KEY_DOUBLE_TAP_ACTION, ActionItem.QUICK_PANEL.getId()),
                ActionItem.QUICK_PANEL
        );
        config.longPressAction = ActionItem.fromId(
                prefs.getString(KEY_LONG_PRESS_ACTION, ActionItem.SCREENSHOT.getId()),
                ActionItem.SCREENSHOT
        );
        config.swipeAction = ActionItem.fromId(
                prefs.getString(KEY_SWIPE_ACTION, ActionItem.HOME.getId()),
                ActionItem.HOME
        );

        config.hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true);
        config.edgeSnapEnabled = prefs.getBoolean(KEY_EDGE_SNAP_ENABLED, true);
        config.idleDimEnabled = prefs.getBoolean(KEY_IDLE_DIM_ENABLED, true);
        return config;
    }

    public void setServiceEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply();
    }

    public boolean isServiceEnabled() {
        return prefs.getBoolean(KEY_SERVICE_ENABLED, false);
    }

    public void setButtonSizeDp(int sizeDp) {
        prefs.edit().putInt(KEY_BUTTON_SIZE_DP, clamp(sizeDp, 35, 80)).apply();
    }

    public void setOpacityPercent(int opacityPercent) {
        prefs.edit().putInt(KEY_OPACITY_PERCENT, clamp(opacityPercent, 10, 100)).apply();
    }

    public void setButtonColor(int color) {
        prefs.edit().putInt(KEY_BUTTON_COLOR, color).apply();
    }

    public void setIconColor(int color) {
        prefs.edit().putInt(KEY_ICON_COLOR, color).apply();
    }

    public void setShadowIntensity(int intensity) {
        prefs.edit().putInt(KEY_SHADOW_INTENSITY, clamp(intensity, 0, 100)).apply();
    }

    public void setAnimationSpeed(int speed) {
        prefs.edit().putInt(KEY_ANIMATION_SPEED, clamp(speed, 0, 2)).apply();
    }

    public void setOrbIconStyle(OrbIconStyle style) {
        prefs.edit().putString(KEY_ORB_ICON_STYLE, style.getId()).apply();
    }

    public void setOverlayTheme(OverlayTheme theme) {
        prefs.edit().putString(KEY_OVERLAY_THEME, theme.getId()).apply();
    }

    public void setCustomThemeColor(int color) {
        prefs.edit().putInt(KEY_CUSTOM_THEME_COLOR, color).apply();
    }

    public void setMenuLayout(MenuLayoutType layout) {
        prefs.edit().putString(KEY_MENU_LAYOUT, layout.getId()).apply();
    }

    public void setSingleTapAction(ActionItem action) {
        prefs.edit().putString(KEY_SINGLE_TAP_ACTION, action.getId()).apply();
    }

    public void setDoubleTapAction(ActionItem action) {
        prefs.edit().putString(KEY_DOUBLE_TAP_ACTION, action.getId()).apply();
    }

    public void setLongPressAction(ActionItem action) {
        prefs.edit().putString(KEY_LONG_PRESS_ACTION, action.getId()).apply();
    }

    public void setSwipeAction(ActionItem action) {
        prefs.edit().putString(KEY_SWIPE_ACTION, action.getId()).apply();
    }

    public void setHapticEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply();
    }

    public void setEdgeSnapEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_EDGE_SNAP_ENABLED, enabled).apply();
    }

    public void setIdleDimEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_IDLE_DIM_ENABLED, enabled).apply();
    }

    public void saveButtonPositionRatio(float xRatio, float yRatio) {
        prefs.edit()
                .putFloat(KEY_POS_X_RATIO, Math.max(0f, Math.min(1f, xRatio)))
                .putFloat(KEY_POS_Y_RATIO, Math.max(0f, Math.min(1f, yRatio)))
                .apply();
    }

    public float getButtonXRatio() {
        return prefs.getFloat(KEY_POS_X_RATIO, 1.0f);
    }

    public float getButtonYRatio() {
        return prefs.getFloat(KEY_POS_Y_RATIO, 0.42f);
    }

    public List<AppShortcut> getShortcuts() {
        List<AppShortcut> list = new ArrayList<>();
        String json = prefs.getString(KEY_SHORTCUTS_JSON, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length() && i < MAX_SHORTCUTS; i++) {
                JSONObject obj = array.getJSONObject(i);
                String pkg = obj.optString("pkg", "");
                String label = obj.optString("label", "");
                if (!pkg.isEmpty()) {
                    list.add(new AppShortcut(pkg, label));
                }
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void saveShortcuts(List<AppShortcut> shortcuts) {
        JSONArray array = new JSONArray();
        int limit = Math.min(shortcuts.size(), MAX_SHORTCUTS);
        for (int i = 0; i < limit; i++) {
            AppShortcut item = shortcuts.get(i);
            JSONObject obj = new JSONObject();
            try {
                obj.put("pkg", item.getPackageName());
                obj.put("label", item.getLabel());
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_SHORTCUTS_JSON, array.toString()).apply();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
