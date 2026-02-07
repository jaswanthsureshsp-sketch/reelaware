package com.example.appusagereminder;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "usage_alerts";
    private static final String CHANNEL_NAME = "App Usage Alerts";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermission();
        createNotificationChannel();

        Button btnUsageAccess = findViewById(R.id.btnUsageAccess);
        btnUsageAccess.setOnClickListener(v -> {
            Intent intent =
                    new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        });

        // Schedule background worker (safe to call repeatedly)
        scheduleUsageWorker();

        // Initial UI update
        updateDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboard(); // refresh when returning from settings
    }

    // ---------------- DASHBOARD ----------------

    private void updateDashboard() {

        SharedPreferences prefs =
                getSharedPreferences("usage_prefs", MODE_PRIVATE);

        long sessionUsage =
                prefs.getLong("session_usage", 0);

        int lastNotified =
                prefs.getInt("last_notified_min", 0);

        int nextAlert =
                (lastNotified == 0) ? 15 : lastNotified + 10;

        TextView tvSessionUsage =
                findViewById(R.id.tvSessionUsage);
        TextView tvNextAlert =
                findViewById(R.id.tvNextAlert);
        TextView tvTrackingStatus =
                findViewById(R.id.tvTrackingStatus);
        TextView tvBrainRot =
                findViewById(R.id.tvBrainRot);
        TextView tvLastChecked =
                findViewById(R.id.tvLastChecked);

        // Session usage + next alert
        tvSessionUsage.setText(
                "Session usage: " + sessionUsage + " minutes"
        );

        tvNextAlert.setText(
                "Next alert at: " + nextAlert + " minutes"
        );

        // Dynamic brain warning (single line, no spam)
        if (sessionUsage >= 15) {
            tvBrainRot.setVisibility(TextView.VISIBLE);
            tvBrainRot.setText(
                    "⚠️ " + sessionUsage +
                            " minutes of reels usage may affect cognitive focus"
            );
        } else {
            tvBrainRot.setVisibility(TextView.GONE);
        }

        // Green / Red Tracking status indicator
        if (hasUsageAccess()) {
            tvTrackingStatus.setText("Tracking: ON");
            tvTrackingStatus.setTextColor(
                    ContextCompat.getColor(
                            this,
                            android.R.color.holo_green_dark
                    )
            );
        } else {
            tvTrackingStatus.setText("Tracking: OFF");
            tvTrackingStatus.setTextColor(
                    ContextCompat.getColor(
                            this,
                            android.R.color.holo_red_dark
                    )
            );
        }

        // 🕒 Last checked (UI trust indicator)
        tvLastChecked.setText("Last checked: just now");
    }

    // ---------------- USAGE ACCESS CHECK ----------------

    private boolean hasUsageAccess() {

        AppOpsManager appOps =
                (AppOpsManager)
                        getSystemService(Context.APP_OPS_SERVICE);

        if (appOps == null) return false;

        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                getPackageName()
        );

        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // ---------------- WORK MANAGER ----------------

    private void scheduleUsageWorker() {

        Constraints constraints =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build();

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(
                        UsageWorker.class,
                        15,
                        TimeUnit.MINUTES
                )
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "UsageWorker",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        workRequest
                );
    }

    // ---------------- PERMISSIONS ----------------

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_DEFAULT
                    );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
