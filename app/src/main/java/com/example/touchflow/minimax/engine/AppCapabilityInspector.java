package com.example.touchflow.minimax.engine;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class AppCapabilityInspector {

    private static final Set<String> SYSTEM_IGNORED_PACKAGES = new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.google.android.inputmethod.latin"
    ));

    private AppCapabilityInspector() {}

    public static String resolveDefaultLauncherPackage(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return "";
    }

    public static boolean isSystemOrLauncherPackage(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return true;
        if (packageName.equals(context.getPackageName())) return true;
        if (SYSTEM_IGNORED_PACKAGES.contains(packageName)) return true;

        String defaultLauncher = resolveDefaultLauncherPackage(context);
        if (!defaultLauncher.isEmpty() && defaultLauncher.equals(packageName)) {
            return true;
        }
        return isAnyHomeLauncher(context, packageName);
    }

    public static boolean isAnyHomeLauncher(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        PackageManager pm = context.getPackageManager();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo info : pm.queryIntentActivities(homeIntent, 0)) {
            if (info.activityInfo != null && packageName.equals(info.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLaunchableApp(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        return context.getPackageManager().getLaunchIntentForPackage(packageName) != null;
    }

    public static String getAppLabel(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return "";
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    public static boolean isVideoCategoryApp(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return appInfo.category == ApplicationInfo.CATEGORY_VIDEO;
        } catch (Exception ignored) {
            return false;
        }
    }
}
