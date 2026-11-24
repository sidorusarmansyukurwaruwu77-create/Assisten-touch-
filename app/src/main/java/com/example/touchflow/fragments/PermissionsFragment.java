package com.example.touchflow.fragments;

import android.Manifest;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.touchflow.services.FloatingOverlayService;
import com.example.touchflow.utilities.PermissionHelper;
import com.google.android.material.button.MaterialButton;

public class PermissionsFragment extends Fragment {

    private LinearLayout containerList;

    private final ActivityResultLauncher<String> runtimePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> renderPermissions());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        containerList = view.findViewById(R.id.permissions_list_container);
        renderPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        renderPermissions();
    }

    private void renderPermissions() {
        if (!isAdded() || containerList == null) return;
        containerList.removeAllViews();

        // 1. Display over other apps
        boolean overlayGranted = PermissionHelper.canDrawOverlays(requireContext());
        addPermissionCard(
                getString(R.string.perm_overlay_title),
                getString(R.string.perm_overlay_desc),
                overlayGranted,
                getString(overlayGranted ? R.string.badge_granted : R.string.badge_required),
                getString(overlayGranted ? R.string.perm_overlay_btn_open : R.string.perm_overlay_btn_grant),
                v -> startActivity(PermissionHelper.buildOverlayPermissionIntent(requireContext()))
        );

        // 2. Accessibility Service
        boolean accessGranted = PermissionHelper.isAccessibilityEnabled(requireContext());
        addPermissionCard(
                getString(R.string.perm_access_title),
                getString(R.string.perm_access_desc),
                accessGranted,
                getString(accessGranted ? R.string.badge_enabled : R.string.badge_required_nav),
                getString(accessGranted ? R.string.perm_access_btn_open : R.string.perm_access_btn_grant),
                v -> startActivity(PermissionHelper.buildAccessibilitySettingsIntent())
        );

        // 3. Notifications (Android 13+)
        boolean notifGranted = PermissionHelper.hasNotificationPermission(requireContext());
        addPermissionCard(
                getString(R.string.perm_notif_title),
                getString(R.string.perm_notif_desc),
                notifGranted,
                getString(notifGranted ? R.string.badge_allowed : R.string.badge_recommended),
                getString(notifGranted ? R.string.perm_notif_btn_open : R.string.perm_notif_btn_grant),
                v -> {
                    if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runtimePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        startActivity(PermissionHelper.buildAppNotificationSettingsIntent(requireContext()));
                    }
                }
        );

        // 4. Modify System Settings
        boolean writeGranted = PermissionHelper.canWriteSettings(requireContext());
        addPermissionCard(
                getString(R.string.perm_write_title),
                getString(R.string.perm_write_desc),
                writeGranted,
                getString(writeGranted ? R.string.badge_allowed : R.string.badge_optional),
                getString(R.string.perm_write_btn),
                v -> startActivity(PermissionHelper.buildWriteSettingsIntent(requireContext()))
        );

        // 5. Bluetooth Status (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean btGranted = PermissionHelper.hasBluetoothConnectPermission(requireContext());
            addPermissionCard(
                    getString(R.string.perm_bt_title),
                    getString(R.string.perm_bt_desc),
                    btGranted,
                    getString(btGranted ? R.string.badge_allowed : R.string.badge_optional),
                    getString(btGranted ? R.string.perm_bt_btn_ok : R.string.perm_bt_btn_grant),
                    v -> {
                        if (!btGranted) {
                            runtimePermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                        }
                    }
            );
        }

        // 6. Battery Optimization / Keep Active When App Closed
        boolean batteryUnrestricted = PermissionHelper.isIgnoringBatteryOptimizations(requireContext());
        addPermissionCard(
                getString(R.string.perm_battery_title),
                getString(R.string.perm_battery_desc),
                batteryUnrestricted,
                getString(batteryUnrestricted ? R.string.perm_battery_unrestricted : R.string.perm_battery_optimized),
                getString(R.string.perm_battery_btn),
                v -> startActivity(PermissionHelper.buildBatteryOptimizationSettingsIntent())
        );

        // 7. Foreground Service State
        boolean fgsRunning = FloatingOverlayService.isServiceRunning();
        addPermissionCard(
                getString(R.string.perm_fgs_title),
                getString(R.string.perm_fgs_desc),
                fgsRunning,
                getString(fgsRunning ? R.string.badge_running : R.string.badge_standby),
                null,
                null
        );
    }

    private void addPermissionCard(String title, String description, boolean granted,
                                   String badgeText, @Nullable String buttonLabel,
                                   @Nullable View.OnClickListener clickListener) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_permission_card, containerList, false);

        ImageView ivStatus = card.findViewById(R.id.iv_perm_status);
        TextView tvTitle = card.findViewById(R.id.tv_perm_title);
        TextView tvBadge = card.findViewById(R.id.tv_perm_badge);
        TextView tvDesc = card.findViewById(R.id.tv_perm_desc);
        MaterialButton btnAction = card.findViewById(R.id.btn_perm_action);

        int statusColor = ContextCompat.getColor(
                requireContext(),
                granted ? R.color.tf_success : R.color.tf_warning
        );

        ivStatus.setImageResource(granted ? R.drawable.ic_check_circle : R.drawable.ic_warning);
        ImageViewCompat.setImageTintList(ivStatus, ColorStateList.valueOf(statusColor));

        tvTitle.setText(title);
        tvBadge.setText(badgeText);
        tvBadge.setTextColor(statusColor);
        tvDesc.setText(description);

        if (buttonLabel != null && clickListener != null) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText(buttonLabel);
            btnAction.setOnClickListener(clickListener);
        } else {
            btnAction.setVisibility(View.GONE);
        }

        containerList.addView(card);
    }
}
