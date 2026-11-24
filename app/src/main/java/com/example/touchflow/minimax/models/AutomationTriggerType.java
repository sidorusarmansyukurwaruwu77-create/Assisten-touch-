package com.example.touchflow.minimax.models;

public enum AutomationTriggerType {
    HOME_PRESS("Home Press"),
    APP_SWITCH("App Switch"),
    MANUAL_LAUNCH("MiniMax Dock");

    private final String label;

    AutomationTriggerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
