package com.example.touchflow.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.touchflow.models.MenuLayoutType;
import com.example.touchflow.models.OrbIconStyle;
import com.example.touchflow.models.OverlayTheme;
import com.example.touchflow.models.TouchFlowConfig;
import com.example.touchflow.overlay.OverlayThemeApplier;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

public class AppearanceFragment extends Fragment {

    private static final int[] BUTTON_COLOR_SWATCHES = {
            Color.parseColor("#7C5CFC"),
            Color.parseColor("#28C7FA"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#1E293B"),
            Color.parseColor("#FFFFFF")
    };

    private static final int[] ICON_COLOR_SWATCHES = {
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#0B0D12"),
            Color.parseColor("#7C5CFC"),
            Color.parseColor("#28C7FA"),
            Color.parseColor("#FACC15")
    };

    private TouchFlowPreferences preferences;

    private FrameLayout previewOrb;
    private ImageView previewIcon;
    private TextView tvPreviewMeta;
    private TextView tvPreviewThemeMeta;
    private TextView tvSizeValue;
    private TextView tvOpacityValue;
    private TextView tvShadowValue;

    private LinearLayout rowOrbIcons;
    private LinearLayout rowButtonColors;
    private LinearLayout rowIconColors;
    private LinearLayout containerCustomTheme;
    private LinearLayout rowCustomThemeColors;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appearance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = new TouchFlowPreferences(requireContext());
        TouchFlowConfig config = preferences.loadConfig();

        previewOrb = view.findViewById(R.id.style_preview_orb);
        previewIcon = view.findViewById(R.id.style_preview_icon);
        tvPreviewMeta = view.findViewById(R.id.tv_style_preview_meta);
        tvPreviewThemeMeta = view.findViewById(R.id.tv_style_preview_theme_meta);
        tvSizeValue = view.findViewById(R.id.tv_size_value);
        tvOpacityValue = view.findViewById(R.id.tv_opacity_value);
        tvShadowValue = view.findViewById(R.id.tv_shadow_value);

        rowOrbIcons = view.findViewById(R.id.row_orb_icons);
        rowButtonColors = view.findViewById(R.id.row_button_colors);
        rowIconColors = view.findViewById(R.id.row_icon_colors);
        containerCustomTheme = view.findViewById(R.id.container_custom_theme_colors);
        rowCustomThemeColors = view.findViewById(R.id.row_custom_theme_colors);

        setupSliders(view, config);
        setupSpeedChips(view, config);
        setupThemeChips(view, config);
        setupLayoutChips(view, config);

        renderOrbIconPickers(config);
        renderColorSwatches(rowButtonColors, BUTTON_COLOR_SWATCHES, config.buttonColor, color -> {
            preferences.setButtonColor(color);
            TouchFlowConfig updated = preferences.loadConfig();
            renderColorSwatches(rowButtonColors, BUTTON_COLOR_SWATCHES, updated.buttonColor, null);
            updatePreview(updated);
        });
        renderColorSwatches(rowIconColors, ICON_COLOR_SWATCHES, config.iconColor, color -> {
            preferences.setIconColor(color);
            TouchFlowConfig updated = preferences.loadConfig();
            renderColorSwatches(rowIconColors, ICON_COLOR_SWATCHES, updated.iconColor, null);
            updatePreview(updated);
        });
        renderColorSwatches(rowCustomThemeColors, BUTTON_COLOR_SWATCHES, config.customThemeColor, color -> {
            preferences.setCustomThemeColor(color);
            TouchFlowConfig updated = preferences.loadConfig();
            renderColorSwatches(rowCustomThemeColors, BUTTON_COLOR_SWATCHES, updated.customThemeColor, null);
            updatePreview(updated);
        });

        updatePreview(config);
    }

