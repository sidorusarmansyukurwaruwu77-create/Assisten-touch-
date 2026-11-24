package com.example.touchflow.overlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import com.example.touchflow.models.OverlayTheme;
import com.example.touchflow.models.TouchFlowConfig;

public final class OverlayThemeApplier {

    private OverlayThemeApplier() {}

    public static void applyPanelBackground(Context context, View target, TouchFlowConfig config, float cornerDp) {
        OverlayTheme theme = config.overlayTheme;
        float radiusPx = cornerDp * context.getResources().getDisplayMetrics().density;
        float strokePx = 1.2f * context.getResources().getDisplayMetrics().density;

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(radiusPx);

        if (theme == OverlayTheme.GRADIENT_THEME) {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setColors(new int[]{
                    Color.parseColor("#EB231942"),
                    Color.parseColor("#EB151E34"),
                    Color.parseColor("#EB0E2738")
            });
            bg.setStroke(Math.round(strokePx), Color.parseColor("#557C5CFC"));
        } else if (theme == OverlayTheme.CUSTOM_THEME) {
            int base = ColorUtils.setAlphaComponent(
                    ColorUtils.blendARGB(Color.parseColor("#11151E"), config.customThemeColor, 0.24f),
                    236
            );
            bg.setColor(base);
            bg.setStroke(Math.round(strokePx), ColorUtils.setAlphaComponent(config.customThemeColor, 130));
        } else {
            bg.setColor(theme.getBackgroundColor());
            bg.setStroke(Math.round(strokePx), theme.getStrokeColor());
        }

        target.setBackground(bg);
    }

    public static void applyCircleBackground(Context context, View target, TouchFlowConfig config) {
        OverlayTheme theme = config.overlayTheme;
        float strokePx = 1.4f * context.getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        if (theme == OverlayTheme.GRADIENT_THEME) {
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setColors(new int[]{
                    Color.parseColor("#ED241848"),
                    Color.parseColor("#ED112238")
            });
            bg.setStroke(Math.round(strokePx), Color.parseColor("#6628C7FA"));
        } else if (theme == OverlayTheme.CUSTOM_THEME) {
            int base = ColorUtils.setAlphaComponent(
                    ColorUtils.blendARGB(Color.parseColor("#11151E"), config.customThemeColor, 0.28f),
                    240
            );
            bg.setColor(base);
            bg.setStroke(Math.round(strokePx), ColorUtils.setAlphaComponent(config.customThemeColor, 150));
        } else {
            bg.setColor(theme.getBackgroundColor());
            bg.setStroke(Math.round(strokePx), theme.getStrokeColor());
        }
        target.setBackground(bg);
    }

    public static void applyTileBackground(Context context, View target, TouchFlowConfig config, boolean active) {
        OverlayTheme theme = config.overlayTheme;
        float radiusPx = 16f * context.getResources().getDisplayMetrics().density;
        float strokePx = 1f * context.getResources().getDisplayMetrics().density;

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(radiusPx);

        int accent = getAccentColor(config);
        if (active) {
            bg.setColor(ColorUtils.setAlphaComponent(accent, 68));
            bg.setStroke(Math.round(strokePx), ColorUtils.setAlphaComponent(accent, 190));
        } else if (theme == OverlayTheme.LIGHT_MODE) {
            bg.setColor(Color.parseColor("#140F172A"));
            bg.setStroke(Math.round(strokePx), Color.parseColor("#1E0F172A"));
        } else {
            bg.setColor(Color.parseColor("#17FFFFFF"));
            bg.setStroke(Math.round(strokePx), Color.parseColor("#24FFFFFF"));
        }
        target.setBackground(bg);
    }

    public static int getPrimaryTextColor(TouchFlowConfig config) {
        return config.overlayTheme.getTextColor();
    }

    public static int getSecondaryTextColor(TouchFlowConfig config) {
        return config.overlayTheme.getSecondaryTextColor();
    }

    public static int getAccentColor(TouchFlowConfig config) {
        if (config.overlayTheme == OverlayTheme.CUSTOM_THEME) {
            return config.customThemeColor;
        }
        return config.overlayTheme.getAccentColor();
    }

    public static void applyFloatingOrbStyle(Context context, View orbContainer, TouchFlowConfig config) {
        float density = context.getResources().getDisplayMetrics().density;
        int baseColor = config.buttonColor;
        int darkerRim = ColorUtils.blendARGB(baseColor, Color.BLACK, 0.25f);
        int glassTint = ColorUtils.setAlphaComponent(baseColor, 225);

        GradientDrawable orb = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{glassTint, ColorUtils.setAlphaComponent(darkerRim, 235)}
        );
        orb.setShape(GradientDrawable.OVAL);
        orb.setStroke(Math.round(1.6f * density), Color.parseColor("#66FFFFFF"));
        orbContainer.setBackground(orb);

        float elevationDp = (config.shadowIntensity / 100f) * 14f;
        orbContainer.setElevation(elevationDp * density);
    }
}
