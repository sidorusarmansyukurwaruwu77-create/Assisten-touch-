package com.example.touchflow.minimax.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.ImageViewCompat;

import com.example.R;
import com.example.touchflow.activities.MainActivity;
import com.example.touchflow.minimax.engine.AppCapabilityInspector;
import com.example.touchflow.minimax.engine.FloatingWindowAdapter;
import com.example.touchflow.minimax.engine.MiniMaxAutomationEngine;
import com.example.touchflow.minimax.preferences.MiniMaxPreferences;
import com.example.touchflow.models.TouchFlowConfig;
import com.example.touchflow.overlay.OverlayThemeApplier;
import com.example.touchflow.utilities.AppPackageHelper;
import com.example.touchflow.utilities.LocaleHelper;
import com.example.touchflow.utilities.SystemActionHelper;
import com.example.touchflow.utilities.ViewAnimationHelper;

public class MiniMaxFloatingController {

    private final Context context;
    private final WindowManager windowManager;
    private final MiniMaxPreferences miniMaxPreferences;
    private final SystemActionHelper systemActionHelper;

    private View rootView;
    private View expandedContainer;
    private View minimizedContainer;
    private WindowManager.LayoutParams layoutParams;
    private boolean isAttached = false;
    private boolean isDockMinimized = false;
    private String trackedPackage = "";
    private String statusSubtitle = "";
    private int screenWidth;
    private int screenHeight;
    private int touchSlop;
    private TouchFlowConfig currentTouchFlowConfig;

    public MiniMaxFloatingController(Context context,
                                     WindowManager windowManager,
                                     MiniMaxPreferences miniMaxPreferences,
                                     SystemActionHelper systemActionHelper) {
        this.context = context;
        this.windowManager = windowManager;
        this.miniMaxPreferences = miniMaxPreferences;
        this.systemActionHelper = systemActionHelper;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        updateScreenBounds();
    }

    public void updateScreenBounds() {
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        screenWidth = Math.max(320, size.x);
        screenHeight = Math.max(480, size.y);
    }

    @SuppressLint("InflateParams")
    public void showOrUpdate(TouchFlowConfig touchFlowConfig, String packageName, String appLabel,
                             String subtitle, boolean startMinimized) {
        this.currentTouchFlowConfig = touchFlowConfig;
        this.isDockMinimized = startMinimized;
        if (packageName != null && !packageName.isEmpty()) {
            this.trackedPackage = packageName;
        } else if (startMinimized && (packageName == null || packageName.isEmpty())) {
            this.trackedPackage = "";
        } else {
            this.trackedPackage = miniMaxPreferences.getLastActiveAppPackage();
        }
        Context uiCtx = LocaleHelper.wrap(context);
        if (subtitle != null && !subtitle.isEmpty()) {
            this.statusSubtitle = subtitle;
        } else {
            this.statusSubtitle = uiCtx.getString(R.string.minimax_dock_tap_mini);
        }

        if (!isAttached) {
            LayoutInflater inflater = LayoutInflater.from(uiCtx);
            rootView = inflater.inflate(R.layout.overlay_minimax_dock, null);
            expandedContainer = rootView.findViewById(R.id.layout_minimax_dock_expanded);
            minimizedContainer = rootView.findViewById(R.id.layout_minimax_dock_minimized);

            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = Math.round(miniMaxPreferences.getDockXRatio() * Math.max(0, screenWidth - 480));
            layoutParams.y = Math.round(miniMaxPreferences.getDockYRatio() * Math.max(0, screenHeight - 220));

            setupInteractions();
            bindContent(appLabel);
            applyDockMode();
            applyTheme(touchFlowConfig);

            try {
                windowManager.addView(rootView, layoutParams);
                isAttached = true;
                ViewAnimationHelper.animateMenuExpand(rootView, touchFlowConfig.getAnimationDurationMs());
            } catch (Exception ignored) {
                isAttached = false;
                rootView = null;
            }
        } else {
            bindContent(appLabel);
            applyDockMode();
            applyTheme(touchFlowConfig);
        }
    }

    private void applyDockMode() {
        if (expandedContainer == null || minimizedContainer == null) return;
        expandedContainer.setVisibility(isDockMinimized ? View.GONE : View.VISIBLE);
        minimizedContainer.setVisibility(isDockMinimized ? View.VISIBLE : View.GONE);
        if (isAttached && rootView != null && layoutParams != null) {
            try {
                windowManager.updateViewLayout(rootView, layoutParams);
            } catch (Exception ignored) {
            }
        }
    }

