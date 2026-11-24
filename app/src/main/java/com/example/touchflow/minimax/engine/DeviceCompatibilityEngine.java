package com.example.touchflow.minimax.engine;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import com.example.touchflow.minimax.models.DeviceCompatibilityReport;
import com.example.touchflow.minimax.models.OemSkinType;

import java.util.Locale;

public final class DeviceCompatibilityEngine {

    private DeviceCompatibilityEngine() {}

    public static DeviceCompatibilityReport inspect(Context context) {
        PackageManager pm = context.getPackageManager();
        String manufacturer = safeStr(Build.MANUFACTURER);
        String brand = safeStr(Build.BRAND);
        String model = safeStr(Build.MODEL);
        String display = safeStr(Build.DISPLAY);
        String androidVersion = Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "Unknown";
        int sdkInt = Build.VERSION.SDK_INT;

        OemSkinType oemSkin = detectOemSkin(pm, manufacturer, brand, display);

        boolean hasFreeformFeature = pm.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT);
        boolean supportsPipFeature = pm.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
        boolean devFreeformEnabled = isDeveloperFreeformEnabled(context);
        boolean supportsLaunchBounds = sdkInt >= Build.VERSION_CODES.Q;

        String tierLabel;
        String summary;

        if (!supportsLaunchBounds) {
            tierLabel = "Limited (Android < 10)";
            summary = "Android 10+ is recommended for reliable multi-window launch bounds. MiniMax Floating Dock remains available.";
        } else if (hasFreeformFeature || devFreeformEnabled) {
            tierLabel = "Freeform Ready";
            summary = "Official Android freeform launch bounds are active on this device. MiniMax can request floating window sizing directly.";
        } else {
            tierLabel = "OEM Hybrid Mode";
            summary = oemSkin.getDisplayName() + " restricts third-party freeform window overrides unless Freeform Windows is enabled in Developer Options. MiniMax will attempt compatible launches and provide instant Floating Dock fallback.";
        }

        String guidance = buildOemGuidance(oemSkin, hasFreeformFeature || devFreeformEnabled);

        return new DeviceCompatibilityReport(
                manufacturer,
                model,
                brand,
                androidVersion,
                sdkInt,
                oemSkin,
                hasFreeformFeature,
                devFreeformEnabled,
                supportsPipFeature,
                supportsLaunchBounds,
                tierLabel,
                summary,
                guidance
        );
    }

    public static OemSkinType detectOemSkin(PackageManager pm, String manufacturer, String brand, String display) {
        String combined = (manufacturer + " " + brand + " " + display).toLowerCase(Locale.ROOT);

        if (combined.contains("tecno") || isPackageInstalled(pm, "com.transsion.hioslauncher")
                || isPackageInstalled(pm, "com.transsion.smartpanel")) {
            return OemSkinType.TECNO_HIOS;
        }
        if (combined.contains("infinix") || isPackageInstalled(pm, "com.transsion.XOSLauncher")) {
            return OemSkinType.INFINIX_XOS;
        }
        if (combined.contains("samsung") || isPackageInstalled(pm, "com.sec.android.app.launcher")) {
            return OemSkinType.SAMSUNG_ONE_UI;
        }
        if (combined.contains("xiaomi") || combined.contains("redmi") || combined.contains("poco")
                || isPackageInstalled(pm, "com.miui.home")) {
            return OemSkinType.XIAOMI_HYPEROS;
        }
        if (combined.contains("oppo") || combined.contains("realme") || combined.contains("oneplus")
                || isPackageInstalled(pm, "com.oppo.launcher")
                || isPackageInstalled(pm, "com.coloros.floatassistant")) {
            return OemSkinType.OPPO_COLOROS;
        }
        if (combined.contains("vivo") || combined.contains("iqoo")
                || isPackageInstalled(pm, "com.bbk.launcher2")) {
            return OemSkinType.VIVO_FUNTOUCH;
        }
        if (combined.contains("google") || combined.contains("pixel") || combined.contains("aosp")) {
            return OemSkinType.ANDROID_STOCK;
        }
        return OemSkinType.OTHER_OEM;
    }

    public static boolean isDeveloperFreeformEnabled(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "enable_freeform_support", 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPackageInstalled(PackageManager pm, String pkg) {
        try {
            pm.getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String buildOemGuidance(OemSkinType skin, boolean freeformReady) {
        String devNote = freeformReady
                ? "Status: Freeform launch bounds enabled.\n\n"
                : "Tip: To unlock native Android Freeform Windows on non-rooted devices, enable Developer Options → 'Enable freeform windows' and restart the device.\n\n";

        switch (skin) {
            case TECNO_HIOS:
                return devNote + "TECNO HiOS (CAMON 40 & HiOS 14/15):\n"
                        + "• HiOS Lightning Multi-Window / Smart Panel manages system floating windows via HiOS system privileges.\n"
                        + "• Enable 'Enable freeform windows' in Settings → System → Developer options so TouchFlow MiniMax can request official Android freeform bounds.\n"
                        + "• Also allow TouchFlow in Phone Master / Auto-start & Battery Optimization so Accessibility detection stays active on Home press.";
            case INFINIX_XOS:
                return devNote + "Infinix XOS:\n"
                        + "• XOS Lightning Multi-Window runs as a system service.\n"
                        + "• Turn on 'Enable freeform windows' in Developer options for native Android multi-window bounds.\n"
                        + "• Whitelist TouchFlow in Phone Master battery settings.";
            case SAMSUNG_ONE_UI:
                return devNote + "Samsung One UI:\n"
                        + "• One UI supports Pop-up View natively via Labs ('Multi window for all apps').\n"
                        + "• Enable Settings → Developer options → 'Enable freeform windows' for automatic bounds launching.";
            case XIAOMI_HYPEROS:
                return devNote + "Xiaomi HyperOS / MIUI:\n"
                        + "• HyperOS floating windows are controlled by MIUI Security/SystemUI.\n"
                        + "• Enable 'Display pop-up windows while running in the background' in App Info → Other permissions, and enable 'Enable freeform windows' in Developer options.";
            case OPPO_COLOROS:
                return devNote + "OPPO ColorOS / Realme UI:\n"
                        + "• ColorOS Flexible Windows is managed by system launcher.\n"
                        + "• Enable 'Enable freeform windows' in Developer options for Android freeform API support.";
            case VIVO_FUNTOUCH:
                return devNote + "Vivo Funtouch / OriginOS:\n"
                        + "• Funtouch Small Window requires system-signed permissions.\n"
                        + "• Enable 'Enable freeform windows' in Developer options and allow background high power usage for TouchFlow.";
            case ANDROID_STOCK:
            default:
                return devNote + "Android Stock / AOSP (Android 10–16):\n"
                        + "• Uses official ActivityOptions.setLaunchBounds() when system freeform is enabled.\n"
                        + "• When freeform is disabled by the system, MiniMax switches gracefully to the TouchFlow Floating Assistant Dock.";
        }
    }

    private static String safeStr(String val) {
        return val != null ? val.trim() : "Unknown";
    }
}
