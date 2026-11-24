package com.example.touchflow.minimax.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.touchflow.models.AppShortcut;
import com.example.touchflow.utilities.AppPackageHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MiniMaxAppSelectAdapter extends RecyclerView.Adapter<MiniMaxAppSelectAdapter.ViewHolder> {

    private final Context context;
    private final List<AppShortcut> allApps = new ArrayList<>();
    private final List<AppShortcut> filteredApps = new ArrayList<>();
    private final Set<String> selectedPackages = new HashSet<>();

    public MiniMaxAppSelectAdapter(Context context, List<AppShortcut> apps, Set<String> initialSelected) {
        this.context = context;
        this.allApps.addAll(apps);
        this.filteredApps.addAll(apps);
        if (initialSelected != null) {
            this.selectedPackages.addAll(initialSelected);
        }
    }

    public Set<String> getSelectedPackages() {
        return new HashSet<>(selectedPackages);
    }

    public void filter(String query) {
        filteredApps.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredApps.addAll(allApps);
        } else {
            String lower = query.trim().toLowerCase(Locale.ROOT);
            for (AppShortcut app : allApps) {
                if (app.getLabel().toLowerCase(Locale.ROOT).contains(lower)
                        || app.getPackageName().toLowerCase(Locale.ROOT).contains(lower)) {
                    filteredApps.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_minimax_app_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppShortcut app = filteredApps.get(position);
        holder.tvName.setText(app.getLabel());
        holder.tvPkg.setText(app.getPackageName());

        Drawable icon = AppPackageHelper.loadAppIcon(context, app.getPackageName());
        if (icon != null) {
            holder.ivIcon.setImageDrawable(icon);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_nav_apps);
        }

        boolean checked = selectedPackages.contains(app.getPackageName());
        holder.checkBox.setChecked(checked);

        holder.itemView.setOnClickListener(v -> {
            if (selectedPackages.contains(app.getPackageName())) {
                selectedPackages.remove(app.getPackageName());
            } else {
                selectedPackages.add(app.getPackageName());
            }
            holder.checkBox.setChecked(selectedPackages.contains(app.getPackageName()));
        });
    }

    @Override
    public int getItemCount() {
        return filteredApps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvName;
        final TextView tvPkg;
        final CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_mm_app_icon);
            tvName = itemView.findViewById(R.id.tv_mm_app_name);
            tvPkg = itemView.findViewById(R.id.tv_mm_app_pkg);
            checkBox = itemView.findViewById(R.id.cb_mm_app_selected);
        }
    }
}