    public void applyTheme(TouchFlowConfig touchFlowConfig) {
        this.currentTouchFlowConfig = touchFlowConfig;
        if (!isAttached || rootView == null) return;
        Context uiCtx = LocaleHelper.wrap(context);
        OverlayThemeApplier.applyPanelBackground(uiCtx, rootView, touchFlowConfig, isDockMinimized ? 19f : 22f);

        int primaryText = OverlayThemeApplier.getPrimaryTextColor(touchFlowConfig);
        int accent = OverlayThemeApplier.getAccentColor(touchFlowConfig);

        TextView title = rootView.findViewById(R.id.tv_minimax_dock_title);
        TextView sub = rootView.findViewById(R.id.tv_minimax_dock_sub);
        ImageButton btnMini = rootView.findViewById(R.id.btn_minimax_dock_open_full);
        ImageButton btnMin = rootView.findViewById(R.id.btn_minimax_dock_minimize);
        ImageButton btnClose = rootView.findViewById(R.id.btn_minimax_dock_close);
        TextView miniLabel = rootView.findViewById(R.id.tv_minimax_dock_mini_label);
        ImageView ivExpand = rootView.findViewById(R.id.iv_minimax_dock_expand);

        title.setTextColor(primaryText);
        sub.setTextColor(accent);
        ImageViewCompat.setImageTintList(btnMini, ColorStateList.valueOf(accent));
        if (btnMin != null) {
            OverlayThemeApplier.applyTileBackground(uiCtx, btnMin, touchFlowConfig, false);
            ImageViewCompat.setImageTintList(btnMin, ColorStateList.valueOf(primaryText));
        }
        ImageViewCompat.setImageTintList(btnClose, ColorStateList.valueOf(primaryText));
        if (miniLabel != null) {
            miniLabel.setTextColor(primaryText);
        }
        if (ivExpand != null) {
            ImageViewCompat.setImageTintList(ivExpand, ColorStateList.valueOf(accent));
        }
    }

