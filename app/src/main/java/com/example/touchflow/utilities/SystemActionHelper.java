package com.example.touchflow.utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.R;
import com.example.touchflow.accessibility.TouchFlowAccessibilityService;
import com.example.touchflow.models.ActionItem;

public class SystemActionHelper {

    public interface OverlayNavigationCallback {
        void onRequestCloseMenu();
        void onRequestOpenMenu();
        void onRequestOpenQuickPanel();
        default void onRequestOpenMiniMax() {}
    }

    private final Context appContext;
    private final AudioManager audioManager;
    private final CameraManager cameraManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String torchCameraId;
    private boolean isTorchOn = false;
    private CameraManager.TorchCallback torchCallback;

    public interface TorchStateListener {
        void onTorchStateChanged(boolean enabled);
    }

    public SystemActionHelper(Context context) {
        this.appContext = context.getApplicationContext();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        this.cameraManager = (CameraManager) appContext.getSystemService(Context.CAMERA_SERVICE);
        resolveTorchCamera();
    }

    private void resolveTorchCamera() {
        if (cameraManager == null) return;
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = chars.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(hasFlash)
                        && (lensFacing == null || lensFacing == CameraCharacteristics.LENS_FACING_BACK)) {
                    torchCameraId = id;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void registerTorchListener(TorchStateListener listener) {
        if (cameraManager == null || torchCallback != null) return;
        torchCallback = new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                if (cameraId.equals(torchCameraId)) {
                    isTorchOn = enabled;
                    if (listener != null) {
                        listener.onTorchStateChanged(enabled);
                    }
                }
            }
        };
        try {
            cameraManager.registerTorchCallback(torchCallback, mainHandler);
        } catch (Exception ignored) {
        }
    }

    public void unregisterTorchListener() {
        if (cameraManager != null && torchCallback != null) {
            try {
                cameraManager.unregisterTorchCallback(torchCallback);
            } catch (Exception ignored) {
            }
            torchCallback = null;
        }
    }

    public boolean isTorchOn() {
        return isTorchOn;
    }

    public void performHaptic(boolean enabled) {
        if (!enabled) return;
        try {
            Vibrator vibrator = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vibrator = vm.getDefaultVibrator();
                }
            } else {
                vibrator = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {
        }
    }

    public void executeAction(ActionItem action, OverlayNavigationCallback callback) {
        if (action == null || action == ActionItem.NONE) return;

        switch (action) {
            case OPEN_MENU:
                if (callback != null) callback.onRequestOpenMenu();
                break;
            case QUICK_PANEL:
                if (callback != null) callback.onRequestOpenQuickPanel();
                break;
            case MINIMAX_ASSISTANT:
                if (callback != null) {
                    callback.onRequestOpenMiniMax();
                } else {
                    com.example.touchflow.services.FloatingOverlayService.openMiniMaxDockFromApp(appContext);
                }
                break;
            case HOME:
                if (callback != null) callback.onRequestCloseMenu();
                if (!TouchFlowAccessibilityService.performHome()) {
                    promptAccessibility(action);
                }
                break;
            case BACK:
                if (callback != null) callback.onRequestCloseMenu();
                if (!TouchFlowAccessibilityService.performBack()) {
                    promptAccessibility(action);
                }
                break;
            case RECENTS:
                if (callback != null) callback.onRequestCloseMenu();
                if (!TouchFlowAccessibilityService.performRecents()) {
                    promptAccessibility(action);
                }
                break;
            case SCREENSHOT:
                if (callback != null) callback.onRequestCloseMenu();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    showToast(appContext.getString(R.string.msg_screenshot_api_unsupported));
                } else {
                    mainHandler.postDelayed(() -> {
                        if (!TouchFlowAccessibilityService.performScreenshot()) {
                            promptAccessibility(action);
                        }
                    }, 220L);
                }
                break;
            case LOCK_SCREEN:
                if (callback != null) callback.onRequestCloseMenu();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    showToast(appContext.getString(R.string.msg_lock_api_unsupported));
                } else if (!TouchFlowAccessibilityService.performLockScreen()) {
                    promptAccessibility(action);
                }
                break;
            case NOTIFICATIONS:
                if (callback != null) callback.onRequestCloseMenu();
                if (!TouchFlowAccessibilityService.performNotifications()) {
                    promptAccessibility(action);
                }
                break;
            case QUICK_SETTINGS:
                if (callback != null) callback.onRequestCloseMenu();
                if (!TouchFlowAccessibilityService.performQuickSettings()) {
                    promptAccessibility(action);
                }
                break;
            case POWER_MENU:
                if (callback != null) callback.onRequestCloseMenu();
                if (!TouchFlowAccessibilityService.performPowerDialog()) {
                    promptAccessibility(action);
                }
                break;
            case VOLUME_UP:
                adjustVolume(AudioManager.ADJUST_RAISE);
                break;
            case VOLUME_DOWN:
                adjustVolume(AudioManager.ADJUST_LOWER);
                break;
            case BRIGHTNESS:
                if (callback != null) {
                    callback.onRequestOpenQuickPanel();
                } else {
                    openDisplaySettings();
                }
                break;
            case WIFI:
                if (callback != null) callback.onRequestCloseMenu();
                openWifiSettings();
                break;
            case BLUETOOTH:
                if (callback != null) callback.onRequestCloseMenu();
                openBluetoothSettings();
                break;
            case FLASHLIGHT:
                toggleFlashlight();
                break;
            case ROTATION:
                toggleAutoRotation();
                break;
            case OPEN_SETTINGS:
                if (callback != null) callback.onRequestCloseMenu();
                openSystemSettings();
                break;
        }
    }

    private void promptAccessibility(ActionItem action) {
        String label = appContext.getString(action.getTitleRes());
        showToast(appContext.getString(R.string.msg_accessibility_required, label));
        try {
            appContext.startActivity(PermissionHelper.buildAccessibilitySettingsIntent());
        } catch (Exception ignored) {
        }
    }

    public void adjustVolume(int direction) {
        if (audioManager == null) return;
        audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
        );
    }

    public int getMusicVolume() {
        if (audioManager == null) return 0;
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public int getMusicMaxVolume() {
        if (audioManager == null) return 15;
        return Math.max(1, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    }

    public void setMusicVolume(int volume) {
        if (audioManager == null) return;
        int clamped = Math.max(0, Math.min(getMusicMaxVolume(), volume));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0);
    }

    public int getScreenBrightness() {
        try {
            return Settings.System.getInt(
                    appContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS
            );
        } catch (Settings.SettingNotFoundException e) {
            return 128;
        }
    }

    public boolean setScreenBrightness(int value) {
        if (!PermissionHelper.canWriteSettings(appContext)) {
            showToast(appContext.getString(R.string.msg_write_settings_required, appContext.getString(R.string.action_brightness)));
            try {
                appContext.startActivity(PermissionHelper.buildWriteSettingsIntent(appContext));
            } catch (Exception ignored) {
            }
            return false;
        }
        int clamped = Math.max(5, Math.min(255, value));
        try {
            Settings.System.putInt(
                    appContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );
            Settings.System.putInt(
                    appContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    clamped
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean toggleFlashlight() {
        if (cameraManager == null || torchCameraId == null
                || !appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            showToast(appContext.getString(R.string.msg_flashlight_unavailable));
            return false;
        }
        try {
            boolean next = !isTorchOn;
            cameraManager.setTorchMode(torchCameraId, next);
            isTorchOn = next;
            return true;
        } catch (Exception e) {
            showToast(appContext.getString(R.string.msg_flashlight_unavailable));
            return false;
        }
    }

    public void toggleAutoRotation() {
        if (!PermissionHelper.canWriteSettings(appContext)) {
            showToast(appContext.getString(R.string.msg_write_settings_required, appContext.getString(R.string.action_rotation)));
            try {
                appContext.startActivity(PermissionHelper.buildWriteSettingsIntent(appContext));
            } catch (Exception ignored) {
            }
            return;
        }
        try {
            int current = Settings.System.getInt(
                    appContext.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
            );
            int next = (current == 1) ? 0 : 1;
            Settings.System.putInt(
                    appContext.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    next
            );
            showToast(appContext.getString(R.string.msg_rotation_toggled, next == 1 ? "ON" : "OFF"));
        } catch (Exception e) {
            openDisplaySettings();
        }
    }

    public void openWifiSettings() {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                intent = new Intent(Settings.Panel.ACTION_WIFI);
            } else {
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        } catch (Exception e) {
            openSystemSettings();
        }
    }

    public void openBluetoothSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        } catch (Exception e) {
            openSystemSettings();
        }
    }

    public void openDisplaySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        } catch (Exception e) {
            openSystemSettings();
        }
    }

    public void openSystemSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    public String getWifiStatusLabel() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return "Connected";
                    }
                }
            }
            WifiManager wifiManager = (WifiManager) appContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                return "On";
            }
            return "Off";
        } catch (Exception e) {
            return "Settings";
        }
    }

    public String getBluetoothStatusLabel() {
        if (!PermissionHelper.hasBluetoothConnectPermission(appContext)) {
            return "Tap to view";
        }
        try {
            BluetoothManager bm = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter adapter = bm != null ? bm.getAdapter() : BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) return "Unavailable";
            return adapter.isEnabled() ? "On" : "Off";
        } catch (SecurityException e) {
            return "Tap to view";
        } catch (Exception e) {
            return "Settings";
        }
    }

    public BatteryInfo getBatteryInfo() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = appContext.registerReceiver(null, filter);
        int level = -1;
        boolean charging = false;
        if (batteryStatus != null) {
            int rawLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (rawLevel >= 0 && scale > 0) {
                level = Math.round((rawLevel * 100f) / scale);
            }
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        }
        if (level < 0) {
            BatteryManager bm = (BatteryManager) appContext.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                charging = bm.isCharging();
            }
        }
        return new BatteryInfo(Math.max(0, Math.min(100, level)), charging);
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show());
    }

    public static class BatteryInfo {
        public final int percent;
        public final boolean charging;

        public BatteryInfo(int percent, boolean charging) {
            this.percent = percent;
            this.charging = charging;
        }
    }
}
