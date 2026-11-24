package com.example.touchflow.utilities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.example.R;
import com.example.touchflow.models.AppShortcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AppPackageHelper {

    private AppPackageHelper() {}

    public static List<AppShortcut> queryLaunchableApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        List<AppShortcut> apps = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        String selfPkg = context.getPackageName();

        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(selfPkg) || seenPackages.contains(pkg)) continue;
            seenPackages.add(pkg);
            CharSequence labelSeq = info.loadLabel(pm);
            String label = labelSeq != null ? labelSeq.toString().trim() : pkg;
            apps.add(new AppShortcut(pkg, label));
        }

        Collections.sort(apps, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
        return apps;
    }

    public static Drawable loadAppIcon(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static boolean launchApp(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(launchIntent);
                return true;
            }
        } catch (Exception ignored) {
        }
        Toast.makeText(context, R.string.msg_app_launch_failed, Toast.LENGTH_SHORT).show();
        return false;
    }
}
