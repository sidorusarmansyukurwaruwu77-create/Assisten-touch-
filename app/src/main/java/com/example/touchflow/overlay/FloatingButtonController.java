package com.example.touchflow.overlay;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.widget.ImageViewCompat;

import com.example.R;
import com.example.touchflow.models.ActionItem;
import com.example.touchflow.models.TouchFlowConfig;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.utilities.SystemActionHelper;
import com.example.touchflow.utilities.ViewAnimationHelper;

public class FloatingButtonController {

    public interface Listener {
        void onActionTriggered(ActionItem action);
    }

    private static final long DOUBLE_TAP_TIMEOUT_MS = 260L;
    private static final long IDLE_DIM_DELAY_MS = 3200L;

    private final Context context;
    private final WindowManager windowManager;
    private final TouchFlowPreferences preferences;
    private final SystemActionHelper systemActionHelper;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View rootView;
    private FrameLayout orbContainer;
    private ImageView orbIcon;
    private WindowManager.LayoutParams layoutParams;
    private TouchFlowConfig currentConfig;
    private boolean isAttached = false;
    private boolean isHiddenForPanel = false;
    private ValueAnimator edgeAnimator;

    private int screenWidth;
    private int screenHeight;
    private int touchSlop;
    private long longPressTimeout;

    private final Runnable idleDimRunnable = () -> {
        if (rootView != null && currentConfig != null && currentConfig.idleDimEnabled && isAttached && !isHiddenForPanel) {
            float targetAlpha = Math.max(0.25f, (currentConfig.opacityPercent / 100f) * 0.55f);
            rootView.animate().alpha(targetAlpha).setDuration(350L).start();
        }
    };

    public FloatingButtonController(Context context,
                                    WindowManager windowManager,
                                    TouchFlowPreferences preferences,
                                    SystemActionHelper systemActionHelper,
                                    Listener listener) {
        this.context = context;
        this.windowManager = windowManager;
        this.preferences = preferences;
        this.systemActionHelper = systemActionHelper;
        this.listener = listener;
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.longPressTimeout = ViewConfiguration.getLongPressTimeout();
        updateScreenBounds();
    }

    public void updateScreenBounds() {
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        screenWidth = Math.max(320, size.x);
        screenHeight = Math.max(480, size.y);
    }

