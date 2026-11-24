package com.example.touchflow.models;

public enum MenuLayoutType {
    CIRCULAR("circular", "Circular Menu"),
    GRID("grid", "Grid Menu"),
    FLOATING_RECTANGLE("rectangle", "Floating Rectangle"),
    COMPACT("compact", "Compact Menu");

    private final String id;
    private final String displayName;

    MenuLayoutType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MenuLayoutType fromId(String id) {
        if (id == null) return GRID;
        for (MenuLayoutType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return GRID;
    }
}
