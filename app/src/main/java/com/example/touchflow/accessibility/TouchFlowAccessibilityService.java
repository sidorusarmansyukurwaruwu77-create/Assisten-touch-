package com.example.touchflow.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

import com.example.touchflow.minimax.engine.MiniMaxAutomationEngine;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.services.FloatingOverlayService;
import com.example.touchflow.utilities.LocaleHelper;
import com.example.touchflow.utilities.PermissionHelper;

import java.lang.ref.WeakReference;

public class TouchFlowAccessibilityService extends AccessibilityService {

    private static WeakReference<TouchFlowAccessibilityService> instanceRef = new WeakReference<>(null);
    private static final long WATCHDOG_INTERVAL_MS = 3000L;
    private long lastWatchdogCheckMs = 0L;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instanceRef = new WeakReference<>(this);
        ensureFloatingOverlayAlive();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastWatchdogCheckMs >= WATCHDOG_INTERVAL_MS) {
            lastWatchdogCheckMs = now;
            ensureFloatingOverlayAlive();
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getPackageName() != null) {
            // Read only the active window package name (canRetrieveWindowContent remains false).
            MiniMaxAutomationEngine.getInstance(this)
                    .onWindowStateChanged(event.getPackageName().toString());
        }
    }

    private void ensureFloatingOverlayAlive() {
        try {
            TouchFlowPreferences prefs = new TouchFlowPreferences(this);
            if (prefs.isServiceEnabled()
                    && PermissionHelper.canDrawOverlays(this)
                    && !FloatingOverlayService.isServiceRunning()) {
                FloatingOverlayService.startService(this);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterrupt() {
        // Required override
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instanceRef.clear();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        instanceRef.clear();
        super.onDestroy();
    }

    public static TouchFlowAccessibilityService getInstance() {
        return instanceRef.get();
    }

    public static boolean isConnected() {
        return instanceRef.get() != null;
    }

    public static boolean performHomeRaw() {
        TouchFlowAccessibilityService service = getInstance();
        return service != null && service.performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public static boolean performMinimize() {
        TouchFlowAccessibilityService service = getInstance();
        if (service == null) return false;
        return MiniMaxAutomationEngine.getInstance(service).triggerMinimizeAction();
    }

    public static boolean performHome() {
        TouchFlowAccessibilityService service = getInstance();
        if (service == null) return false;
        boolean ok = service.performGlobalAction(GLOBAL_ACTION_HOME);
        if (ok) {
            try {
                MiniMaxAutomationEngine.getInstance(service).onTouchFlowHomePressed();
            } catch (Exception ignored) {
            }
        }
        return ok;
    }

    public static boolean performBack() {
        TouchFlowAccessibilityService service = getInstance();
        return service != null && service.performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public static boolean performRecents() {
        TouchFlowAccessibilityService service = getInstance();
        return service != null && service.performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    public static boolean performNotifications() {
        TouchFlowAccessibilityService service = getInstance();
        return service != null && service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    public static boolean performQuickSettings() {
        TouchFlowAccessibilityService service = getInstance();
        return service != null && service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
    }

    public static boolean performPowerDialog() {
        TouchFlowAccessibilityService service = getInstance();
        return service != null && service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
    }

    public static boolean performLockScreen() {
        TouchFlowAccessibilityService service = getInstance();
        if (service == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
        return false;
    }

    public static boolean performScreenshot() {
        TouchFlowAccessibilityService service = getInstance();
        if (service == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        }
        return false;
    }
}
