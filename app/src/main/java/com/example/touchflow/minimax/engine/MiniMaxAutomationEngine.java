package com.example.touchflow.minimax.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.touchflow.minimax.models.AutomationLogEntry;
import com.example.touchflow.minimax.models.AutomationTriggerType;
import com.example.touchflow.minimax.models.DeviceCompatibilityReport;
import com.example.touchflow.minimax.models.MiniMaxConfig;
import com.example.touchflow.minimax.preferences.MiniMaxPreferences;

public class MiniMaxAutomationEngine {

    public interface DockEventListener {
        void onMiniMaxDockRequested(String packageName, String appLabel, String statusDetail, boolean wasFreeformLaunched);
    }

    private static final long DEBOUNCE_MS = 1500L;

    private static volatile MiniMaxAutomationEngine instance;

    private final Context appContext;
    private final MiniMaxPreferences preferences;
    private final FloatingWindowAdapter windowAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DockEventListener dockListener;
    private String currentForegroundPackage = "";
    private String lastActiveAppPackage = "";
    private long lastTriggerTimeMs = 0L;
    private String lastTriggeredPackage = "";

    private MiniMaxAutomationEngine(Context context) {
        this.appContext = context.getApplicationContext();
        this.preferences = new MiniMaxPreferences(this.appContext);
        this.windowAdapter = new FloatingWindowAdapter(this.appContext);
        this.lastActiveAppPackage = preferences.getLastActiveAppPackage();
    }

    public static MiniMaxAutomationEngine getInstance(Context context) {
        if (instance == null) {
            synchronized (MiniMaxAutomationEngine.class) {
                if (instance == null) {
                    instance = new MiniMaxAutomationEngine(context);
                }
            }
        }
        return instance;
    }

    public void setDockEventListener(DockEventListener listener) {
        this.dockListener = listener;
    }

    public String getLastActiveAppPackage() {
        return lastActiveAppPackage;
    }

    public void onWindowStateChanged(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        if (packageName.equals(currentForegroundPackage)) return;

        String previousForeground = currentForegroundPackage;
        currentForegroundPackage = packageName;

        boolean isLauncher = AppCapabilityInspector.isAnyHomeLauncher(appContext, packageName);
        if (isLauncher) {
            if (!previousForeground.isEmpty() && isEligibleApp(previousForeground)) {
                lastActiveAppPackage = previousForeground;
                preferences.setLastActiveAppPackage(previousForeground);
                handleTrigger(previousForeground, AutomationTriggerType.HOME_PRESS);
            }
            return;
        }

        if (AppCapabilityInspector.isSystemOrLauncherPackage(appContext, packageName)) {
            return;
        }

        // Valid user app came to foreground
        if (!previousForeground.isEmpty()
                && !previousForeground.equals(packageName)
                && isEligibleApp(previousForeground)) {
            handleTrigger(previousForeground, AutomationTriggerType.APP_SWITCH);
        }

        if (AppCapabilityInspector.isLaunchableApp(appContext, packageName)) {
            lastActiveAppPackage = packageName;
            preferences.setLastActiveAppPackage(packageName);
        }
    }

    public void onTouchFlowHomePressed() {
        String candidate = !currentForegroundPackage.isEmpty()
                && !AppCapabilityInspector.isSystemOrLauncherPackage(appContext, currentForegroundPackage)
                ? currentForegroundPackage
                : lastActiveAppPackage;

        if (isEligibleApp(candidate)) {
            lastActiveAppPackage = candidate;
            preferences.setLastActiveAppPackage(candidate);
            handleTrigger(candidate, AutomationTriggerType.HOME_PRESS);
        }
    }

    public FloatingWindowAdapter.LaunchResult triggerManualMiniLaunch(String packageName) {
        String target = (packageName != null && !packageName.isEmpty()) ? packageName : lastActiveAppPackage;
        if (target == null || target.isEmpty()) {
            return new FloatingWindowAdapter.LaunchResult(
                    false, "NO_TARGET", "Open another app first so MiniMax tracks your last active app.", false
            );
        }

        MiniMaxConfig config = preferences.loadConfig();
        DeviceCompatibilityReport report = DeviceCompatibilityEngine.inspect(appContext);
        FloatingWindowAdapter.LaunchResult result = windowAdapter.tryLaunchMiniWindow(target, config, report);
        String label = AppCapabilityInspector.getAppLabel(appContext, target);

        preferences.addHistoryEntry(new AutomationLogEntry(
                System.currentTimeMillis(),
                target,
                label,
                AutomationTriggerType.MANUAL_LAUNCH.getLabel(),
                result.mechanism,
                result.launchedInWindow,
                result.detail
        ));

        if (!result.launchedInWindow && result.showFloatingFallback) {
            notifyDock(target, label, result.detail, false);
        }
        return result;
    }

    public void openLastAppNormally(String packageName) {
        windowAdapter.launchAppNormally(packageName);
    }

    private void handleTrigger(String targetPackage, AutomationTriggerType triggerType) {
        MiniMaxConfig config = preferences.loadConfig();
        if (!config.autoMiniEnabled) return;
        if (triggerType == AutomationTriggerType.HOME_PRESS && !config.autoMiniOnHome) return;
        if (triggerType == AutomationTriggerType.APP_SWITCH && !config.autoMiniOnAppSwitch) return;

        if (!passesAppFilters(targetPackage, config)) {
            return;
        }

        long now = System.currentTimeMillis();
        if ((now - lastTriggerTimeMs) < DEBOUNCE_MS && targetPackage.equals(lastTriggeredPackage)) {
            return;
        }
        lastTriggerTimeMs = now;
        lastTriggeredPackage = targetPackage;

        DeviceCompatibilityReport report = DeviceCompatibilityEngine.inspect(appContext);
        FloatingWindowAdapter.LaunchResult result = windowAdapter.tryLaunchMiniWindow(targetPackage, config, report);
        String appLabel = AppCapabilityInspector.getAppLabel(appContext, targetPackage);

        preferences.addHistoryEntry(new AutomationLogEntry(
                now,
                targetPackage,
                appLabel,
                triggerType.getLabel(),
                result.mechanism,
                result.launchedInWindow,
                result.detail
        ));

        if (config.floatingAssistantEnabled && (result.showFloatingFallback || result.launchedInWindow)) {
            notifyDock(targetPackage, appLabel, result.detail, result.launchedInWindow);
        }
    }

    private boolean isEligibleApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        if (AppCapabilityInspector.isSystemOrLauncherPackage(appContext, packageName)) return false;
        return AppCapabilityInspector.isLaunchableApp(appContext, packageName);
    }

    private boolean passesAppFilters(String packageName, MiniMaxConfig config) {
        if (!isEligibleApp(packageName)) return false;
        if (config.excludedPackages.contains(packageName)) {
            return false;
        }
        if (config.onlySupportedAppsMode) {
            return config.supportedPackages.contains(packageName);
        }
        return true;
    }

    private void notifyDock(String packageName, String appLabel, String detail, boolean launched) {
        mainHandler.post(() -> {
            if (dockListener != null) {
                dockListener.onMiniMaxDockRequested(packageName, appLabel, detail, launched);
            }
        });
    }
}
