package com.example.touchflow.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.widget.ImageViewCompat;

import com.example.R;
import com.example.touchflow.activities.MainActivity;
import com.example.touchflow.models.ActionItem;
import com.example.touchflow.models.AppShortcut;
import com.example.touchflow.models.MenuLayoutType;
import com.example.touchflow.models.TouchFlowConfig;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.utilities.AppPackageHelper;
import com.example.touchflow.utilities.LocaleHelper;
import com.example.touchflow.utilities.SystemActionHelper;
import com.example.touchflow.utilities.ViewAnimationHelper;

import java.util.List;

public class FloatingMenuController {

    public interface Listener {
        void onMenuClosed();
        void onOpenQuickPanel();
        void onExecuteAction(ActionItem action);
        default void onOpenMiniMax() {}
    }

    private final Context context;
    private final WindowManager windowManager;
    private final TouchFlowPreferences preferences;
    private final SystemActionHelper systemActionHelper;
    private final Listener listener;

    private View rootView;
    private LinearLayout cardContainer;
    private FrameLayout actionsHost;
    private Context uiContext;
    private boolean isShowing = false;
    private TouchFlowConfig currentConfig;

    public FloatingMenuController(Context context,
                                  WindowManager windowManager,
                                  TouchFlowPreferences preferences,
                                  SystemActionHelper systemActionHelper,
                                  Listener listener) {
        this.context = context;
        this.windowManager = windowManager;
        this.preferences = preferences;
        this.systemActionHelper = systemActionHelper;
        this.listener = listener;
    }

    public boolean isShowing() {
        return isShowing;
    }

