package com.example.touchflow.minimax.models;

import java.util.HashSet;
import java.util.Set;

public class MiniMaxConfig {
    public boolean autoMiniEnabled;
    public boolean floatingAssistantEnabled;
    public boolean autoMiniOnHome;
    public boolean autoMiniOnAppSwitch;
    public boolean onlySupportedAppsMode;
    public Set<String> supportedPackages = new HashSet<>();
    public Set<String> excludedPackages = new HashSet<>();
    public MiniWindowPosition windowPosition = MiniWindowPosition.TOP_RIGHT;
    public MiniWindowSize windowSize = MiniWindowSize.BALANCED;
}
