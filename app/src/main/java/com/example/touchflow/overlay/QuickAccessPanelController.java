package com.example.touchflow.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.core.widget.ImageViewCompat;

import com.example.R;
import com.example.touchflow.models.AppShortcut;
import com.example.touchflow.models.TouchFlowConfig;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.utilities.AppPackageHelper;
import com.example.touchflow.utilities.SystemActionHelper;
import com.example.touchflow.utilities.ViewAnimationHelper;

import java.util.List;

public class QuickAccessPanelController {

    public interface Listener {
        void onPanelClosed();
        void onBackToMenu();
    }

    private final Context context;
    private final WindowManager windowManager;
    private final TouchFlowPreferences preferences;
    private final SystemActionHelper systemActionHelper;
    private final Listener listener;

    private View rootView;
    private LinearLayout cardView;
    private boolean isShowing = false;
    private TouchFlowConfig currentConfig;

    public QuickAccessPanelController(Context context,
                                      WindowManager windowManager,
                                      TouchFlowPreferences preferences,
                                      SystemActionHelper systemActionHelper,
                                      Listener listener) {
        this.context = context;
        this.windowManager = windowManager;
        this.preferences = preferences;
        this.systemActionHelper = systemActionHelper;
        this.listener = listener;
    }

    public boolean isShowing() {
        return isShowing;
    }

