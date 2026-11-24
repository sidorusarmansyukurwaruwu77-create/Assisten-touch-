package com.example.touchflow.models;

public class TouchFlowConfig {
    public boolean serviceEnabled;
    public int buttonSizeDp;          // 35 - 80 dp
    public int opacityPercent;        // 10 - 100 %
    public int buttonColor;
    public int iconColor;
    public int shadowIntensity;       // 0 - 100 %
    public int animationSpeed;        // 0 = Slow, 1 = Normal, 2 = Snappy
    public OrbIconStyle orbIconStyle;
    public OverlayTheme overlayTheme;
    public MenuLayoutType menuLayout;
    public int customThemeColor;

    public ActionItem singleTapAction;
    public ActionItem doubleTapAction;
    public ActionItem longPressAction;
    public ActionItem swipeAction;

    public boolean hapticEnabled;
    public boolean edgeSnapEnabled;
    public boolean idleDimEnabled;

    public long getAnimationDurationMs() {
        switch (animationSpeed) {
            case 0:
                return 340L;
            case 2:
                return 140L;
            case 1:
            default:
                return 220L;
        }
    }

    public String getAnimationSpeedLabel() {
        switch (animationSpeed) {
            case 0:
                return "Smooth";
            case 2:
                return "Snappy";
            case 1:
            default:
                return "Balanced";
        }
    }
}
