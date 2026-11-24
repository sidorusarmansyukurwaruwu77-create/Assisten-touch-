package com.example.touchflow.minimax.models;

public enum MiniWindowPosition {
    TOP_LEFT("top_left", "Top Left"),
    TOP_RIGHT("top_right", "Top Right"),
    CENTER("center", "Center"),
    BOTTOM_LEFT("bottom_left", "Bottom Left"),
    BOTTOM_RIGHT("bottom_right", "Bottom Right");

    private final String id;
    private final String displayName;

    MiniWindowPosition(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MiniWindowPosition fromId(String id) {
        if (id == null) return TOP_RIGHT;
        for (MiniWindowPosition pos : values()) {
            if (pos.id.equals(id)) return pos;
        }
        return TOP_RIGHT;
    }
}
