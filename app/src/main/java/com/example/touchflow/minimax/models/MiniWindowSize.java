package com.example.touchflow.minimax.models;

public enum MiniWindowSize {
    COMPACT("compact", "Compact (55%)", 0.55f),
    BALANCED("balanced", "Balanced (70%)", 0.70f),
    LARGE("large", "Large (85%)", 0.85f);

    private final String id;
    private final String displayName;
    private final float scaleFraction;

    MiniWindowSize(String id, String displayName, float scaleFraction) {
        this.id = id;
        this.displayName = displayName;
        this.scaleFraction = scaleFraction;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getScaleFraction() {
        return scaleFraction;
    }

    public static MiniWindowSize fromId(String id) {
        if (id == null) return BALANCED;
        for (MiniWindowSize s : values()) {
            if (s.id.equals(id)) return s;
        }
        return BALANCED;
    }
}
