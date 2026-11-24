package com.example.touchflow.minimax.models;

public class AutomationLogEntry {
    public final long timestampMs;
    public final String packageName;
    public final String appLabel;
    public final String triggerLabel;
    public final String mechanism;
    public final boolean success;
    public final String detail;

    public AutomationLogEntry(long timestampMs,
                              String packageName,
                              String appLabel,
                              String triggerLabel,
                              String mechanism,
                              boolean success,
                              String detail) {
        this.timestampMs = timestampMs;
        this.packageName = packageName;
        this.appLabel = appLabel;
        this.triggerLabel = triggerLabel;
        this.mechanism = mechanism;
        this.success = success;
        this.detail = detail;
    }
}
