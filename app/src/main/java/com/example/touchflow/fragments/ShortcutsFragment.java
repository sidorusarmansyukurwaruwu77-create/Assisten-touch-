package com.example.touchflow.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.touchflow.adapters.InstalledAppAdapter;
import com.example.touchflow.adapters.ShortcutAdapter;
import com.example.touchflow.models.AppShortcut;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.utilities.AppPackageHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShortcutsFragment extends Fragment {

    private TouchFlowPreferences preferences;
    private ShortcutAdapter adapter;
    private List<AppShortcut> shortcuts = new ArrayList<>();

    private TextView tvCount;
    private View emptyStateView;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shortcuts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = new TouchFlowPreferences(requireContext());

        tvCount = view.findViewById(R.id.tv_shortcuts_count);
        emptyStateView = view.findViewById(R.id.layout_shortcuts_empty);
        recyclerView = view.findViewById(R.id.rv_shortcuts);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ShortcutAdapter(requireContext(), new ShortcutAdapter.ActionListener() {
            @Override
            public void onMoveUp(int position) {
                if (position > 0 && position < shortcuts.size()) {
                    Collections.swap(shortcuts, position, position - 1);
                    persistAndRefresh();
                }
            }

            @Override
            public void onMoveDown(int position) {
                if (position >= 0 && position < shortcuts.size() - 1) {
                    Collections.swap(shortcuts, position, position + 1);
                    persistAndRefresh();
                }
            }

            @Override
            public void onRemove(int position) {
                if (position >= 0 && position < shortcuts.size()) {
                    shortcuts.remove(position);
                    persistAndRefresh();
                }
            }
        });
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btn_add_shortcut).setOnClickListener(v -> {
            if (shortcuts.size() >= TouchFlowPreferences.MAX_SHORTCUTS) {
                Toast.makeText(requireContext(), R.string.msg_max_shortcuts, Toast.LENGTH_SHORT).show();
                return;
            }
            showAppPickerDialog();
        });

        shortcuts = preferences.getShortcuts();
        refreshUi();
    }

    private void persistAndRefresh() {
        preferences.saveShortcuts(shortcuts);
        refreshUi();
    }

    private void refreshUi() {
        adapter.submitList(shortcuts);
        tvCount.setText(shortcuts.size() + " / " + TouchFlowPreferences.MAX_SHORTCUTS);
        boolean isEmpty = shortcuts.isEmpty();
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showAppPickerDialog() {
        List<AppShortcut> installed = AppPackageHelper.queryLaunchableApps(requireContext());
        Set<String> alreadyPinned = new HashSet<>();
        for (AppShortcut s : shortcuts) {
            alreadyPinned.add(s.getPackageName());
        }
        List<AppShortcut> available = new ArrayList<>();
        for (AppShortcut app : installed) {
            if (!alreadyPinned.contains(app.getPackageName())) {
                available.add(app);
            }
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_app_picker, null);
        EditText etSearch = dialogView.findViewById(R.id.et_app_search);
        RecyclerView rvApps = dialogView.findViewById(R.id.rv_installed_apps);
        rvApps.setLayoutManager(new LinearLayoutManager(requireContext()));

        final AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        InstalledAppAdapter appAdapter = new InstalledAppAdapter(requireContext(), available, app -> {
            if (shortcuts.size() < TouchFlowPreferences.MAX_SHORTCUTS) {
                shortcuts.add(app);
                persistAndRefresh();
            }
            dialog.dismiss();
        });
        rvApps.setAdapter(appAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                appAdapter.filter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }
}