    @SuppressLint("InflateParams")
    public void show(TouchFlowConfig config) {
        this.currentConfig = config;
        if (isShowing) return;

        uiContext = LocaleHelper.wrap(context);
        LayoutInflater inflater = LayoutInflater.from(uiContext);
        rootView = inflater.inflate(R.layout.overlay_assistive_menu, null);
        cardContainer = rootView.findViewById(R.id.menu_card_container);
        actionsHost = rootView.findViewById(R.id.menu_actions_host);

        View scrim = rootView.findViewById(R.id.menu_scrim);
        scrim.setOnClickListener(v -> dismiss(true));

        ImageView btnApp = rootView.findViewById(R.id.btn_open_touchflow_app);
        btnApp.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            dismiss(false);
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        });

        ImageButton btnMiniMax = rootView.findViewById(R.id.btn_menu_minimax);
        if (btnMiniMax != null) {
            btnMiniMax.setOnClickListener(v -> {
                systemActionHelper.performHaptic(config.hapticEnabled);
                dismiss(false);
                if (listener != null) listener.onOpenMiniMax();
            });
        }

        View btnMinPill = rootView.findViewById(R.id.btn_menu_minimize_pill);
        if (btnMinPill != null) {
            btnMinPill.setOnClickListener(v -> {
                systemActionHelper.performHaptic(config.hapticEnabled);
                if (listener != null) listener.onExecuteAction(ActionItem.MINIMIZE);
            });
        }

        View btnMiniMaxPill = rootView.findViewById(R.id.btn_menu_minimax_pill);
        if (btnMiniMaxPill != null) {
            btnMiniMaxPill.setOnClickListener(v -> {
                systemActionHelper.performHaptic(config.hapticEnabled);
                dismiss(false);
                if (listener != null) listener.onOpenMiniMax();
            });
        }

        TextView btnQuickPanel = rootView.findViewById(R.id.btn_switch_quick_panel);
        btnQuickPanel.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            dismiss(false);
            if (listener != null) listener.onOpenQuickPanel();
        });

        ImageButton btnClose = rootView.findViewById(R.id.btn_close_menu);
        btnClose.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            dismiss(true);
        });

        bindThemeAndHeader(config);
        populateMenuLayout(config);
        populateFavoriteApps(config);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        try {
            windowManager.addView(rootView, params);
            isShowing = true;
            ViewAnimationHelper.animateMenuExpand(cardContainer, config.getAnimationDurationMs());
        } catch (Exception ignored) {
            isShowing = false;
            rootView = null;
        }
    }

    private void bindThemeAndHeader(TouchFlowConfig config) {
        Context ctx = uiContext != null ? uiContext : LocaleHelper.wrap(context);
        int primaryText = OverlayThemeApplier.getPrimaryTextColor(config);
        int accent = OverlayThemeApplier.getAccentColor(config);

        TextView title = rootView.findViewById(R.id.tv_menu_title);
        title.setTextColor(primaryText);

        ImageButton btnMiniMax = rootView.findViewById(R.id.btn_menu_minimax);
        if (btnMiniMax != null) {
            OverlayThemeApplier.applyTileBackground(ctx, btnMiniMax, config, false);
            ImageViewCompat.setImageTintList(btnMiniMax, ColorStateList.valueOf(accent));
        }

        View btnMinPill = rootView.findViewById(R.id.btn_menu_minimize_pill);
        ImageView ivMinPill = rootView.findViewById(R.id.iv_menu_minimize_pill);
        TextView tvMinPill = rootView.findViewById(R.id.tv_menu_minimize_pill);
        if (btnMinPill != null) {
            OverlayThemeApplier.applyTileBackground(ctx, btnMinPill, config, false);
            if (ivMinPill != null) ImageViewCompat.setImageTintList(ivMinPill, ColorStateList.valueOf(accent));
            if (tvMinPill != null) tvMinPill.setTextColor(primaryText);
        }

        View btnMiniMaxPill = rootView.findViewById(R.id.btn_menu_minimax_pill);
        ImageView ivMiniMaxPill = rootView.findViewById(R.id.iv_menu_minimax_pill);
        TextView tvMiniMaxPill = rootView.findViewById(R.id.tv_menu_minimax_pill);
        if (btnMiniMaxPill != null) {
            OverlayThemeApplier.applyTileBackground(ctx, btnMiniMaxPill, config, false);
            if (ivMiniMaxPill != null) ImageViewCompat.setImageTintList(ivMiniMaxPill, ColorStateList.valueOf(accent));
            if (tvMiniMaxPill != null) tvMiniMaxPill.setTextColor(primaryText);
        }

        TextView btnQuickPanel = rootView.findViewById(R.id.btn_switch_quick_panel);
        btnQuickPanel.setTextColor(primaryText);
        OverlayThemeApplier.applyTileBackground(ctx, btnQuickPanel, config, true);

        ImageView btnApp = rootView.findViewById(R.id.btn_open_touchflow_app);
        ImageViewCompat.setImageTintList(btnApp, ColorStateList.valueOf(accent));

        ImageButton btnClose = rootView.findViewById(R.id.btn_close_menu);
        ImageViewCompat.setImageTintList(btnClose, ColorStateList.valueOf(primaryText));

        float density = ctx.getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams cardParams = (LinearLayout.LayoutParams) cardContainer.getLayoutParams();
        if (config.menuLayout == MenuLayoutType.CIRCULAR) {
            cardParams.width = Math.round(332 * density);
            OverlayThemeApplier.applyPanelBackground(ctx, cardContainer, config, 32f);
        } else if (config.menuLayout == MenuLayoutType.FLOATING_RECTANGLE) {
            cardParams.width = Math.round(316 * density);
            OverlayThemeApplier.applyPanelBackground(ctx, cardContainer, config, 24f);
        } else if (config.menuLayout == MenuLayoutType.COMPACT) {
            cardParams.width = Math.round(336 * density);
            OverlayThemeApplier.applyPanelBackground(ctx, cardContainer, config, 22f);
        } else {
            cardParams.width = Math.round(332 * density);
            OverlayThemeApplier.applyPanelBackground(ctx, cardContainer, config, 28f);
        }
        cardContainer.setLayoutParams(cardParams);
    }

    private void populateMenuLayout(TouchFlowConfig config) {
        actionsHost.removeAllViews();
        List<ActionItem> actions = ActionItem.getMainMenuActions();
        MenuLayoutType layoutType = config.menuLayout;

        if (layoutType == MenuLayoutType.CIRCULAR) {
            buildCircularMenu(actions, config);
        } else if (layoutType == MenuLayoutType.FLOATING_RECTANGLE) {
            buildRectangleMenu(actions, config);
        } else if (layoutType == MenuLayoutType.COMPACT) {
            buildCompactMenu(actions, config);
        } else {
            buildGridMenu(actions, config);
        }
    }

    private void buildGridMenu(List<ActionItem> actions, TouchFlowConfig config) {
        float density = context.getResources().getDisplayMetrics().density;
        int margin = Math.round(4 * density);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(4);
        grid.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        for (ActionItem item : actions) {
            View tile = createActionTile(item, config, false);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(margin, margin, margin, margin);
            tile.setLayoutParams(lp);
            grid.addView(tile);
        }
        actionsHost.addView(grid);
    }

    private void buildCircularMenu(List<ActionItem> actions, TouchFlowConfig config) {
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = Math.round(300 * density);

        FrameLayout ringCanvas = new FrameLayout(context);
        FrameLayout.LayoutParams canvasLp = new FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER_HORIZONTAL);
        ringCanvas.setLayoutParams(canvasLp);
        OverlayThemeApplier.applyCircleBackground(context, ringCanvas, config);

        int center = sizePx / 2;

        // Center hub button -> Quick Access Panel
        LinearLayout centerHub = new LinearLayout(context);
        int hubSize = Math.round(62 * density);
        FrameLayout.LayoutParams hubLp = new FrameLayout.LayoutParams(hubSize, hubSize);
        hubLp.leftMargin = center - hubSize / 2;
        hubLp.topMargin = center - hubSize / 2;
        centerHub.setLayoutParams(hubLp);
        centerHub.setGravity(Gravity.CENTER);
        centerHub.setOrientation(LinearLayout.VERTICAL);
        OverlayThemeApplier.applyTileBackground(context, centerHub, config, true);

        ImageView hubIcon = new ImageView(context);
        int hubIconSize = Math.round(24 * density);
        hubIcon.setLayoutParams(new LinearLayout.LayoutParams(hubIconSize, hubIconSize));
        hubIcon.setImageResource(R.drawable.ic_action_quick_panel);
        ImageViewCompat.setImageTintList(hubIcon, ColorStateList.valueOf(OverlayThemeApplier.getAccentColor(config)));
        centerHub.addView(hubIcon);

        centerHub.setOnClickListener(v -> {
            systemActionHelper.performHaptic(config.hapticEnabled);
            dismiss(false);
            if (listener != null) listener.onOpenQuickPanel();
        });
        ringCanvas.addView(centerHub);

        // Place 6 items on inner ring and 10 items on outer ring
        int innerCount = 6;
        int outerCount = actions.size() - innerCount;
        float innerRadius = 72 * density;
        float outerRadius = 119 * density;
        int nodeSize = Math.round(48 * density);

        for (int i = 0; i < actions.size(); i++) {
            ActionItem action = actions.get(i);
            boolean isInner = i < innerCount;
            int idx = isInner ? i : (i - innerCount);
            int totalInRing = isInner ? innerCount : outerCount;
            double angle = (2.0 * Math.PI * idx / totalInRing) - (Math.PI / 2.0);
            float r = isInner ? innerRadius : outerRadius;

            int cx = Math.round(center + (float) (r * Math.cos(angle)));
            int cy = Math.round(center + (float) (r * Math.sin(angle)));

            View node = createCircularNode(action, config, nodeSize);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(nodeSize, nodeSize);
            lp.leftMargin = cx - nodeSize / 2;
            lp.topMargin = cy - nodeSize / 2;
            node.setLayoutParams(lp);
            ringCanvas.addView(node);
        }

        actionsHost.addView(ringCanvas);
    }

    private void buildRectangleMenu(List<ActionItem> actions, TouchFlowConfig config) {
        float density = context.getResources().getDisplayMetrics().density;
        ScrollView scroll = new ScrollView(context);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(300 * density)
        ));
        scroll.setVerticalScrollBarEnabled(false);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(2);
        grid.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        int margin = Math.round(4 * density);
        for (ActionItem item : actions) {
            View rowTile = createRectangleRowTile(item, config);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(margin, margin, margin, margin);
            rowTile.setLayoutParams(lp);
            grid.addView(rowTile);
        }

        scroll.addView(grid);
        actionsHost.addView(scroll);
    }

    private void buildCompactMenu(List<ActionItem> actions, TouchFlowConfig config) {
        float density = context.getResources().getDisplayMetrics().density;
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        GridLayout grid = new GridLayout(context);
        grid.setRowCount(2);
        grid.setColumnCount(8);
        grid.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        int margin = Math.round(4 * density);
        int tileW = Math.round(72 * density);
        for (ActionItem item : actions) {
            View tile = createActionTile(item, config, true);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = tileW;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.setMargins(margin, margin, margin, margin);
            tile.setLayoutParams(lp);
            grid.addView(tile);
        }

        scroll.addView(grid);
        actionsHost.addView(scroll);
    }

    private View createActionTile(ActionItem item, TouchFlowConfig config, boolean compact) {
        Context ctx = uiContext != null ? uiContext : LocaleHelper.wrap(context);
        LayoutInflater inflater = LayoutInflater.from(ctx);
        View tile = inflater.inflate(R.layout.item_overlay_action, actionsHost, false);
        ImageView icon = tile.findViewById(R.id.iv_action_icon);
        TextView label = tile.findViewById(R.id.tv_action_label);

        boolean isActive = (item == ActionItem.FLASHLIGHT && systemActionHelper.isTorchOn());
        OverlayThemeApplier.applyTileBackground(ctx, tile, config, isActive);

        icon.setImageResource(item.getIconRes());
        int tint = isActive ? OverlayThemeApplier.getAccentColor(config) : OverlayThemeApplier.getPrimaryTextColor(config);
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(tint));

        label.setText(item.getTitleRes());
        label.setTextColor(OverlayThemeApplier.getPrimaryTextColor(config));
        if (compact) {
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        }

        tile.setContentDescription(ctx.getString(item.getTitleRes()));
        tile.setOnClickListener(v -> handleActionTileClick(v, item, config));
        return tile;
    }

    private View createCircularNode(ActionItem item, TouchFlowConfig config, int sizePx) {
        Context ctx = uiContext != null ? uiContext : LocaleHelper.wrap(context);
        float density = ctx.getResources().getDisplayMetrics().density;
        LinearLayout node = new LinearLayout(ctx);
        node.setOrientation(LinearLayout.VERTICAL);
        node.setGravity(Gravity.CENTER);
        node.setPadding(Math.round(4 * density), Math.round(4 * density), Math.round(4 * density), Math.round(4 * density));
        boolean isActive = (item == ActionItem.FLASHLIGHT && systemActionHelper.isTorchOn());
        OverlayThemeApplier.applyTileBackground(ctx, node, config, isActive);

        ImageView icon = new ImageView(ctx);
        int iconSz = Math.round(18 * density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSz, iconSz));
        icon.setImageResource(item.getIconRes());
        int tint = isActive ? OverlayThemeApplier.getAccentColor(config) : OverlayThemeApplier.getPrimaryTextColor(config);
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(tint));

        TextView label = new TextView(ctx);
        label.setText(item.getTitleRes());
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
        label.setTextColor(OverlayThemeApplier.getSecondaryTextColor(config));
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);

        node.addView(icon);
        node.addView(label);
        node.setContentDescription(ctx.getString(item.getTitleRes()));
        node.setOnClickListener(v -> handleActionTileClick(v, item, config));
        return node;
    }

    private View createRectangleRowTile(ActionItem item, TouchFlowConfig config) {
        Context ctx = uiContext != null ? uiContext : LocaleHelper.wrap(context);
        float density = ctx.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Math.round(46 * density));
        int padH = Math.round(10 * density);
        int padV = Math.round(8 * density);
        row.setPadding(padH, padV, padH, padV);

        boolean isActive = (item == ActionItem.FLASHLIGHT && systemActionHelper.isTorchOn());
        OverlayThemeApplier.applyTileBackground(ctx, row, config, isActive);

        ImageView icon = new ImageView(ctx);
        int iconSz = Math.round(18 * density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSz, iconSz));
        icon.setImageResource(item.getIconRes());
        int tint = isActive ? OverlayThemeApplier.getAccentColor(config) : OverlayThemeApplier.getPrimaryTextColor(config);
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(tint));

        TextView label = new TextView(ctx);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        tvLp.setMarginStart(Math.round(8 * density));
        label.setLayoutParams(tvLp);
        label.setText(item.getTitleRes());
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        label.setTextColor(OverlayThemeApplier.getPrimaryTextColor(config));
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);

        row.addView(icon);
        row.addView(label);
        row.setContentDescription(ctx.getString(item.getTitleRes()));
        row.setOnClickListener(v -> handleActionTileClick(v, item, config));
        return row;
    }

    private void handleActionTileClick(View view, ActionItem item, TouchFlowConfig config) {
        systemActionHelper.performHaptic(config.hapticEnabled);
        ViewAnimationHelper.animateSpringScale(view, 0.94f);
        view.postDelayed(() -> ViewAnimationHelper.animateSpringScale(view, 1.0f), 90L);
        if (listener != null) {
            listener.onExecuteAction(item);
        }
        if (item == ActionItem.FLASHLIGHT && isShowing) {
            // Refresh tile tint smoothly to reflect new torch state
            actionsHost.postDelayed(() -> {
                if (isShowing && currentConfig != null) {
                    populateMenuLayout(currentConfig);
                }
            }, 90L);
        }
    }

    private void populateFavoriteApps(TouchFlowConfig config) {
        List<AppShortcut> shortcuts = preferences.getShortcuts();
        LinearLayout section = rootView.findViewById(R.id.menu_favorites_section);
        LinearLayout row = rootView.findViewById(R.id.menu_favorites_row);
        TextView sectionTitle = rootView.findViewById(R.id.tv_menu_favorites_label);

        if (shortcuts.isEmpty()) {
            section.setVisibility(View.GONE);
            return;
        }

        section.setVisibility(View.VISIBLE);
        sectionTitle.setTextColor(OverlayThemeApplier.getSecondaryTextColor(config));
        row.removeAllViews();

        float density = context.getResources().getDisplayMetrics().density;
        int itemWidth = Math.round(62 * density);
        int iconSize = Math.round(28 * density);
        int marginEnd = Math.round(6 * density);

        for (AppShortcut app : shortcuts) {
            LinearLayout chip = new LinearLayout(context);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(marginEnd);
            chip.setLayoutParams(lp);
            int pad = Math.round(6 * density);
            chip.setPadding(pad, pad, pad, pad);
            OverlayThemeApplier.applyTileBackground(context, chip, config, false);

            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
            Drawable appIcon = AppPackageHelper.loadAppIcon(context, app.getPackageName());
            if (appIcon != null) {
                iv.setImageDrawable(appIcon);
            } else {
                iv.setImageResource(R.drawable.ic_nav_apps);
            }

            TextView tv = new TextView(context);
            tv.setText(app.getLabel());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            tv.setTextColor(OverlayThemeApplier.getPrimaryTextColor(config));
            tv.setMaxLines(1);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tvLp.topMargin = Math.round(3 * density);
            tv.setLayoutParams(tvLp);

            chip.addView(iv);
            chip.addView(tv);
            chip.setOnClickListener(v -> {
                systemActionHelper.performHaptic(config.hapticEnabled);
                dismiss(true);
                AppPackageHelper.launchApp(context, app.getPackageName());
            });
            row.addView(chip);
        }
    }

    public void dismiss(boolean notifyListener) {
        if (!isShowing || rootView == null) {
            isShowing = false;
            return;
        }
        final View viewToRemove = rootView;
        final long duration = currentConfig != null ? currentConfig.getAnimationDurationMs() : 200L;
        isShowing = false;
        rootView = null;
        cardContainer = null;
        actionsHost = null;

        ViewAnimationHelper.animateMenuCollapse(viewToRemove.findViewById(R.id.menu_card_container), duration, () -> {
            try {
                windowManager.removeViewImmediate(viewToRemove);
            } catch (Exception ignored) {
            }
            if (notifyListener && listener != null) {
                listener.onMenuClosed();
            }
        });
    }

    public void removeImmediate() {
        if (rootView != null) {
            try {
                windowManager.removeViewImmediate(rootView);
            } catch (Exception ignored) {
            }
        }
        isShowing = false;
        rootView = null;
        cardContainer = null;
        actionsHost = null;
    }
}