    @SuppressLint("InflateParams")
    public void show(TouchFlowConfig config) {
        this.currentConfig = config;
        if (isShowing) return;

        LayoutInflater inflater = LayoutInflater.from(context);
        rootView = inflater.inflate(R.layout.overlay_quick_access_panel, null);
        cardView = rootView.findViewById(R.id.quick_panel_card);

        View scrim = rootView.findViewById(R.id.quick_panel_scrim);
        scrim.setOnClickListener(v -> dismiss(true));

        ImageButton btnBack = rootView.findViewById(R.id.btn_qp_back);
        btnBack.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            dismiss(false);
            if (listener != null) listener.onBackToMenu();
        });

        ImageButton btnClose = rootView.findViewById(R.id.btn_qp_close);
        btnClose.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            dismiss(true);
        });

        applyThemeAndBindData(config);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        try {
            windowManager.addView(rootView, params);
            isShowing = true;
            ViewAnimationHelper.animateMenuExpand(cardView, config.getAnimationDurationMs());
        } catch (Exception ignored) {
            isShowing = false;
            rootView = null;
        }
    }

    private void applyThemeAndBindData(TouchFlowConfig config) {
        int primaryText = OverlayThemeApplier.getPrimaryTextColor(config);
        int secondaryText = OverlayThemeApplier.getSecondaryTextColor(config);
        int accent = OverlayThemeApplier.getAccentColor(config);

        OverlayThemeApplier.applyPanelBackground(context, cardView, config, 26f);

        TextClock tcTime = rootView.findViewById(R.id.tc_qp_time);
        TextClock tcDate = rootView.findViewById(R.id.tc_qp_date);
        tcTime.setTextColor(primaryText);
        tcDate.setTextColor(secondaryText);

        ImageButton btnBack = rootView.findViewById(R.id.btn_qp_back);
        ImageButton btnClose = rootView.findViewById(R.id.btn_qp_close);
        ImageViewCompat.setImageTintList(btnBack, ColorStateList.valueOf(primaryText));
        ImageViewCompat.setImageTintList(btnClose, ColorStateList.valueOf(primaryText));

        // Status pills
        bindStatusPills(config, primaryText, accent);

        // Volume slider
        bindVolumeControl(config, primaryText, secondaryText, accent);

        // Brightness slider
        bindBrightnessControl(config, primaryText, secondaryText, accent);

        // Flashlight toggle
        bindFlashlightButton(config, primaryText, accent);

        // Favorite Apps
        bindFavoriteApps(config, primaryText, secondaryText);
    }

    private void bindStatusPills(TouchFlowConfig config, int primaryText, int accent) {
        LinearLayout batteryPill = rootView.findViewById(R.id.pill_qp_battery);
        ImageView ivBattery = rootView.findViewById(R.id.iv_qp_battery);
        TextView tvBattery = rootView.findViewById(R.id.tv_qp_battery);
        SystemActionHelper.BatteryInfo battery = systemActionHelper.getBatteryInfo();
        OverlayThemeApplier.applyTileBackground(context, batteryPill, config, battery.charging);
        ImageViewCompat.setImageTintList(ivBattery, ColorStateList.valueOf(battery.charging ? accent : primaryText));
        tvBattery.setTextColor(primaryText);
        tvBattery.setText(battery.charging ? (battery.percent + "% ⚡") : (battery.percent + "%"));

        LinearLayout wifiPill = rootView.findViewById(R.id.pill_qp_wifi);
        ImageView ivWifi = rootView.findViewById(R.id.iv_qp_wifi);
        TextView tvWifi = rootView.findViewById(R.id.tv_qp_wifi);
        String wifiStatus = systemActionHelper.getWifiStatusLabel();
        boolean wifiActive = "Connected".equals(wifiStatus) || "On".equals(wifiStatus);
        OverlayThemeApplier.applyTileBackground(context, wifiPill, config, wifiActive);
        ImageViewCompat.setImageTintList(ivWifi, ColorStateList.valueOf(wifiActive ? accent : primaryText));
        tvWifi.setTextColor(primaryText);
        tvWifi.setText("Wi-Fi: " + wifiStatus);
        wifiPill.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            dismiss(true);
            systemActionHelper.openWifiSettings();
        });

        LinearLayout btPill = rootView.findViewById(R.id.pill_qp_bluetooth);
        ImageView ivBt = rootView.findViewById(R.id.iv_qp_bluetooth);
        TextView tvBt = rootView.findViewById(R.id.tv_qp_bluetooth);
        String btStatus = systemActionHelper.getBluetoothStatusLabel();
        boolean btActive = "On".equals(btStatus);
        OverlayThemeApplier.applyTileBackground(context, btPill, config, btActive);
        ImageViewCompat.setImageTintList(ivBt, ColorStateList.valueOf(btActive ? accent : primaryText));
        tvBt.setTextColor(primaryText);
        tvBt.setText("BT: " + btStatus);
        btPill.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            dismiss(true);
            systemActionHelper.openBluetoothSettings();
        });
    }

    private void bindVolumeControl(TouchFlowConfig config, int primaryText, int secondaryText, int accent) {
        ImageView ivVol = rootView.findViewById(R.id.iv_qp_volume);
        TextView tvLabel = rootView.findViewById(R.id.tv_qp_volume_label);
        TextView tvVal = rootView.findViewById(R.id.tv_qp_volume_val);
        SeekBar seekVol = rootView.findViewById(R.id.seek_qp_volume);

        ImageViewCompat.setImageTintList(ivVol, ColorStateList.valueOf(accent));
        tvLabel.setTextColor(primaryText);
        tvVal.setTextColor(secondaryText);

        int maxVol = systemActionHelper.getMusicMaxVolume();
        int curVol = systemActionHelper.getMusicVolume();
        seekVol.setMax(maxVol);
        seekVol.setProgress(curVol);
        seekVol.setProgressTintList(ColorStateList.valueOf(accent));
        seekVol.setThumbTintList(ColorStateList.valueOf(accent));
        tvVal.setText(Math.round((curVol * 100f) / maxVol) + "%");

        seekVol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVal.setText(Math.round((progress * 100f) / maxVol) + "%");
                if (fromUser) {
                    systemActionHelper.setMusicVolume(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                systemActionHelper.performHaptic(config.hapticEnabled);
            }
        });
    }

    private void bindBrightnessControl(TouchFlowConfig config, int primaryText, int secondaryText, int accent) {
        ImageView ivBright = rootView.findViewById(R.id.iv_qp_brightness);
        TextView tvLabel = rootView.findViewById(R.id.tv_qp_brightness_label);
        TextView tvVal = rootView.findViewById(R.id.tv_qp_brightness_val);
        SeekBar seekBright = rootView.findViewById(R.id.seek_qp_brightness);

        ImageViewCompat.setImageTintList(ivBright, ColorStateList.valueOf(accent));
        tvLabel.setTextColor(primaryText);
        tvVal.setTextColor(secondaryText);

        int curBrightness = systemActionHelper.getScreenBrightness();
        int progress = Math.max(0, Math.min(250, curBrightness - 5));
        seekBright.setMax(250);
        seekBright.setProgress(progress);
        seekBright.setProgressTintList(ColorStateList.valueOf(accent));
        seekBright.setThumbTintList(ColorStateList.valueOf(accent));
        tvVal.setText(Math.round((progress * 100f) / 250f) + "%");

        seekBright.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                tvVal.setText(Math.round((p * 100f) / 250f) + "%");
                if (fromUser && com.example.touchflow.utilities.PermissionHelper.canWriteSettings(context)) {
                    systemActionHelper.setScreenBrightness(p + 5);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                systemActionHelper.performHaptic(config.hapticEnabled);
                if (!com.example.touchflow.utilities.PermissionHelper.canWriteSettings(context)) {
                    dismiss(true);
                    systemActionHelper.setScreenBrightness(seekBar.getProgress() + 5);
                }
            }
        });
    }

    private void bindFlashlightButton(TouchFlowConfig config, int primaryText, int accent) {
        LinearLayout btnFlash = rootView.findViewById(R.id.btn_qp_flashlight);
        ImageView ivFlash = rootView.findViewById(R.id.iv_qp_flashlight);
        TextView tvFlash = rootView.findViewById(R.id.tv_qp_flashlight);
        TextView tvState = rootView.findViewById(R.id.tv_qp_flashlight_state);

        Runnable refreshUi = () -> {
            if (!isShowing || rootView == null) return;
            boolean on = systemActionHelper.isTorchOn();
            OverlayThemeApplier.applyTileBackground(context, btnFlash, config, on);
            ImageViewCompat.setImageTintList(ivFlash, ColorStateList.valueOf(on ? accent : primaryText));
            tvFlash.setTextColor(primaryText);
            tvState.setText(on ? "ON" : "OFF");
            tvState.setTextColor(on ? accent : OverlayThemeApplier.getSecondaryTextColor(config));
        };

        refreshUi.run();
        btnFlash.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            systemActionHelper.toggleFlashlight();
            btnFlash.postDelayed(refreshUi, 80L);
        });
    }

    private void bindFavoriteApps(TouchFlowConfig config, int primaryText, int secondaryText) {
        TextView tvTitle = rootView.findViewById(R.id.tv_qp_apps_title);
        GridLayout grid = rootView.findViewById(R.id.grid_qp_favorites);
        TextView tvEmpty = rootView.findViewById(R.id.tv_qp_no_favorites);

        tvTitle.setTextColor(secondaryText);
        tvEmpty.setTextColor(secondaryText);

        List<AppShortcut> shortcuts = preferences.getShortcuts();
        grid.removeAllViews();

        if (shortcuts.isEmpty()) {
            grid.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        grid.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        float density = context.getResources().getDisplayMetrics().density;
        int margin = Math.round(4 * density);
        int iconSize = Math.round(30 * density);

        for (AppShortcut app : shortcuts) {
            LinearLayout tile = new LinearLayout(context);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            int pad = Math.round(8 * density);
            tile.setPadding(pad, pad, pad, pad);
            OverlayThemeApplier.applyTileBackground(context, tile, config, false);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(margin, margin, margin, margin);
            tile.setLayoutParams(lp);

            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
            Drawable icon = AppPackageHelper.loadAppIcon(context, app.getPackageName());
            if (icon != null) {
                iv.setImageDrawable(icon);
            } else {
                iv.setImageResource(R.drawable.ic_nav_apps);
            }

            TextView tv = new TextView(context);
            tv.setText(app.getLabel());
            tv.setTextColor(primaryText);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            tv.setMaxLines(1);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tvLp.topMargin = Math.round(4 * density);
            tv.setLayoutParams(tvLp);

            tile.addView(iv);
            tile.addView(tv);
            tile.setOnClickListener(v -> {
                systemActionHelper.performHaptic(config.hapticEnabled);
                dismiss(true);
                AppPackageHelper.launchApp(context, app.getPackageName());
            });
            grid.addView(tile);
        }
    }

    public void dismiss(boolean notifyListener) {
        if (!isShowing || rootView == null) {
            isShowing = false;
            return;
        }
        final View viewToRemove = rootView;
        final long duration = currentConfig != null ? currentConfig.getAnimationDurationMs() : 200L;
        isShowing = false;
        rootView = null;
        cardView = null;

        ViewAnimationHelper.animateMenuCollapse(viewToRemove.findViewById(R.id.quick_panel_card), duration, () -> {
            try {
                windowManager.removeViewImmediate(viewToRemove);
            } catch (Exception ignored) {
            }
            if (notifyListener && listener != null) {
                listener.onPanelClosed();
            }
        });
    }

    public void removeImmediate() {
        if (rootView != null) {
            try {
                windowManager.removeViewImmediate(rootView);
            } catch (Exception ignored) {
            }
        }
        isShowing = false;
        rootView = null;
        cardView = null;
    }
}
