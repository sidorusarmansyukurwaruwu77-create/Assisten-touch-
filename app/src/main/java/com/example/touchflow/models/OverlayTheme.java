package com.example.touchflow.models;

import android.graphics.Color;

public enum OverlayTheme {
    DARK_MODE("dark", "Dark Mode", Color.parseColor("#E6151922"), Color.parseColor("#FFFFFF"), Color.parseColor("#9AA3B2"), Color.parseColor("#7C5CFC"), Color.parseColor("#2EFFFFFF")),
    LIGHT_MODE("light", "Light Mode", Color.parseColor("#F5F5F7FB"), Color.parseColor("#0F172A"), Color.parseColor("#475569"), Color.parseColor("#7C5CFC"), Color.parseColor("#220F172A")),
    AMOLED_BLACK("amoled", "AMOLED Black", Color.parseColor("#F5000000"), Color.parseColor("#FFFFFF"), Color.parseColor("#8B949E"), Color.parseColor("#28C7FA"), Color.parseColor("#2AFFFFFF")),
    TRANSPARENT_GLASS("glass", "Transparent Glass", Color.parseColor("#A6151922"), Color.parseColor("#FFFFFF"), Color.parseColor("#CBD5E1"), Color.parseColor("#28C7FA"), Color.parseColor("#44FFFFFF")),
    GRADIENT_THEME("gradient", "Gradient Theme", Color.parseColor("#E61D1736"), Color.parseColor("#FFFFFF"), Color.parseColor("#C4B5FD"), Color.parseColor("#28C7FA"), Color.parseColor("#4D7C5CFC")),
    CUSTOM_THEME("custom", "Custom Theme", Color.parseColor("#E6171C28"), Color.parseColor("#FFFFFF"), Color.parseColor("#A5B4FC"), Color.parseColor("#7C5CFC"), Color.parseColor("#4DFFFFFF"));

    private final String id;
    private final String displayName;
    private final int backgroundColor;
    private final int textColor;
    private final int secondaryTextColor;
    private final int accentColor;
    private final int strokeColor;

    OverlayTheme(String id, String displayName, int backgroundColor, int textColor,
                 int secondaryTextColor, int accentColor, int strokeColor) {
        this.id = id;
        this.displayName = displayName;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.secondaryTextColor = secondaryTextColor;
        this.accentColor = accentColor;
        this.strokeColor = strokeColor;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getSecondaryTextColor() {
        return secondaryTextColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public static OverlayTheme fromId(String id) {
        if (id == null) return DARK_MODE;
        for (OverlayTheme t : values()) {
            if (t.id.equals(id)) return t;
        }
        return DARK_MODE;
    }
}
