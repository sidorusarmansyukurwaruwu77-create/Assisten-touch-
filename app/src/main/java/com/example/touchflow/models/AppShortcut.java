package com.example.touchflow.models;

public class AppShortcut {
    private final String packageName;
    private final String label;

    public AppShortcut(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getLabel() {
        return label;
    }
}
