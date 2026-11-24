package com.example.touchflow.minimax.models;

import android.content.Context;

import androidx.annotation.StringRes;

import com.example.R;

public enum MiniWindowPosition {
    TOP_LEFT("top_left", "Top Left", R.string.minimax_pos_top_left),
    TOP_RIGHT("top_right", "Top Right", R.string.minimax_pos_top_right),
    CENTER("center", "Center", R.string.minimax_pos_center),
    BOTTOM_LEFT("bottom_left", "Bottom Left", R.string.minimax_pos_bottom_left),
    BOTTOM_RIGHT("bottom_right", "Bottom Right", R.string.minimax_pos_bottom_right);

    private final String id;
    private final String displayName;
    @StringRes
    private final int titleRes;

    MiniWindowPosition(String id, String displayName, @StringRes int titleRes) {
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

    public static MiniWindowPosition fromId(String id) {
        if (id == null) return TOP_RIGHT;
        for (MiniWindowPosition pos : values()) {
            if (pos.id.equals(id)) return pos;
        }
        return TOP_RIGHT;
    }
}
