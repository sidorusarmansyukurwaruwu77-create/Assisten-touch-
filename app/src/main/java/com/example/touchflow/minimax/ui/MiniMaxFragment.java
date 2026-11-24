package com.example.touchflow.minimax.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.touchflow.accessibility.TouchFlowAccessibilityService;
import com.example.touchflow.activities.MainActivity;
import com.example.touchflow.minimax.engine.AppCapabilityInspector;
import com.example.touchflow.minimax.engine.DeviceCompatibilityEngine;
import com.example.touchflow.minimax.engine.FloatingWindowAdapter;
import com.example.touchflow.minimax.engine.MiniMaxAutomationEngine;
import com.example.touchflow.minimax.models.AutomationLogEntry;
import com.example.touchflow.minimax.models.DeviceCompatibilityReport;
import com.example.touchflow.minimax.models.MiniMaxConfig;
import com.example.touchflow.minimax.models.MiniWindowPosition;
import com.example.touchflow.minimax.models.MiniWindowSize;
import com.example.touchflow.minimax.preferences.MiniMaxPreferences;
import com.example.touchflow.models.AppShortcut;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.services.FloatingOverlayService;
import com.example.touchflow.utilities.AppPackageHelper;
import com.example.touchflow.utilities.PermissionHelper;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;
import java.util.Set;

public class MiniMaxFragment extends Fragment {

    private MiniMaxPreferences miniMaxPrefs;
    private TouchFlowPreferences touchFlowPrefs;
    private MiniMaxHistoryAdapter historyAdapter;

    private TextView tvTierBadge;
    private View bannerPermissions;
    private TextView tvPermReason;
    private MaterialSwitch switchAutoMini;
    private MaterialSwitch switchFloatingAssistant;
    private MaterialSwitch switchAutoHome;
    private MaterialSwitch switchAutoSwitch;
    private MaterialSwitch switchOnlySupported;
    private TextView tvSupportedCount;
    private TextView tvExcludedCount;
    private TextView tvCompatDetails;
    private TextView tvLastApp;
    private TextView tvHistoryEmpty;
    private RecyclerView rvHistory;