    private void setupSliders(View view, TouchFlowConfig config) {
        Slider sliderSize = view.findViewById(R.id.slider_button_size);
        sliderSize.setValue(config.buttonSizeDp);
        tvSizeValue.setText(config.buttonSizeDp + " dp");
        sliderSize.addOnChangeListener((slider, value, fromUser) -> {
            int val = Math.round(value);
            tvSizeValue.setText(val + " dp");
            if (fromUser) {
                preferences.setButtonSizeDp(val);
                updatePreview(preferences.loadConfig());
            }
        });

        Slider sliderOpacity = view.findViewById(R.id.slider_opacity);
        sliderOpacity.setValue(config.opacityPercent);
        tvOpacityValue.setText(config.opacityPercent + "%");
        sliderOpacity.addOnChangeListener((slider, value, fromUser) -> {
            int val = Math.round(value);
            tvOpacityValue.setText(val + "%");
            if (fromUser) {
                preferences.setOpacityPercent(val);
                updatePreview(preferences.loadConfig());
            }
        });

        Slider sliderShadow = view.findViewById(R.id.slider_shadow);
        sliderShadow.setValue(config.shadowIntensity);
        tvShadowValue.setText(config.shadowIntensity + "%");
        sliderShadow.addOnChangeListener((slider, value, fromUser) -> {
            int val = Math.round(value);
            tvShadowValue.setText(val + "%");
            if (fromUser) {
                preferences.setShadowIntensity(val);
                updatePreview(preferences.loadConfig());
            }
        });
    }

