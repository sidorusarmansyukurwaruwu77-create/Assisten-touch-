package com.example.touchflow.fragments;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.touchflow.accessibility.TouchFlowAccessibilityService;
import com.example.touchflow.activities.MainActivity;
import com.example.touchflow.minimax.engine.DeviceCompatibilityEngine;
import com.example.touchflow.minimax.models.DeviceCompatibilityReport;
import com.example.touchflow.minimax.models.MiniMaxConfig;
import com.example.touchflow.minimax.preferences.MiniMaxPreferences;
import com.example.touchflow.models.TouchFlowConfig;
import com.example.touchflow.overlay.OverlayThemeApplier;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.services.FloatingOverlayService;
import com.example.touchflow.utilities.LocaleHelper;
import com.example.touchflow.utilities.PermissionHelper;
import com.example.touchflow.utilities.SystemActionHelper;
import com.example.touchflow.utilities.ViewAnimationHelper;
import com.google.android.material.button.MaterialButton;

public class DashboardFragment extends Fragment {

    private TouchFlowPreferences preferences;
    private MiniMaxPreferences miniMaxPreferences;
    private SystemActionHelper systemActionHelper;

    private TextView tvStatusBadge;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private MaterialButton btnToggleService;
    private MaterialButton chipLangId;
    private MaterialButton chipLangEn;
    private FrameLayout previewOrbContainer;
    private ImageView previewOrbIcon;
    private TextView tvPermPill;
    private TextView tvPermDetail;
    private TextView tvConfigSummary;
    private TextView tvDashMiniMaxStatus;
    private TextView tvDashMiniMaxPill;
    private ImageView ivDashFlashlight;
    private TextView tvDashFlashlight;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
            (sp, key) -> {
                if (isAdded()) refreshUi();
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = new TouchFlowPreferences(requireContext());
        miniMaxPreferences = new MiniMaxPreferences(requireContext());
        systemActionHelper = new SystemActionHelper(requireContext());
        systemActionHelper.registerTorchListener(enabled -> {
            if (isAdded()) updateFlashlightTile(enabled);
        });

        tvStatusBadge = view.findViewById(R.id.tv_status_badge);
        tvStatusTitle = view.findViewById(R.id.tv_status_title);
        tvStatusDesc = view.findViewById(R.id.tv_status_desc);
        btnToggleService = view.findViewById(R.id.btn_toggle_service);
        chipLangId = view.findViewById(R.id.chip_lang_id);
        chipLangEn = view.findViewById(R.id.chip_lang_en);
        previewOrbContainer = view.findViewById(R.id.preview_orb_container);
        previewOrbIcon = view.findViewById(R.id.preview_orb_icon);
        tvPermPill = view.findViewById(R.id.tv_perm_summary_pill);
        tvPermDetail = view.findViewById(R.id.tv_perm_detail_line);
        tvConfigSummary = view.findViewById(R.id.tv_config_summary_body);
        tvDashMiniMaxStatus = view.findViewById(R.id.tv_dash_minimax_status);
        tvDashMiniMaxPill = view.findViewById(R.id.tv_dash_minimax_pill);
        ivDashFlashlight = view.findViewById(R.id.iv_dash_flashlight);
        tvDashFlashlight = view.findViewById(R.id.tv_dash_flashlight);

        previewOrbContainer.setOnClickListener(v -> {
            ViewAnimationHelper.animateSpringScale(previewOrbContainer, 0.86f);
            previewOrbContainer.postDelayed(() -> ViewAnimationHelper.animateSpringScale(previewOrbContainer, 1.0f), 110L);
        });

        btnToggleService.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleTouchFlowService();
                v.postDelayed(this::refreshUi, 220L);
            }
        });

        view.findViewById(R.id.card_quick_open_menu).setOnClickListener(v -> {
            if (PermissionHelper.canDrawOverlays(requireContext()) && preferences.isServiceEnabled()) {
                android.content.Intent i = new android.content.Intent(requireContext(), FloatingOverlayService.class);
                i.setAction(FloatingOverlayService.ACTION_OPEN_MENU);
                requireContext().startService(i);
            } else {
                Toast.makeText(requireContext(), R.string.msg_enable_touchflow_menu, Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.card_quick_open_panel).setOnClickListener(v -> {
            if (PermissionHelper.canDrawOverlays(requireContext()) && preferences.isServiceEnabled()) {
                FloatingOverlayService.openQuickPanelFromApp(requireContext());
            } else {
                Toast.makeText(requireContext(), R.string.msg_enable_touchflow_panel, Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.card_quick_minimize).setOnClickListener(v -> {
            if (PermissionHelper.canDrawOverlays(requireContext()) && preferences.isServiceEnabled()) {
                if (PermissionHelper.isAccessibilityEnabled(requireContext())) {
                    FloatingOverlayService.minimizeFromApp(requireContext());
                } else {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.msg_accessibility_required, getString(R.string.action_minimize)),
                            Toast.LENGTH_SHORT
                    ).show();
                    startActivity(PermissionHelper.buildAccessibilitySettingsIntent());
                }
            } else if (!TouchFlowAccessibilityService.performMinimize()) {
                Toast.makeText(requireContext(), R.string.msg_enable_touchflow_minimize, Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.card_quick_flashlight).setOnClickListener(v -> {
            systemActionHelper.toggleFlashlight();
            v.postDelayed(() -> {
                if (isAdded()) updateFlashlightTile(systemActionHelper.isTorchOn());
            }, 80L);
        });

        chipLangId.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).applyLanguageSelection(LocaleHelper.LANG_INDONESIAN);
            }
        });

        chipLangEn.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).applyLanguageSelection(LocaleHelper.LANG_ENGLISH);
            }
        });

        view.findViewById(R.id.card_dash_permissions).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_permissions);
            }
        });

        view.findViewById(R.id.card_dash_config).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_appearance);
            }
        });

        view.findViewById(R.id.card_dash_minimax).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openMiniMaxPage();
            }
        });

        preferences.registerListener(prefsListener);
        miniMaxPreferences.registerListener(prefsListener);
        refreshUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    @Override
    public void onDestroyView() {
        if (preferences != null) {
            preferences.unregisterListener(prefsListener);
        }
        if (miniMaxPreferences != null) {
            miniMaxPreferences.unregisterListener(prefsListener);
        }
        if (systemActionHelper != null) {
            systemActionHelper.unregisterTorchListener();
        }
        super.onDestroyView();
    }

    private void refreshUi() {
        if (!isAdded()) return;
        TouchFlowConfig config = preferences.loadConfig();
        boolean active = config.serviceEnabled && PermissionHelper.canDrawOverlays(requireContext());

        if (active) {
            tvStatusBadge.setText(R.string.badge_active);
            tvStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.tf_success));
            tvStatusTitle.setText(R.string.dashboard_status_active);
            tvStatusDesc.setText(R.string.dashboard_status_desc_active);
            btnToggleService.setText(R.string.btn_disable_touchflow);
            btnToggleService.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.tf_surface_elevated)));
        } else {
            tvStatusBadge.setText(R.string.badge_standby);
            tvStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.tf_text_secondary));
            tvStatusTitle.setText(R.string.dashboard_status_inactive);
            tvStatusDesc.setText(R.string.dashboard_status_desc_inactive);
            btnToggleService.setText(R.string.btn_enable_touchflow);
            btnToggleService.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.tf_primary)));
        }

        // Language chips
        boolean isId = LocaleHelper.isIndonesian(requireContext());
        int activeTint = ContextCompat.getColor(requireContext(), R.color.tf_primary);
        int idleTint = ContextCompat.getColor(requireContext(), R.color.tf_surface_elevated);
        int primaryText = ContextCompat.getColor(requireContext(), R.color.tf_text_primary);
        int secondaryText = ContextCompat.getColor(requireContext(), R.color.tf_text_secondary);

        chipLangId.setBackgroundTintList(ColorStateList.valueOf(isId ? activeTint : idleTint));
        chipLangId.setTextColor(isId ? primaryText : secondaryText);
        chipLangEn.setBackgroundTintList(ColorStateList.valueOf(!isId ? activeTint : idleTint));
        chipLangEn.setTextColor(!isId ? primaryText : secondaryText);

        // Update Live Floating Button Preview
        float density = getResources().getDisplayMetrics().density;
        int previewPx = Math.round(config.buttonSizeDp * density);
        FrameLayout.LayoutParams orbLp = (FrameLayout.LayoutParams) previewOrbContainer.getLayoutParams();
        orbLp.width = previewPx;
        orbLp.height = previewPx;
        previewOrbContainer.setLayoutParams(orbLp);

        int iconPx = Math.round(previewPx * 0.52f);
        FrameLayout.LayoutParams iconLp = (FrameLayout.LayoutParams) previewOrbIcon.getLayoutParams();
        iconLp.width = iconPx;
        iconLp.height = iconPx;
        previewOrbIcon.setLayoutParams(iconLp);

        previewOrbIcon.setImageResource(config.orbIconStyle.getDrawableRes());
        ImageViewCompat.setImageTintList(previewOrbIcon, ColorStateList.valueOf(config.iconColor));
        OverlayThemeApplier.applyFloatingOrbStyle(requireContext(), previewOrbContainer, config);
        previewOrbContainer.setAlpha(config.opacityPercent / 100f);

        // Permissions Summary
        boolean overlayOk = PermissionHelper.canDrawOverlays(requireContext());
        boolean accessOk = PermissionHelper.isAccessibilityEnabled(requireContext());
        boolean notifOk = PermissionHelper.hasNotificationPermission(requireContext());
        boolean writeOk = PermissionHelper.canWriteSettings(requireContext());
        int grantedCount = (overlayOk ? 1 : 0) + (accessOk ? 1 : 0) + (notifOk ? 1 : 0) + (writeOk ? 1 : 0);

        tvPermPill.setText(getString(R.string.dash_perm_ready_format, grantedCount, 4));
        tvPermPill.setTextColor(ContextCompat.getColor(
                requireContext(),
                grantedCount == 4 ? R.color.tf_success : R.color.tf_warning
        ));
        tvPermDetail.setText(getString(
                R.string.dash_perm_detail_template,
                getString(overlayOk ? R.string.dash_perm_granted : R.string.dash_perm_required),
                getString(accessOk ? R.string.dash_perm_enabled : R.string.dash_perm_needed_nav),
                getString(notifOk ? R.string.dash_perm_allowed : R.string.dash_perm_recommended),
                getString(writeOk ? R.string.dash_perm_allowed : R.string.dash_perm_optional_settings)
        ));

        // Config Summary
        int shortcutsCount = preferences.getShortcuts().size();
        tvConfigSummary.setText(getString(
                R.string.dash_config_summary_template,
                config.buttonSizeDp,
                config.opacityPercent,
                config.orbIconStyle.getDisplayName(),
                config.overlayTheme.getDisplayName(requireContext()),
                config.menuLayout.getDisplayName(requireContext()),
                getString(config.singleTapAction.getTitleRes()),
                getString(config.doubleTapAction.getTitleRes()),
                shortcutsCount,
                TouchFlowPreferences.MAX_SHORTCUTS
        ));

        // MiniMax Summary
        MiniMaxConfig mmConfig = miniMaxPreferences.loadConfig();
        DeviceCompatibilityReport report = DeviceCompatibilityEngine.inspect(requireContext());
        tvDashMiniMaxStatus.setText(
                getString(R.string.minimax_auto_mini) + ": "
                        + getString(mmConfig.autoMiniEnabled ? R.string.status_on : R.string.status_off)
                        + " • " + report.oemSkin.getDisplayName()
                        + " (" + report.capabilityTierLabel + ")"
        );
        tvDashMiniMaxPill.setText(mmConfig.autoMiniEnabled
                ? getString(R.string.status_on)
                : getString(R.string.dash_minimax_open));
        tvDashMiniMaxPill.setTextColor(ContextCompat.getColor(
                requireContext(),
                mmConfig.autoMiniEnabled ? R.color.tf_success : R.color.tf_secondary
        ));

        updateFlashlightTile(systemActionHelper.isTorchOn());
    }

    private void updateFlashlightTile(boolean on) {
        if (!isAdded()) return;
        int color = ContextCompat.getColor(requireContext(), on ? R.color.tf_secondary : R.color.tf_text_primary);
        ImageViewCompat.setImageTintList(ivDashFlashlight, ColorStateList.valueOf(color));
        tvDashFlashlight.setText(on ? getString(R.string.dash_torch_on) : getString(R.string.action_flashlight));
        tvDashFlashlight.setTextColor(color);
    }
}
