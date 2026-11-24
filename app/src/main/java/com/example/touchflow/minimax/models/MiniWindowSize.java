package com.example.touchflow.minimax.models;

import android.content.Context;

import androidx.annotation.StringRes;

import com.example.R;

public enum MiniWindowSize {
    COMPACT("compact", "Compact (55%)", R.string.minimax_size_compact, 0.55f),
    BALANCED("balanced", "Balanced (70%)", R.string.minimax_size_balanced, 0.70f),
    LARGE("large", "Large (85%)", R.string.minimax_size_large, 0.85f);

    private final String id;
    private final String displayName;
    @StringRes
    private final int titleRes;
    private final float scaleFraction;

    MiniWindowSize(String id, String displayName, @StringRes int titleRes, float scaleFraction) {
        this.id = id;
        this.displayName = displayName;
        this.titleRes = titleRes;
        this.scaleFraction = scaleFraction;
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
