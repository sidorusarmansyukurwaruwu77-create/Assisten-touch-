package com.example.touchflow.minimax.models;

public class DeviceCompatibilityReport {
    public final String manufacturer;
    public final String model;
    public final String brand;
    public final String androidVersion;
    public final int sdkInt;
    public final OemSkinType oemSkin;
    public final boolean hasFreeformSystemFeature;
    public final boolean isDeveloperFreeformEnabled;
    public final boolean supportsPictureInPictureFeature;
    public final boolean supportsLaunchBoundsApi;
    public final String capabilityTierLabel;
    public final String summaryText;
    public final String oemSetupGuidance;

    public DeviceCompatibilityReport(String manufacturer,
                                     String model,
                                     String brand,
                                     String androidVersion,
                                     int sdkInt,
                                     OemSkinType oemSkin,
                                     boolean hasFreeformSystemFeature,
                                     boolean isDeveloperFreeformEnabled,
                                     boolean supportsPictureInPictureFeature,
                                     boolean supportsLaunchBoundsApi,
                                     String capabilityTierLabel,
                                     String summaryText,
                                     String oemSetupGuidance) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.brand = brand;
        this.androidVersion = androidVersion;
        this.sdkInt = sdkInt;
        this.oemSkin = oemSkin;
        this.hasFreeformSystemFeature = hasFreeformSystemFeature;
        this.isDeveloperFreeformEnabled = isDeveloperFreeformEnabled;
        this.supportsPictureInPictureFeature = supportsPictureInPictureFeature;
        this.supportsLaunchBoundsApi = supportsLaunchBoundsApi;
        this.capabilityTierLabel = capabilityTierLabel;
        this.summaryText = summaryText;
        this.oemSetupGuidance = oemSetupGuidance;
    }

    public boolean canAttemptNativeFreeform() {
        return supportsLaunchBoundsApi && (hasFreeformSystemFeature || isDeveloperFreeformEnabled);
    }
}