    private void setupSpeedChips(View view, TouchFlowConfig config) {
        ChipGroup group = view.findViewById(R.id.chips_anim_speed);
        if (config.animationSpeed == 0) {
            group.check(R.id.chip_speed_slow);
        } else if (config.animationSpeed == 2) {
            group.check(R.id.chip_speed_fast);
        } else {
            group.check(R.id.chip_speed_normal);
        }
        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_speed_slow) {
                preferences.setAnimationSpeed(0);
            } else if (id == R.id.chip_speed_fast) {
                preferences.setAnimationSpeed(2);
            } else {
                preferences.setAnimationSpeed(1);
            }
        });
    }

    private void setupThemeChips(View view, TouchFlowConfig config) {
        ChipGroup group = view.findViewById(R.id.chips_overlay_theme);
        switch (config.overlayTheme) {
            case LIGHT_MODE:
                group.check(R.id.chip_theme_light);
                break;
            case AMOLED_BLACK:
                group.check(R.id.chip_theme_amoled);
                break;
            case TRANSPARENT_GLASS:
                group.check(R.id.chip_theme_glass);
                break;
            case GRADIENT_THEME:
                group.check(R.id.chip_theme_gradient);
                break;
            case CUSTOM_THEME:
                group.check(R.id.chip_theme_custom);
                break;
            case DARK_MODE:
            default:
                group.check(R.id.chip_theme_dark);
                break;
        }
        containerCustomTheme.setVisibility(
                config.overlayTheme == OverlayTheme.CUSTOM_THEME ? View.VISIBLE : View.GONE
        );

        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            OverlayTheme selected = OverlayTheme.DARK_MODE;
            if (id == R.id.chip_theme_light) selected = OverlayTheme.LIGHT_MODE;
            else if (id == R.id.chip_theme_amoled) selected = OverlayTheme.AMOLED_BLACK;
            else if (id == R.id.chip_theme_glass) selected = OverlayTheme.TRANSPARENT_GLASS;
            else if (id == R.id.chip_theme_gradient) selected = OverlayTheme.GRADIENT_THEME;
            else if (id == R.id.chip_theme_custom) selected = OverlayTheme.CUSTOM_THEME;

            preferences.setOverlayTheme(selected);
            containerCustomTheme.setVisibility(
                    selected == OverlayTheme.CUSTOM_THEME ? View.VISIBLE : View.GONE
            );
            updatePreview(preferences.loadConfig());
        });
    }

    private void setupLayoutChips(View view, TouchFlowConfig config) {
        ChipGroup group = view.findViewById(R.id.chips_menu_layout);
        switch (config.menuLayout) {
            case CIRCULAR:
                group.check(R.id.chip_layout_circular);
                break;
            case FLOATING_RECTANGLE:
                group.check(R.id.chip_layout_rectangle);
                break;
            case COMPACT:
                group.check(R.id.chip_layout_compact);
                break;
            case GRID:
            default:
                group.check(R.id.chip_layout_grid);
                break;
        }
        group.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            MenuLayoutType selected = MenuLayoutType.GRID;
            if (id == R.id.chip_layout_circular) selected = MenuLayoutType.CIRCULAR;
            else if (id == R.id.chip_layout_rectangle) selected = MenuLayoutType.FLOATING_RECTANGLE;
            else if (id == R.id.chip_layout_compact) selected = MenuLayoutType.COMPACT;

            preferences.setMenuLayout(selected);
            updatePreview(preferences.loadConfig());
        });
    }

    private void renderOrbIconPickers(TouchFlowConfig config) {
        rowOrbIcons.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = Math.round(52 * density);
        int marginEnd = Math.round(10 * density);

        for (OrbIconStyle style : OrbIconStyle.values()) {
            FrameLayout item = new FrameLayout(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(marginEnd);
            item.setLayoutParams(lp);
            item.setContentDescription(style.getDisplayName());

            boolean selected = style == config.orbIconStyle;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(14 * density);
            bg.setColor(selected ? Color.parseColor("#337C5CFC") : Color.parseColor("#14FFFFFF"));
            bg.setStroke(
                    Math.round((selected ? 2f : 1f) * density),
                    selected ? Color.parseColor("#7C5CFC") : Color.parseColor("#24FFFFFF")
            );
            item.setBackground(bg);

            ImageView iv = new ImageView(requireContext());
            int iconSz = Math.round(24 * density);
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(iconSz, iconSz, Gravity.CENTER);
            iv.setLayoutParams(ivLp);
            iv.setImageResource(style.getDrawableRes());
            ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(
                    selected ? Color.parseColor("#28C7FA") : Color.WHITE
            ));
            item.addView(iv);

            item.setOnClickListener(v -> {
                preferences.setOrbIconStyle(style);
                TouchFlowConfig updated = preferences.loadConfig();
                renderOrbIconPickers(updated);
                updatePreview(updated);
            });
            rowOrbIcons.addView(item);
        }
    }

    private interface ColorSelectCallback {
        void onSelected(int color);
    }

    private void renderColorSwatches(LinearLayout container, int[] swatches, int selectedColor,
                                     @Nullable ColorSelectCallback callback) {
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = Math.round(48 * density);
        int marginEnd = Math.round(10 * density);

        for (int color : swatches) {
            FrameLayout holder = new FrameLayout(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(marginEnd);
            holder.setLayoutParams(lp);

            boolean isSelected = color == selectedColor;
            GradientDrawable ring = new GradientDrawable();
            ring.setShape(GradientDrawable.OVAL);
            ring.setColor(color);
            ring.setStroke(
                    Math.round((isSelected ? 3f : 1.2f) * density),
                    isSelected ? Color.parseColor("#28C7FA") : Color.parseColor("#44FFFFFF")
            );
            holder.setBackground(ring);

            if (callback != null) {
                holder.setTag(callback);
            }
            holder.setOnClickListener(v -> {
                if (callback != null) {
                    callback.onSelected(color);
                } else {
                    if (container == rowButtonColors) {
                        preferences.setButtonColor(color);
                        TouchFlowConfig updated = preferences.loadConfig();
                        renderColorSwatches(rowButtonColors, BUTTON_COLOR_SWATCHES, updated.buttonColor, null);
                        updatePreview(updated);
                    } else if (container == rowIconColors) {
                        preferences.setIconColor(color);
                        TouchFlowConfig updated = preferences.loadConfig();
                        renderColorSwatches(rowIconColors, ICON_COLOR_SWATCHES, updated.iconColor, null);
                        updatePreview(updated);
                    } else if (container == rowCustomThemeColors) {
                        preferences.setCustomThemeColor(color);
                        TouchFlowConfig updated = preferences.loadConfig();
                        renderColorSwatches(rowCustomThemeColors, BUTTON_COLOR_SWATCHES, updated.customThemeColor, null);
                        updatePreview(updated);
                    }
                }
            });
            container.addView(holder);
        }
    }

    private void updatePreview(TouchFlowConfig config) {
        float density = getResources().getDisplayMetrics().density;
        int orbPx = Math.round(config.buttonSizeDp * density);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) previewOrb.getLayoutParams();
        lp.width = orbPx;
        lp.height = orbPx;
        previewOrb.setLayoutParams(lp);

        int iconPx = Math.round(orbPx * 0.52f);
        FrameLayout.LayoutParams iconLp = (FrameLayout.LayoutParams) previewIcon.getLayoutParams();
        iconLp.width = iconPx;
        iconLp.height = iconPx;
        previewIcon.setLayoutParams(iconLp);

        previewIcon.setImageResource(config.orbIconStyle.getDrawableRes());
        ImageViewCompat.setImageTintList(previewIcon, ColorStateList.valueOf(config.iconColor));
        OverlayThemeApplier.applyFloatingOrbStyle(requireContext(), previewOrb, config);
        previewOrb.setAlpha(config.opacityPercent / 100f);

        tvPreviewMeta.setText(config.buttonSizeDp + " dp • " + config.opacityPercent + "% Opacity");
        tvPreviewThemeMeta.setText(config.overlayTheme.getDisplayName() + " • " + config.menuLayout.getDisplayName());
    }
}