    private void bindContent(String explicitLabel) {
        if (rootView == null) return;
        Context uiCtx = LocaleHelper.wrap(context);
        ImageView ivApp = rootView.findViewById(R.id.iv_minimax_dock_app);
        TextView tvTitle = rootView.findViewById(R.id.tv_minimax_dock_title);
        TextView tvSub = rootView.findViewById(R.id.tv_minimax_dock_sub);
        ImageView ivMiniApp = rootView.findViewById(R.id.iv_minimax_dock_mini_app);
        TextView tvMiniLabel = rootView.findViewById(R.id.tv_minimax_dock_mini_label);

        if (trackedPackage != null && !trackedPackage.isEmpty()) {
            String label = (explicitLabel != null && !explicitLabel.isEmpty())
                    ? explicitLabel
                    : AppCapabilityInspector.getAppLabel(uiCtx, trackedPackage);
            tvTitle.setText(label);
            tvSub.setText(statusSubtitle);
            if (tvMiniLabel != null) {
                tvMiniLabel.setText(label);
            }
            Drawable icon = AppPackageHelper.loadAppIcon(uiCtx, trackedPackage);
            if (icon != null) {
                ivApp.setImageDrawable(icon);
                if (ivMiniApp != null) ivMiniApp.setImageDrawable(icon);
            } else {
                ivApp.setImageResource(R.drawable.ic_minimax);
                if (ivMiniApp != null) ivMiniApp.setImageResource(R.drawable.ic_minimax);
            }
        } else {
            tvTitle.setText(R.string.minimax_title);
            tvSub.setText(uiCtx.getString(R.string.minimax_dock_open_app_hint));
            ivApp.setImageResource(R.drawable.ic_minimax);
            if (ivMiniApp != null) ivMiniApp.setImageResource(R.drawable.ic_minimax);
            if (tvMiniLabel != null) {
                tvMiniLabel.setText(uiCtx.getString(R.string.minimax_dock_minimized_chip));
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupInteractions() {
        View btnLaunch = rootView.findViewById(R.id.btn_minimax_dock_launch);
        ImageButton btnOpenMini = rootView.findViewById(R.id.btn_minimax_dock_open_full);
        ImageButton btnMinDock = rootView.findViewById(R.id.btn_minimax_dock_minimize);
        ImageButton btnClose = rootView.findViewById(R.id.btn_minimax_dock_close);

        View.OnClickListener launchClick = v -> {
            if (currentTouchFlowConfig != null) {
                systemActionHelper.performHaptic(currentTouchFlowConfig.hapticEnabled);
            }
            Context uiCtx = LocaleHelper.wrap(context);
            if (trackedPackage == null || trackedPackage.isEmpty()) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_OPEN_MINIMAX, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
                return;
            }
            FloatingWindowAdapter.LaunchResult result = MiniMaxAutomationEngine
                    .getInstance(context)
                    .triggerManualMiniLaunch(trackedPackage);
            if (!result.launchedInWindow) {
                Toast.makeText(
                        uiCtx,
                        AppCapabilityInspector.getAppLabel(uiCtx, trackedPackage),
                        Toast.LENGTH_SHORT
                ).show();
                MiniMaxAutomationEngine.getInstance(context).openLastAppNormally(trackedPackage);
            }
        };

        btnLaunch.setOnClickListener(launchClick);
        btnOpenMini.setOnClickListener(launchClick);

        if (btnMinDock != null) {
            btnMinDock.setOnClickListener(v -> {
                if (currentTouchFlowConfig != null) {
                    systemActionHelper.performHaptic(currentTouchFlowConfig.hapticEnabled);
                }
                isDockMinimized = true;
                applyDockMode();
                if (currentTouchFlowConfig != null) {
                    applyTheme(currentTouchFlowConfig);
                }
            });
        }

        if (minimizedContainer != null) {
            minimizedContainer.setOnClickListener(v -> {
                if (currentTouchFlowConfig != null) {
                    systemActionHelper.performHaptic(currentTouchFlowConfig.hapticEnabled);
                }
                isDockMinimized = false;
                applyDockMode();
                if (currentTouchFlowConfig != null) {
                    applyTheme(currentTouchFlowConfig);
                }
            });
        }

        btnClose.setOnClickListener(v -> {
            if (currentTouchFlowConfig != null) {
                systemActionHelper.performHaptic(currentTouchFlowConfig.hapticEnabled);
            }
            remove();
        });

        rootView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean dragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isAttached || layoutParams == null) return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        dragging = false;
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (!dragging && Math.hypot(dx, dy) > touchSlop) {
                            dragging = true;
                        }
                        if (dragging) {
                            int w = Math.max(1, rootView.getWidth());
                            int h = Math.max(1, rootView.getHeight());
                            layoutParams.x = Math.max(0, Math.min(screenWidth - w, Math.round(initialX + dx)));
                            layoutParams.y = Math.max(0, Math.min(screenHeight - h, Math.round(initialY + dy)));
                            try {
                                windowManager.updateViewLayout(rootView, layoutParams);
                            } catch (Exception ignored) {
                            }
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (dragging) {
                            int w = Math.max(1, rootView.getWidth());
                            int h = Math.max(1, rootView.getHeight());
                            float rx = (float) layoutParams.x / Math.max(1, screenWidth - w);
                            float ry = (float) layoutParams.y / Math.max(1, screenHeight - h);
                            miniMaxPreferences.saveDockPositionRatio(rx, ry);
                            dragging = false;
                            return true;
                        }
                        return false;
                }
                return false;
            }
        });
    }

    public boolean isAttached() {
        return isAttached;
    }

    public void onConfigurationChanged() {
        updateScreenBounds();
        if (isAttached && rootView != null && layoutParams != null) {
            int w = Math.max(1, rootView.getWidth());
            int h = Math.max(1, rootView.getHeight());
            layoutParams.x = Math.round(miniMaxPreferences.getDockXRatio() * Math.max(0, screenWidth - w));
            layoutParams.y = Math.round(miniMaxPreferences.getDockYRatio() * Math.max(0, screenHeight - h));
            try {
                windowManager.updateViewLayout(rootView, layoutParams);
            } catch (Exception ignored) {
            }
        }
    }

    public void remove() {
        if (isAttached && rootView != null) {
            try {
                windowManager.removeViewImmediate(rootView);
            } catch (Exception ignored) {
            }
        }
        isAttached = false;
        isDockMinimized = false;
        rootView = null;
        expandedContainer = null;
        minimizedContainer = null;
    }
}
