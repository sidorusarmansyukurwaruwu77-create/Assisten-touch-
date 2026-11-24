package com.example.touchflow.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.touchflow.fragments.AppearanceFragment;
import com.example.touchflow.fragments.DashboardFragment;
import com.example.touchflow.fragments.GesturesFragment;
import com.example.touchflow.fragments.PermissionsFragment;
import com.example.touchflow.fragments.ShortcutsFragment;
import com.example.touchflow.preferences.TouchFlowPreferences;
import com.example.touchflow.services.FloatingOverlayService;
import com.example.touchflow.utilities.PermissionHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private TouchFlowPreferences preferences;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (preferences != null && preferences.isServiceEnabled() && PermissionHelper.canDrawOverlays(this)) {
                    FloatingOverlayService.startService(this);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = new TouchFlowPreferences(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.top_bar), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    bars.top + Math.round(12 * getResources().getDisplayMetrics().density),
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, navBars.bottom);
            return insets;
        });

        ImageButton btnAbout = findViewById(R.id.btn_about);
        btnAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                switchFragment(new DashboardFragment());
                return true;
            } else if (id == R.id.nav_appearance) {
                switchFragment(new AppearanceFragment());
                return true;
            } else if (id == R.id.nav_gestures) {
                switchFragment(new GesturesFragment());
                return true;
            } else if (id == R.id.nav_shortcuts) {
                switchFragment(new ShortcutsFragment());
                return true;
            } else if (id == R.id.nav_permissions) {
                switchFragment(new PermissionsFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences.isServiceEnabled()
                && PermissionHelper.canDrawOverlays(this)
                && !FloatingOverlayService.isServiceRunning()) {
            FloatingOverlayService.startService(this);
        }
    }

    public void navigateToTab(int menuItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuItemId);
        }
    }

    public void toggleTouchFlowService() {
        boolean currentlyEnabled = preferences.isServiceEnabled() && FloatingOverlayService.isServiceRunning();
        if (currentlyEnabled) {
            preferences.setServiceEnabled(false);
            FloatingOverlayService.stopService(this);
            return;
        }

        if (!PermissionHelper.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.msg_overlay_permission_required, Toast.LENGTH_LONG).show();
            startActivity(PermissionHelper.buildOverlayPermissionIntent(this));
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !PermissionHelper.hasNotificationPermission(this)) {
            preferences.setServiceEnabled(true);
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        preferences.setServiceEnabled(true);
        FloatingOverlayService.startService(this);
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
