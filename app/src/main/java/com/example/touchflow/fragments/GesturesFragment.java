package com.example.touchflow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.touchflow.models.ActionItem;
import com.example.touchflow.models.TouchFlowConfig;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class GesturesFragment extends Fragment {

    private TouchFlowPreferences preferences;

    private TextView tvSingleTap;
    private TextView tvDoubleTap;
    private TextView tvLongPress;
    private TextView tvSwipe;

    private MaterialSwitch switchHaptic;
    private MaterialSwitch switchEdgeSnap;
    private MaterialSwitch switchIdleDim;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gestures, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = new TouchFlowPreferences(requireContext());

        tvSingleTap = view.findViewById(R.id.tv_single_tap_value);
        tvDoubleTap = view.findViewById(R.id.tv_double_tap_value);
        tvLongPress = view.findViewById(R.id.tv_long_press_value);
        tvSwipe = view.findViewById(R.id.tv_swipe_value);

        switchHaptic = view.findViewById(R.id.switch_haptic);
        switchEdgeSnap = view.findViewById(R.id.switch_edge_snap);
        switchIdleDim = view.findViewById(R.id.switch_idle_dim);

        view.findViewById(R.id.row_single_tap).setOnClickListener(v ->
                showActionPicker(getString(R.string.gesture_single_tap), preferences.loadConfig().singleTapAction, chosen -> {
                    preferences.setSingleTapAction(chosen);
                    refreshValues();
                }));

        view.findViewById(R.id.row_double_tap).setOnClickListener(v ->
                showActionPicker(getString(R.string.gesture_double_tap), preferences.loadConfig().doubleTapAction, chosen -> {
                    preferences.setDoubleTapAction(chosen);
                    refreshValues();
                }));

        view.findViewById(R.id.row_long_press).setOnClickListener(v ->
                showActionPicker(getString(R.string.gesture_long_press), preferences.loadConfig().longPressAction, chosen -> {
                    preferences.setLongPressAction(chosen);
                    refreshValues();
                }));

        view.findViewById(R.id.row_swipe_gesture).setOnClickListener(v ->
                showActionPicker(getString(R.string.gesture_swipe), preferences.loadConfig().swipeAction, chosen -> {
                    preferences.setSwipeAction(chosen);
                    refreshValues();
                }));

        refreshValues();

        switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferences.setHapticEnabled(isChecked));
        switchEdgeSnap.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferences.setEdgeSnapEnabled(isChecked));
        switchIdleDim.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferences.setIdleDimEnabled(isChecked));
    }

    private void refreshValues() {
        TouchFlowConfig config = preferences.loadConfig();
        tvSingleTap.setText(config.singleTapAction.getTitleRes());
        tvDoubleTap.setText(config.doubleTapAction.getTitleRes());
        tvLongPress.setText(config.longPressAction.getTitleRes());
        tvSwipe.setText(config.swipeAction.getTitleRes());

        switchHaptic.setChecked(config.hapticEnabled);
        switchEdgeSnap.setChecked(config.edgeSnapEnabled);
        switchIdleDim.setChecked(config.idleDimEnabled);
    }

    private interface ActionPickedCallback {
        void onPicked(ActionItem action);
    }

    private void showActionPicker(String title, ActionItem current, ActionPickedCallback callback) {
        List<ActionItem> actions = ActionItem.getGestureAssignableActions();
        String[] labels = new String[actions.size()];
        int checkedIdx = 0;
        for (int i = 0; i < actions.size(); i++) {
            ActionItem item = actions.get(i);
            labels[i] = getString(item.getTitleRes());
            if (item == current) {
                checkedIdx = i;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(labels, checkedIdx, (dialog, which) -> {
                    callback.onPicked(actions.get(which));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
