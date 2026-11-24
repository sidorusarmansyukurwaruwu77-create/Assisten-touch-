package com.example.touchflow.minimax.engine;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.WindowManager;

import com.example.touchflow.minimax.models.DeviceCompatibilityReport;
import com.example.touchflow.minimax.models.MiniMaxConfig;
import com.example.touchflow.minimax.models.MiniWindowPosition;
import com.example.touchflow.minimax.models.MiniWindowSize;

public class FloatingWindowAdapter {

    public static class LaunchResult {
        public final boolean launchedInWindow;
        public final String mechanism;
        public final String detail;
        public final boolean showFloatingFallback;

        public LaunchResult(boolean launchedInWindow, String mechanism, String detail, boolean showFloatingFallback) {
            this.launchedInWindow = launchedInWindow;
            this.mechanism = mechanism;
            this.detail = detail;
            this.showFloatingFallback = showFloatingFallback;
        }
    }

    private final Context appContext;

    public FloatingWindowAdapter(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public LaunchResult tryLaunchMiniWindow(String packageName,
                                            MiniMaxConfig config,
                                            DeviceCompatibilityReport report) {
        if (packageName == null || packageName.isEmpty()) {
            return new LaunchResult(false, "NO_TARGET", "No active app detected.", false);
        }

        PackageManager pm = appContext.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            return new LaunchResult(false, "UNLAUNCHABLE", "App has no launcher activity.", false);
        }

        boolean isVideoApp = AppCapabilityInspector.isVideoCategoryApp(appContext, packageName);

        // Only attempt official ActivityOptions.setLaunchBounds when freeform is genuinely enabled
        if (report.canAttemptNativeFreeform()) {
            try {
                Rect bounds = computeLaunchBounds(config.windowPosition, config.windowSize);
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchBounds(bounds);

                launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                                | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                );
                appContext.startActivity(launchIntent, options.toBundle());

                return new LaunchResult(
                        true,
                        "FREEFORM_BOUNDS",
                        "Requested freeform window (" + config.windowSize.getDisplayName() + ", "
                                + config.windowPosition.getDisplayName() + ")"
                                + (isVideoApp ? " • Video app" : ""),
                        false
                );
            } catch (Exception e) {
                return new LaunchResult(
                        false,
                        "FREEFORM_FALLBACK",
                        "Freeform launch rejected by OEM (" + e.getClass().getSimpleName() + "). Showing MiniMax Dock.",
                        true
                );
            }
        }

        // Honest fallback when OEM (e.g. TECNO HiOS / ColorOS / One UI) has system freeform locked
        String pipNote = report.supportsPictureInPictureFeature && isVideoApp
                ? "System PiP works only when initiated by the video app itself."
                : "3rd-party apps cannot force another app into PiP or OEM floating window.";

        return new LaunchResult(
                false,
                "OEM_DOCK_FALLBACK",
                report.oemSkin.getDisplayName() + " restricts third-party freeform bounds. "
                        + pipNote + " MiniMax Dock ready.",
                true
        );
    }

    public boolean launchAppNormally(String packageName) {
        try {
            Intent launchIntent = appContext.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent == null) return false;
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            appContext.startActivity(launchIntent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Rect computeLaunchBounds(MiniWindowPosition position, MiniWindowSize size) {
        WindowManager wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        int screenW = 1080;
        int screenH = 2400;
        if (wm != null) {
            Point p = new Point();
            wm.getDefaultDisplay().getSize(p);
            screenW = Math.max(480, p.x);
            screenH = Math.max(800, p.y);
        }

        float scale = size.getScaleFraction();
        int winW = Math.round(screenW * scale);
        int winH = Math.round(screenH * (scale * 0.72f));
        int margin = Math.round(24 * appContext.getResources().getDisplayMetrics().density);
        int topSafe = Math.round(64 * appContext.getResources().getDisplayMetrics().density);
        int bottomSafe = Math.round(80 * appContext.getResources().getDisplayMetrics().density);

        int left;
        int top;
        switch (position) {
            case TOP_LEFT:
                left = margin;
                top = topSafe;
                break;
            case CENTER:
                left = (screenW - winW) / 2;
                top = (screenH - winH) / 2;
                break;
            case BOTTOM_LEFT:
                left = margin;
                top = Math.max(topSafe, screenH - winH - bottomSafe);
                break;
            case BOTTOM_RIGHT:
                left = Math.max(margin, screenW - winW - margin);
                top = Math.max(topSafe, screenH - winH - bottomSafe);
                break;
            case TOP_RIGHT:
            default:
                left = Math.max(margin, screenW - winW - margin);
                top = topSafe;
                break;
        }
        return new Rect(left, top, left + winW, top + winH);
    }
}
