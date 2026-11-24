package com.example.touchflow.minimax.models;

import android.content.Context;

import androidx.annotation.StringRes;

import com.example.R;

public enum AutomationTriggerType {
    HOME_PRESS("Home Press", R.string.minimax_trigger_home),
    APP_SWITCH("App Switch", R.string.minimax_trigger_switch),
    MANUAL_LAUNCH("MiniMax Dock", R.string.minimax_trigger_manual),
    MINIMIZE("Minimize", R.string.minimax_trigger_minimize);

    private final String label;
    @StringRes
    private final int labelRes;

    AutomationTriggerType(String label, @StringRes int labelRes) {
        this.label = label;
        this.labelRes = labelRes;
    }

    public String getLabel() {
        return label;
    }

    public String getLabel(Context context) {
        return context.getString(labelRes);
    }
}
