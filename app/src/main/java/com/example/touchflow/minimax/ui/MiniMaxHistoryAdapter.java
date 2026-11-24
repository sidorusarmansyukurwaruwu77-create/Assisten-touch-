package com.example.touchflow.minimax.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.touchflow.minimax.models.AutomationLogEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MiniMaxHistoryAdapter extends RecyclerView.Adapter<MiniMaxHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<AutomationLogEntry> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public MiniMaxHistoryAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<AutomationLogEntry> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_minimax_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutomationLogEntry entry = items.get(position);
        String name = (entry.appLabel != null && !entry.appLabel.isEmpty()) ? entry.appLabel : entry.packageName;
        holder.tvApp.setText(name);
        holder.tvMeta.setText(timeFormat.format(new Date(entry.timestampMs))
                + " • " + entry.triggerLabel + " • " + entry.mechanism);
        holder.tvDetail.setText(entry.detail);

        holder.tvStatus.setText(entry.success ? "Freeform" : "Fallback");
        holder.tvStatus.setTextColor(ContextCompat.getColor(
                context,
                entry.success ? R.color.tf_success : R.color.tf_warning
        ));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvApp;
        final TextView tvStatus;
        final TextView tvMeta;
        final TextView tvDetail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvApp = itemView.findViewById(R.id.tv_hist_app);
            tvStatus = itemView.findViewById(R.id.tv_hist_status);
            tvMeta = itemView.findViewById(R.id.tv_hist_meta);
            tvDetail = itemView.findViewById(R.id.tv_hist_detail);
        }
    }
}
