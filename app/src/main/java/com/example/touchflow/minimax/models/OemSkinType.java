package com.example.touchflow.minimax.models;

public enum OemSkinType {
    TECNO_HIOS("TECNO HiOS"),
    INFINIX_XOS("Infinix XOS"),
    SAMSUNG_ONE_UI("Samsung One UI"),
    XIAOMI_HYPEROS("Xiaomi HyperOS / MIUI"),
    OPPO_COLOROS("OPPO ColorOS / Realme UI"),
    VIVO_FUNTOUCH("Vivo Funtouch / OriginOS"),
    ANDROID_STOCK("Android Stock / AOSP"),
    OTHER_OEM("Other OEM Android");

    private final String displayName;

    OemSkinType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
