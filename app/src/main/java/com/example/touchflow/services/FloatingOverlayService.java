package com.example.touchflow.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.R;
import com.example.touchflow.activities.MainActivity;
import com.example.touchflow.models.ActionItem;
import com.example.touchflow.models.TouchFlowConfig;
import com.example.touchflow.overlay.FloatingButtonController;
import com.example.touchflow.overlay.FloatingMenuController;
import com.example.touchflow.overlay.QuickAccessPanelController;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.utilities.PermissionHelper;
import com.example.touchflow.utilities.SystemActionHelper;

public class FloatingOverlayService extends Service {

    public static final String ACTION_START = "com.example.touchflow.ACTION_START";
    public static final String ACTION_STOP = "com.example.touchflow.ACTION_STOP";
    public static final String ACTION_OPEN_MENU = "com.example.touchflow.ACTION_OPEN_MENU";
    public static final String ACTION_OPEN_QUICK_PANEL = "com.example.touchflow.ACTION_OPEN_QUICK_PANEL";

    private static final String CHANNEL_ID = "touchflow_overlay_channel";
    private static final int NOTIFICATION_ID = 1042;
    private static volatile boolean isRunning = false;

    private TouchFlowPreferences preferences;
    private SystemActionHelper systemActionHelper;
    private WindowManager windowManager;
    private FloatingButtonController buttonController;
    private FloatingMenuController menuController;
    private QuickAccessPanelController quickPanelController;
    private TouchFlowConfig currentConfig;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
            (sharedPreferences, key) -> {
                if (preferences == null) return;
                currentConfig = preferences.loadConfig();
                if (!currentConfig.serviceEnabled) {
                    stopOverlayAndSelf();
                    return;
                }
                if (buttonController != null) {
                    buttonController.applyConfig(currentConfig);
                }
            };

    public static boolean isServiceRunning() {
        return isRunning;
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, FloatingOverlayService.class);
        intent.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, FloatingOverlayService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static void openQuickPanelFromApp(Context context) {
        Intent intent = new Intent(context, FloatingOverlayService.class);
        intent.setAction(ACTION_OPEN_QUICK_PANEL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = new TouchFlowPreferences(this);
        systemActionHelper = new SystemActionHelper(this);
        systemActionHelper.registerTorchListener(null);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        currentConfig = preferences.loadConfig();

        buttonController = new FloatingButtonController(
                this,
                windowManager,
                preferences,
                systemActionHelper,
                this::handleGestureAction
        );

        menuController = new FloatingMenuController(
                this,
                windowManager,
                preferences,
                systemActionHelper,
                new FloatingMenuController.Listener() {
                    @Override
                    public void onMenuClosed() {
                        buttonController.setTemporarilyHidden(false);
                    }

                    @Override
                    public void onOpenQuickPanel() {
                        showQuickPanel();
                    }

                    @Override
                    public void onExecuteAction(ActionItem action) {
                        executeSystemAction(action);
                    }
                }
        );

        quickPanelController = new QuickAccessPanelController(
                this,
                windowManager,
                preferences,
                systemActionHelper,
                new QuickAccessPanelController.Listener() {
                    @Override
                    public void onPanelClosed() {
                        buttonController.setTemporarilyHidden(false);
                    }

                    @Override
                    public void onBackToMenu() {
                        showMainMenu();
                    }
                }
        );

        preferences.registerListener(prefsListener);
        createNotificationChannel();
        startInForeground();
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!PermissionHelper.canDrawOverlays(this)) {
            preferences.setServiceEnabled(false);
            stopOverlayAndSelf();
            return START_NOT_STICKY;
        }

        currentConfig = preferences.loadConfig();
        String action = intent != null ? intent.getAction() : ACTION_START;

        if (ACTION_STOP.equals(action)) {
            preferences.setServiceEnabled(false);
            stopOverlayAndSelf();
            return START_NOT_STICKY;
        }

        buttonController.show(currentConfig);

        if (ACTION_OPEN_MENU.equals(action)) {
            showMainMenu();
        } else if (ACTION_OPEN_QUICK_PANEL.equals(action)) {
            showQuickPanel();
        }

        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (buttonController != null) {
            buttonController.onConfigurationChanged();
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (preferences != null) {
            preferences.unregisterListener(prefsListener);
        }
        if (systemActionHelper != null) {
            systemActionHelper.unregisterTorchListener();
        }
        if (menuController != null) menuController.removeImmediate();
        if (quickPanelController != null) quickPanelController.removeImmediate();
        if (buttonController != null) buttonController.remove();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleGestureAction(ActionItem action) {
        if (action == null || action == ActionItem.NONE) return;
        if (action == ActionItem.OPEN_MENU) {
            showMainMenu();
        } else if (action == ActionItem.QUICK_PANEL) {
            showQuickPanel();
        } else {
            executeSystemAction(action);
        }
    }

    private void showMainMenu() {
        if (quickPanelController.isShowing()) {
            quickPanelController.removeImmediate();
        }
        if (!menuController.isShowing()) {
            currentConfig = preferences.loadConfig();
            buttonController.setTemporarilyHidden(true);
            menuController.show(currentConfig);
        }
    }

    private void showQuickPanel() {
        if (menuController.isShowing()) {
            menuController.removeImmediate();
        }
        if (!quickPanelController.isShowing()) {
            currentConfig = preferences.loadConfig();
            buttonController.setTemporarilyHidden(true);
            quickPanelController.show(currentConfig);
        }
    }

    private void executeSystemAction(ActionItem action) {
        systemActionHelper.executeAction(action, new SystemActionHelper.OverlayNavigationCallback() {
            @Override
            public void onRequestCloseMenu() {
                if (menuController.isShowing()) {
                    menuController.dismiss(true);
                }
                if (quickPanelController.isShowing()) {
                    quickPanelController.dismiss(true);
                }
            }

            @Override
            public void onRequestOpenMenu() {
                showMainMenu();
            }

            @Override
            public void onRequestOpenQuickPanel() {
                showQuickPanel();
            }
        });
    }

    private void stopOverlayAndSelf() {
        isRunning = false;
        if (menuController != null) menuController.removeImmediate();
        if (quickPanelController != null) quickPanelController.removeImmediate();
        if (buttonController != null) buttonController.remove();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notif_channel_desc));
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startInForeground() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent contentPending = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openMenuIntent = new Intent(this, FloatingOverlayService.class);
        openMenuIntent.setAction(ACTION_OPEN_MENU);
        PendingIntent menuPending = PendingIntent.getService(
                this, 1, openMenuIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, FloatingOverlayService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orb_ring)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentPending)
                .addAction(0, getString(R.string.notif_action_toggle_menu), menuPending)
                .addAction(0, getString(R.string.notif_action_stop), stopPending)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }
}
