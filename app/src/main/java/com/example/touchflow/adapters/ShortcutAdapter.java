package com.example.touchflow.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.touchflow.models.AppShortcut;
import com.example.touchflow.utilities.AppPackageHelper;

import java.util.ArrayList;
import java.util.List;

public class ShortcutAdapter extends RecyclerView.Adapter<ShortcutAdapter.ViewHolder> {

    public interface ActionListener {
        void onMoveUp(int position);
        void onMoveDown(int position);
        void onRemove(int position);
    }

    private final Context context;
    private final List<AppShortcut> items = new ArrayList<>();
    private final ActionListener listener;

    public ShortcutAdapter(Context context, ActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submitList(List<AppShortcut> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortcut_selected, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppShortcut item = items.get(position);
        holder.tvName.setText(item.getLabel());
        holder.tvPkg.setText(item.getPackageName());

        Drawable icon = AppPackageHelper.loadAppIcon(context, item.getPackageName());
        if (icon != null) {
            holder.ivIcon.setImageDrawable(icon);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_nav_apps);
        }

        holder.btnUp.setEnabled(position > 0);
        holder.btnUp.setAlpha(position > 0 ? 1f : 0.3f);
        holder.btnDown.setEnabled(position < items.size() - 1);
        holder.btnDown.setAlpha(position < items.size() - 1 ? 1f : 0.3f);

        holder.btnUp.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onMoveUp(pos);
            }
        });
        holder.btnDown.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onMoveDown(pos);
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRemove(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvName;
        final TextView tvPkg;
        final ImageButton btnUp;
        final ImageButton btnDown;
        final ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_shortcut_icon);
            tvName = itemView.findViewById(R.id.tv_shortcut_name);
            tvPkg = itemView.findViewById(R.id.tv_shortcut_pkg);
            btnUp = itemView.findViewById(R.id.btn_shortcut_up);
            btnDown = itemView.findViewById(R.id.btn_shortcut_down);
            btnDelete = itemView.findViewById(R.id.btn_shortcut_delete);
        }
    }
}
