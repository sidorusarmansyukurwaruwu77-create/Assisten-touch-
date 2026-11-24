package com.example.touchflow.models;

import android.content.Context;

import androidx.annotation.StringRes;

import com.example.R;

public enum MenuLayoutType {
    CIRCULAR("circular", "Circular Menu", R.string.layout_circular),
    GRID("grid", "Grid Menu", R.string.layout_grid),
    FLOATING_RECTANGLE("rectangle", "Floating Rectangle", R.string.layout_rectangle),
    COMPACT("compact", "Compact Menu", R.string.layout_compact);

    private final String id;
    private final String displayName;
    @StringRes
    private final int titleRes;

    MenuLayoutType(String id, String displayName, @StringRes int titleRes) {
        this.id = id;
        this.displayName = displayName;
        this.titleRes = titleRes;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayName(Context context) {
        return context.getString(titleRes);
    }

    public static MenuLayoutType fromId(String id) {
        if (id == null) return GRID;
        for (MenuLayoutType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return GRID;
    }
}