    @SuppressLint("InflateParams")
    public void show(TouchFlowConfig config) {
        this.currentConfig = config;
        if (isAttached) {
            applyConfig(config);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        rootView = inflater.inflate(R.layout.overlay_floating_button, null);
        orbContainer = rootView.findViewById(R.id.floating_orb_container);
        orbIcon = rootView.findViewById(R.id.floating_orb_icon);

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;

        applyConfig(config);
        restorePositionFromRatio();
        setupTouchHandling();

        try {
            windowManager.addView(rootView, layoutParams);
            isAttached = true;
            ViewAnimationHelper.animateSpringScale(orbContainer, 1.0f);
            scheduleIdleDim();
        } catch (Exception ignored) {
            isAttached = false;
        }
    }

    public void applyConfig(TouchFlowConfig config) {
        this.currentConfig = config;
        if (rootView == null || orbContainer == null || orbIcon == null) return;

        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = Math.round(config.buttonSizeDp * density);
        FrameLayout.LayoutParams orbParams = (FrameLayout.LayoutParams) orbContainer.getLayoutParams();
        orbParams.width = sizePx;
        orbParams.height = sizePx;
        orbContainer.setLayoutParams(orbParams);

        int iconSizePx = Math.round(sizePx * 0.52f);
        FrameLayout.LayoutParams iconParams = (FrameLayout.LayoutParams) orbIcon.getLayoutParams();
        iconParams.width = iconSizePx;
        iconParams.height = iconSizePx;
        orbIcon.setLayoutParams(iconParams);

        orbIcon.setImageResource(config.orbIconStyle.getDrawableRes());
        ImageViewCompat.setImageTintList(orbIcon, android.content.res.ColorStateList.valueOf(config.iconColor));

        OverlayThemeApplier.applyFloatingOrbStyle(context, orbContainer, config);
        rootView.setAlpha(config.opacityPercent / 100f);

        if (isAttached) {
            try {
                windowManager.updateViewLayout(rootView, layoutParams);
            } catch (Exception ignored) {
            }
            scheduleIdleDim();
        }
    }

    public void onConfigurationChanged() {
        updateScreenBounds();
        if (isAttached && rootView != null && layoutParams != null) {
            restorePositionFromRatio();
            try {
                windowManager.updateViewLayout(rootView, layoutParams);
            } catch (Exception ignored) {
            }
        }
    }

    public void setTemporarilyHidden(boolean hidden) {
        isHiddenForPanel = hidden;
        if (rootView == null) return;
        handler.removeCallbacks(idleDimRunnable);
        if (hidden) {
            rootView.setVisibility(View.GONE);
        } else {
            rootView.setVisibility(View.VISIBLE);
            if (currentConfig != null) {
                rootView.setAlpha(currentConfig.opacityPercent / 100f);
            }
            ViewAnimationHelper.animateSpringScale(orbContainer, 1.0f);
            scheduleIdleDim();
        }
    }

    public void remove() {
        handler.removeCallbacksAndMessages(null);
        if (edgeAnimator != null) {
            edgeAnimator.cancel();
            edgeAnimator = null;
        }
        if (isAttached && rootView != null) {
            try {
                windowManager.removeViewImmediate(rootView);
            } catch (Exception ignored) {
            }
        }
        isAttached = false;
        rootView = null;
        orbContainer = null;
        orbIcon = null;
    }

    private void restorePositionFromRatio() {
        if (layoutParams == null) return;
        float density = context.getResources().getDisplayMetrics().density;
        int viewSize = Math.round(((currentConfig != null ? currentConfig.buttonSizeDp : 56) + 12) * density);
        int maxX = Math.max(0, screenWidth - viewSize);
        int maxY = Math.max(0, screenHeight - viewSize);
        layoutParams.x = Math.round(preferences.getButtonXRatio() * maxX);
        layoutParams.y = Math.round(preferences.getButtonYRatio() * maxY);
    }

    private void savePositionRatio() {
        if (layoutParams == null || rootView == null) return;
        int width = Math.max(1, rootView.getWidth());
        int height = Math.max(1, rootView.getHeight());
        int maxX = Math.max(1, screenWidth - width);
        int maxY = Math.max(1, screenHeight - height);
        float xRatio = (float) layoutParams.x / maxX;
        float yRatio = (float) layoutParams.y / maxY;
        preferences.saveButtonPositionRatio(xRatio, yRatio);
    }

    private void scheduleIdleDim() {
        handler.removeCallbacks(idleDimRunnable);
        if (currentConfig != null && currentConfig.idleDimEnabled && !isHiddenForPanel) {
            handler.postDelayed(idleDimRunnable, IDLE_DIM_DELAY_MS);
        }
    }

    private void restoreActiveOpacity() {
        handler.removeCallbacks(idleDimRunnable);
        if (rootView != null && currentConfig != null) {
            rootView.animate().alpha(currentConfig.opacityPercent / 100f).setDuration(120L).start();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchHandling() {
        orbContainer.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long downTimeMs;
            private boolean isDragging = false;
            private boolean longPressTriggered = false;
            private int pendingTapCount = 0;

            private final Runnable longPressRunnable = () -> {
                if (!isDragging && isAttached) {
                    longPressTriggered = true;
                    pendingTapCount = 0;
                    triggerHaptic();
                    ViewAnimationHelper.animatePressScale(orbContainer, false);
                    if (currentConfig != null && listener != null) {
                        listener.onActionTriggered(currentConfig.longPressAction);
                    }
                }
            };

            private final Runnable singleTapRunnable = () -> {
                if (pendingTapCount == 1 && currentConfig != null && listener != null) {
                    pendingTapCount = 0;
                    listener.onActionTriggered(currentConfig.singleTapAction);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isAttached || layoutParams == null) return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (edgeAnimator != null) edgeAnimator.cancel();
                        restoreActiveOpacity();
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        downTimeMs = System.currentTimeMillis();
                        isDragging = false;
                        longPressTriggered = false;
                        ViewAnimationHelper.animatePressScale(orbContainer, true);
                        handler.postDelayed(longPressRunnable, longPressTimeout);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (!isDragging && (Math.hypot(dx, dy) > touchSlop)) {
                            isDragging = true;
                            handler.removeCallbacks(longPressRunnable);
                            handler.removeCallbacks(singleTapRunnable);
                            pendingTapCount = 0;
                        }
                        if (isDragging) {
                            int viewW = Math.max(1, rootView.getWidth());
                            int viewH = Math.max(1, rootView.getHeight());
                            layoutParams.x = clamp(Math.round(initialX + dx), 0, Math.max(0, screenWidth - viewW));
                            layoutParams.y = clamp(Math.round(initialY + dy), 0, Math.max(0, screenHeight - viewH));
                            try {
                                windowManager.updateViewLayout(rootView, layoutParams);
                            } catch (Exception ignored) {
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(longPressRunnable);
                        ViewAnimationHelper.animatePressScale(orbContainer, false);

                        if (longPressTriggered) {
                            scheduleIdleDim();
                            return true;
                        }

                        if (isDragging) {
                            long duration = System.currentTimeMillis() - downTimeMs;
                            float totalDx = event.getRawX() - initialTouchX;
                            float totalDy = event.getRawY() - initialTouchY;
                            float distance = (float) Math.hypot(totalDx, totalDy);

                            // Detect quick directional flick / swipe if configured
                            boolean swipeHandled = false;
                            if (duration < 280L && distance > touchSlop * 4f
                                    && currentConfig != null
                                    && currentConfig.swipeAction != ActionItem.NONE) {
                                // Check if user intentionally swiped up/down/across fast without dragging far across screen
                                if (Math.abs(totalDy) > Math.abs(totalDx) * 1.4f && distance < screenHeight * 0.28f) {
                                    swipeHandled = true;
                                    triggerHaptic();
                                    // Restore pre-flick position nicely
                                    layoutParams.x = initialX;
                                    layoutParams.y = initialY;
                                    snapOrSavePosition();
                                    if (listener != null) {
                                        listener.onActionTriggered(currentConfig.swipeAction);
                                    }
                                }
                            }
                            if (!swipeHandled) {
                                snapOrSavePosition();
                            }
                            scheduleIdleDim();
                            return true;
                        }

                        // Handle Single Tap vs Double Tap
                        triggerHaptic();
                        if (currentConfig != null && currentConfig.doubleTapAction == ActionItem.NONE) {
                            pendingTapCount = 0;
                            if (listener != null) {
                                listener.onActionTriggered(currentConfig.singleTapAction);
                            }
                        } else {
                            pendingTapCount++;
                            if (pendingTapCount == 1) {
                                handler.postDelayed(singleTapRunnable, DOUBLE_TAP_TIMEOUT_MS);
                            } else if (pendingTapCount >= 2) {
                                handler.removeCallbacks(singleTapRunnable);
                                pendingTapCount = 0;
                                if (currentConfig != null && listener != null) {
                                    listener.onActionTriggered(currentConfig.doubleTapAction);
                                }
                            }
                        }
                        scheduleIdleDim();
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longPressRunnable);
                        ViewAnimationHelper.animatePressScale(orbContainer, false);
                        snapOrSavePosition();
                        scheduleIdleDim();
                        return true;
                }
                return false;
            }
        });
    }

    private void snapOrSavePosition() {
        if (layoutParams == null || rootView == null) return;
        int viewW = Math.max(1, rootView.getWidth());
        int maxX = Math.max(0, screenWidth - viewW);
        if (currentConfig != null && currentConfig.edgeSnapEnabled) {
            int center = layoutParams.x + (viewW / 2);
            int targetX = (center < screenWidth / 2) ? 0 : maxX;
            long animMs = currentConfig.getAnimationDurationMs();
            edgeAnimator = ViewAnimationHelper.animateWindowX(
                    layoutParams.x,
                    targetX,
                    animMs,
                    animation -> {
                        if (!isAttached || rootView == null) return;
                        layoutParams.x = (int) animation.getAnimatedValue();
                        try {
                            windowManager.updateViewLayout(rootView, layoutParams);
                        } catch (Exception ignored) {
                        }
                    },
                    this::savePositionRatio
            );
        } else {
            savePositionRatio();
        }
    }

    private void triggerHaptic() {
        if (currentConfig != null) {
            systemActionHelper.performHaptic(currentConfig.hapticEnabled);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