    private boolean bindingUi = false;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
            (sp, key) -> {
                if (isAdded()) refreshState();
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_minimax, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        miniMaxPrefs = new MiniMaxPreferences(requireContext());
        touchFlowPrefs = new TouchFlowPreferences(requireContext());

        tvTierBadge = view.findViewById(R.id.tv_minimax_tier_badge);
        bannerPermissions = view.findViewById(R.id.banner_minimax_permissions);
        tvPermReason = view.findViewById(R.id.tv_minimax_perm_reason);
        switchAutoMini = view.findViewById(R.id.switch_auto_mini);
        switchFloatingAssistant = view.findViewById(R.id.switch_floating_assistant);
        switchAutoHome = view.findViewById(R.id.switch_auto_mini_home);
        switchAutoSwitch = view.findViewById(R.id.switch_auto_mini_switch);
        switchOnlySupported = view.findViewById(R.id.switch_only_supported);
        tvSupportedCount = view.findViewById(R.id.tv_supported_count);
        tvExcludedCount = view.findViewById(R.id.tv_excluded_count);
        tvCompatDetails = view.findViewById(R.id.tv_compat_details);
        tvLastApp = view.findViewById(R.id.tv_minimax_last_app);
        tvHistoryEmpty = view.findViewById(R.id.tv_minimax_history_empty);
        rvHistory = view.findViewById(R.id.rv_minimax_history);

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyAdapter = new MiniMaxHistoryAdapter(requireContext());
        rvHistory.setAdapter(historyAdapter);

        view.findViewById(R.id.btn_minimax_back).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        view.findViewById(R.id.btn_minimax_open_permissions).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_permissions);
            }
        });

        view.findViewById(R.id.btn_minimax_minimize_now).setOnClickListener(v -> {
            if (PermissionHelper.canDrawOverlays(requireContext()) && touchFlowPrefs.isServiceEnabled()) {
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

        setupSwitches();
        setupAppRulesRows(view);
        setupPositionAndSizeChips(view);
        setupCompatibilityAndHistoryButtons(view);

        miniMaxPrefs.registerListener(prefsListener);
        refreshState();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshState();
    }

    @Override
    public void onDestroyView() {
        if (miniMaxPrefs != null) {
            miniMaxPrefs.unregisterListener(prefsListener);
        }
        super.onDestroyView();
    }

    private void setupSwitches() {
        switchAutoMini.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            miniMaxPrefs.setAutoMiniEnabled(isChecked);
            if (isChecked && !PermissionHelper.isAccessibilityEnabled(requireContext())) {
                Toast.makeText(requireContext(),
                        R.string.minimax_need_access_toast,
                        Toast.LENGTH_LONG).show();
            }
        });

        switchFloatingAssistant.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingUi) return;
            miniMaxPrefs.setFloatingAssistantEnabled(isChecked);
            if (isChecked && touchFlowPrefs.isServiceEnabled() && PermissionHelper.canDrawOverlays(requireContext())) {
                FloatingOverlayService.openMiniMaxDockFromApp(requireContext());
            }
        });

        switchAutoHome.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!bindingUi) miniMaxPrefs.setAutoMiniOnHome(isChecked);
        });

        switchAutoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!bindingUi) miniMaxPrefs.setAutoMiniOnAppSwitch(isChecked);
        });

        switchOnlySupported.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!bindingUi) miniMaxPrefs.setOnlySupportedAppsMode(isChecked);
        });
    }

    private void setupAppRulesRows(View view) {
        view.findViewById(R.id.row_supported_apps).setOnClickListener(v ->
                showAppSelectionDialog(getString(R.string.minimax_supported_apps), true));

        view.findViewById(R.id.row_excluded_apps).setOnClickListener(v ->
                showAppSelectionDialog(getString(R.string.minimax_excluded_apps), false));
    }

    private void setupPositionAndSizeChips(View view) {
        MiniMaxConfig config = miniMaxPrefs.loadConfig();

        ChipGroup posGroup = view.findViewById(R.id.chips_window_position);
        switch (config.windowPosition) {
            case TOP_LEFT:
                posGroup.check(R.id.chip_pos_top_left);
                break;
            case CENTER:
                posGroup.check(R.id.chip_pos_center);
                break;
            case BOTTOM_LEFT:
                posGroup.check(R.id.chip_pos_bottom_left);
                break;
            case BOTTOM_RIGHT:
                posGroup.check(R.id.chip_pos_bottom_right);
                break;
            case TOP_RIGHT:
            default:
                posGroup.check(R.id.chip_pos_top_right);
                break;
        }
        posGroup.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            MiniWindowPosition pos = MiniWindowPosition.TOP_RIGHT;
            if (id == R.id.chip_pos_top_left) pos = MiniWindowPosition.TOP_LEFT;
            else if (id == R.id.chip_pos_center) pos = MiniWindowPosition.CENTER;
            else if (id == R.id.chip_pos_bottom_left) pos = MiniWindowPosition.BOTTOM_LEFT;
            else if (id == R.id.chip_pos_bottom_right) pos = MiniWindowPosition.BOTTOM_RIGHT;
            miniMaxPrefs.setWindowPosition(pos);
        });

        ChipGroup sizeGroup = view.findViewById(R.id.chips_window_size);
        switch (config.windowSize) {
            case COMPACT:
                sizeGroup.check(R.id.chip_size_compact);
                break;
            case LARGE:
                sizeGroup.check(R.id.chip_size_large);
                break;
            case BALANCED:
            default:
                sizeGroup.check(R.id.chip_size_balanced);
                break;
        }
        sizeGroup.setOnCheckedStateChangeListener((g, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            MiniWindowSize sz = MiniWindowSize.BALANCED;
            if (id == R.id.chip_size_compact) sz = MiniWindowSize.COMPACT;
            else if (id == R.id.chip_size_large) sz = MiniWindowSize.LARGE;
            miniMaxPrefs.setWindowSize(sz);
        });
    }

    private void setupCompatibilityAndHistoryButtons(View view) {
        view.findViewById(R.id.btn_compat_guide).setOnClickListener(v -> {
            DeviceCompatibilityReport report = DeviceCompatibilityEngine.inspect(requireContext());
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(report.oemSkin.getDisplayName())
                    .setMessage(report.oemSetupGuidance)
                    .setPositiveButton(R.string.minimax_dev_settings_btn, (d, w) -> openDeveloperOrSystemSettings())
                    .setNegativeButton(R.string.btn_close, null)
                    .show();
        });

        view.findViewById(R.id.btn_minimax_test).setOnClickListener(v -> {
            String target = MiniMaxAutomationEngine.getInstance(requireContext()).getLastActiveAppPackage();
            if (target == null || target.isEmpty()) {
                Set<String> supported = miniMaxPrefs.loadConfig().supportedPackages;
                for (String pkg : supported) {
                    if (AppCapabilityInspector.isLaunchableApp(requireContext(), pkg)) {
                        target = pkg;
                        break;
                    }
                }
            }
            FloatingWindowAdapter.LaunchResult result = MiniMaxAutomationEngine
                    .getInstance(requireContext())
                    .triggerManualMiniLaunch(target);
            Toast.makeText(requireContext(), result.detail, Toast.LENGTH_LONG).show();
            refreshState();
        });

        view.findViewById(R.id.btn_minimax_clear_history).setOnClickListener(v -> {
            miniMaxPrefs.clearHistory();
            refreshState();
        });
    }

    private void openDeveloperOrSystemSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
            }
        }
    }

    private void showAppSelectionDialog(String title, boolean isSupportedList) {
        List<AppShortcut> installed = AppPackageHelper.queryLaunchableApps(requireContext());
        MiniMaxConfig config = miniMaxPrefs.loadConfig();
        Set<String> initial = isSupportedList ? config.supportedPackages : config.excludedPackages;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_app_picker, null);
        EditText etSearch = dialogView.findViewById(R.id.et_app_search);
        RecyclerView rvApps = dialogView.findViewById(R.id.rv_installed_apps);
        rvApps.setLayoutManager(new LinearLayoutManager(requireContext()));

        MiniMaxAppSelectAdapter adapter = new MiniMaxAppSelectAdapter(requireContext(), installed, initial);
        rvApps.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, (d, w) -> {
                    Set<String> updated = adapter.getSelectedPackages();
                    if (isSupportedList) {
                        miniMaxPrefs.setSupportedPackages(updated);
                    } else {
                        miniMaxPrefs.setExcludedPackages(updated);
                    }
                    refreshState();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void refreshState() {
        if (!isAdded()) return;
        bindingUi = true;

        MiniMaxConfig config = miniMaxPrefs.loadConfig();
        switchAutoMini.setChecked(config.autoMiniEnabled);
        switchFloatingAssistant.setChecked(config.floatingAssistantEnabled);
        switchAutoHome.setChecked(config.autoMiniOnHome);
        switchAutoSwitch.setChecked(config.autoMiniOnAppSwitch);
        switchOnlySupported.setChecked(config.onlySupportedAppsMode);

        tvSupportedCount.setText(getString(R.string.minimax_apps_count_format, config.supportedPackages.size()));
        tvExcludedCount.setText(getString(R.string.minimax_apps_count_format, config.excludedPackages.size()));

        // Permissions notice (non-intrusive banner, never spams dialogs)
        boolean overlayOk = PermissionHelper.canDrawOverlays(requireContext());
        boolean accessOk = PermissionHelper.isAccessibilityEnabled(requireContext());
        if (!overlayOk || !accessOk) {
            bannerPermissions.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            if (!overlayOk) {
                sb.append(getString(R.string.minimax_perm_overlay_missing)).append("\n");
            }
            if (!accessOk) {
                sb.append(getString(R.string.minimax_perm_access_missing));
            }
            tvPermReason.setText(sb.toString().trim());
        } else {
            bannerPermissions.setVisibility(View.GONE);
        }

        // Device Compatibility
        DeviceCompatibilityReport report = DeviceCompatibilityEngine.inspect(requireContext());
        tvTierBadge.setText(report.capabilityTierLabel);
        tvTierBadge.setTextColor(ContextCompat.getColor(
                requireContext(),
                report.canAttemptNativeFreeform() ? R.color.tf_success : R.color.tf_secondary
        ));

        tvCompatDetails.setText(
                "• " + report.manufacturer + " " + report.model + "\n"
                        + "• OS: " + report.oemSkin.getDisplayName() + "\n"
                        + "• Android " + report.androidVersion + " (API " + report.sdkInt + ")\n"
                        + "• Freeform: " + getString(report.hasFreeformSystemFeature ? R.string.badge_allowed : R.string.status_unavailable)
                        + " • Dev Flag: " + getString(report.isDeveloperFreeformEnabled ? R.string.status_on : R.string.status_off) + "\n"
                        + "• PiP: " + getString(report.supportsPictureInPictureFeature ? R.string.badge_allowed : R.string.status_unavailable)
                        + "\n\n" + report.summaryText
        );

        // Last active app
        String lastPkg = MiniMaxAutomationEngine.getInstance(requireContext()).getLastActiveAppPackage();
        if (lastPkg != null && !lastPkg.isEmpty()) {
            tvLastApp.setText(getString(
                    R.string.minimax_last_app_format,
                    AppCapabilityInspector.getAppLabel(requireContext(), lastPkg),
                    lastPkg
            ));
        } else {
            tvLastApp.setText(R.string.minimax_last_app_none);
        }

        // History
        List<AutomationLogEntry> history = miniMaxPrefs.getHistory();
        historyAdapter.submitList(history);
        tvHistoryEmpty.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(history.isEmpty() ? View.GONE : View.VISIBLE);

        bindingUi = false;
    }
}
