package com.example.touchflow.models;

import androidx.annotation.DrawableRes;

import com.example.R;

public enum OrbIconStyle {
    RING("ring", "Orbit Ring", R.drawable.ic_orb_ring),
    DOT("dot", "Classic Dot", R.drawable.ic_orb_dot),
    SPARK("spark", "Pulse Spark", R.drawable.ic_orb_spark),
    COMPASS("compass", "Navigator", R.drawable.ic_orb_compass),
    GRID("grid", "Matrix", R.drawable.ic_orb_grid),
    BOLT("bolt", "Quick Bolt", R.drawable.ic_orb_bolt);

    private final String id;
    private final String displayName;
    @DrawableRes
    private final int drawableRes;

    OrbIconStyle(String id, String displayName, @DrawableRes int drawableRes) {
        this.id = id;
        this.displayName = displayName;
        this.drawableRes = drawableRes;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @DrawableRes
    public int getDrawableRes() {
        return drawableRes;
    }

    public static OrbIconStyle fromId(String id) {
        if (id == null) return RING;
        for (OrbIconStyle style : values()) {
            if (style.id.equals(id)) return style;
        }
        return RING;
    }
}
