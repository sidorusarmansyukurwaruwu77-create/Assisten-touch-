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
import com.example.touchflow.utilities.SystemActionHelper;
import com.example.touchflow.utilities.ViewAnimationHelper;

public class MiniMaxFloatingController {

    private final Context context;
    private final WindowManager windowManager;
    private final MiniMaxPreferences miniMaxPreferences;
    private final SystemActionHelper systemActionHelper;

    private View rootView;
    private WindowManager.LayoutParams layoutParams;
    private boolean isAttached = false;
    private String trackedPackage = "";
    private String statusSubtitle = "Tap for Mini Window";
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
    public void showOrUpdate(TouchFlowConfig touchFlowConfig, String packageName, String appLabel, String subtitle) {
        this.currentTouchFlowConfig = touchFlowConfig;
        if (packageName != null && !packageName.isEmpty()) {
            this.trackedPackage = packageName;
        } else {
            this.trackedPackage = miniMaxPreferences.getLastActiveAppPackage();
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            this.statusSubtitle = subtitle;
        }

        if (!isAttached) {
            LayoutInflater inflater = LayoutInflater.from(context);
            rootView = inflater.inflate(R.layout.overlay_minimax_dock, null);

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
            applyTheme(touchFlowConfig);
        }
    }

    public void applyTheme(TouchFlowConfig touchFlowConfig) {
        this.currentTouchFlowConfig = touchFlowConfig;
        if (!isAttached || rootView == null) return;
        OverlayThemeApplier.applyPanelBackground(context, rootView, touchFlowConfig, 22f);

        int primaryText = OverlayThemeApplier.getPrimaryTextColor(touchFlowConfig);
        int accent = OverlayThemeApplier.getAccentColor(touchFlowConfig);

        TextView title = rootView.findViewById(R.id.tv_minimax_dock_title);
        TextView sub = rootView.findViewById(R.id.tv_minimax_dock_sub);
        ImageButton btnMini = rootView.findViewById(R.id.btn_minimax_dock_open_full);
        ImageButton btnClose = rootView.findViewById(R.id.btn_minimax_dock_close);

        title.setTextColor(primaryText);
        sub.setTextColor(accent);
        ImageViewCompat.setImageTintList(btnMini, ColorStateList.valueOf(accent));
        ImageViewCompat.setImageTintList(btnClose, ColorStateList.valueOf(primaryText));
    }

    private void bindContent(String explicitLabel) {
        if (rootView == null) return;
        ImageView ivApp = rootView.findViewById(R.id.iv_minimax_dock_app);
        TextView tvTitle = rootView.findViewById(R.id.tv_minimax_dock_title);
        TextView tvSub = rootView.findViewById(R.id.tv_minimax_dock_sub);

        if (trackedPackage != null && !trackedPackage.isEmpty()) {
            String label = (explicitLabel != null && !explicitLabel.isEmpty())
                    ? explicitLabel
                    : AppCapabilityInspector.getAppLabel(context, trackedPackage);
            tvTitle.setText(label);
            tvSub.setText(statusSubtitle);
            Drawable icon = AppPackageHelper.loadAppIcon(context, trackedPackage);
            if (icon != null) {
                ivApp.setImageDrawable(icon);
            } else {
                ivApp.setImageResource(R.drawable.ic_minimax);
            }
        } else {
            tvTitle.setText(R.string.minimax_title);
            tvSub.setText("Open an app to track");
            ivApp.setImageResource(R.drawable.ic_minimax);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupInteractions() {
        View btnLaunch = rootView.findViewById(R.id.btn_minimax_dock_launch);
        ImageButton btnOpenMini = rootView.findViewById(R.id.btn_minimax_dock_open_full);
        ImageButton btnClose = rootView.findViewById(R.id.btn_minimax_dock_close);

        View.OnClickListener launchClick = v -> {
            if (currentTouchFlowConfig != null) {
                systemActionHelper.performHaptic(currentTouchFlowConfig.hapticEnabled);
            }
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
                        context,
                        "OEM freeform restricted — opening " + AppCapabilityInspector.getAppLabel(context, trackedPackage),
                        Toast.LENGTH_SHORT
                ).show();
                MiniMaxAutomationEngine.getInstance(context).openLastAppNormally(trackedPackage);
            }
        };

        btnLaunch.setOnClickListener(launchClick);
        btnOpenMini.setOnClickListener(launchClick);

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
        rootView = null;
    }
}
