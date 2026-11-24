package com.example.touchflow.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.services.FloatingOverlayService;
import com.example.touchflow.utilities.PermissionHelper;

public class OverlayRestartReceiver extends BroadcastReceiver {

    public static final String ACTION_RESTART_OVERLAY = "com.example.touchflow.ACTION_RESTART_OVERLAY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        if (!ACTION_RESTART_OVERLAY.equals(action)
                && !Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        TouchFlowPreferences prefs = new TouchFlowPreferences(context);
        if (prefs.isServiceEnabled()
                && PermissionHelper.canDrawOverlays(context)
                && !FloatingOverlayService.isServiceRunning()) {
            try {
                FloatingOverlayService.startService(context.getApplicationContext());
            } catch (Exception ignored) {
            }
        }
    }
}
