package com.example.touchflow.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.example.R;

import java.util.ArrayList;
import java.util.List;

public enum ActionItem {
    HOME("home", R.string.action_home, R.drawable.ic_action_home, true, true),
    BACK("back", R.string.action_back, R.drawable.ic_action_back, true, true),
    RECENTS("recents", R.string.action_recents, R.drawable.ic_action_recents, true, true),
    SCREENSHOT("screenshot", R.string.action_screenshot, R.drawable.ic_action_screenshot, true, true),
    LOCK_SCREEN("lock_screen", R.string.action_lock_screen, R.drawable.ic_action_lock, true, true),
    VOLUME_UP("volume_up", R.string.action_volume_up, R.drawable.ic_action_volume_up, false, true),
    VOLUME_DOWN("volume_down", R.string.action_volume_down, R.drawable.ic_action_volume_down, false, true),
    BRIGHTNESS("brightness", R.string.action_brightness, R.drawable.ic_action_brightness, false, true),
    WIFI("wifi", R.string.action_wifi, R.drawable.ic_action_wifi, false, true),
    BLUETOOTH("bluetooth", R.string.action_bluetooth, R.drawable.ic_action_bluetooth, false, true),
    FLASHLIGHT("flashlight", R.string.action_flashlight, R.drawable.ic_action_flashlight, false, true),
    ROTATION("rotation", R.string.action_rotation, R.drawable.ic_action_rotation, false, true),
    NOTIFICATIONS("notifications", R.string.action_notifications, R.drawable.ic_action_notifications, true, true),
    QUICK_SETTINGS("quick_settings", R.string.action_quick_settings, R.drawable.ic_action_quick_settings, true, true),
    POWER_MENU("power_menu", R.string.action_power_menu, R.drawable.ic_action_power, true, true),
    OPEN_SETTINGS("open_settings", R.string.action_open_settings, R.drawable.ic_action_settings, false, true),

    // Gesture-only / Navigation helpers
    OPEN_MENU("open_menu", R.string.action_open_menu, R.drawable.ic_orb_ring, false, false),
    QUICK_PANEL("quick_panel", R.string.action_quick_panel, R.drawable.ic_action_quick_panel, false, false),
    NONE("none", R.string.action_none, R.drawable.ic_close, false, false);

    private final String id;
    @StringRes
    private final int titleRes;
    @DrawableRes
    private final int iconRes;
    private final boolean requiresAccessibility;
    private final boolean showInMainMenu;

    ActionItem(String id, @StringRes int titleRes, @DrawableRes int iconRes,
               boolean requiresAccessibility, boolean showInMainMenu) {
        this.id = id;
        this.titleRes = titleRes;
        this.iconRes = iconRes;
        this.requiresAccessibility = requiresAccessibility;
        this.showInMainMenu = showInMainMenu;
    }

    public String getId() {
        return id;
    }

    @StringRes
    public int getTitleRes() {
        return titleRes;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    public boolean requiresAccessibility() {
        return requiresAccessibility;
    }

    public boolean isShowInMainMenu() {
        return showInMainMenu;
    }

    public static ActionItem fromId(String id, ActionItem fallback) {
        if (id == null) return fallback;
        for (ActionItem item : values()) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        return fallback;
    }

    public static List<ActionItem> getMainMenuActions() {
        List<ActionItem> list = new ArrayList<>();
        for (ActionItem item : values()) {
            if (item.showInMainMenu) {
                list.add(item);
            }
        }
        return list;
    }

    public static List<ActionItem> getGestureAssignableActions() {
        List<ActionItem> list = new ArrayList<>();
        list.add(OPEN_MENU);
        list.add(QUICK_PANEL);
        list.addAll(getMainMenuActions());
        list.add(NONE);
        return list;
    }
}
