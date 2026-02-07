package com.example.appusagereminder;

import android.app.NotificationManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.List;

public class UsageWorker extends Worker {

    private static final String CHANNEL_ID = "usage_alerts";
    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";

    private static final int FIRST_THRESHOLD = 15;
    private static final int REPEAT_INTERVAL = 10;
    private static final int NOTIFICATION_ID = 1001;

    public UsageWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        SharedPreferences prefs =
                getApplicationContext()
                        .getSharedPreferences(
                                "usage_prefs",
                                Context.MODE_PRIVATE
                        );

        // 🔁 MIDNIGHT RESET (runs once per new day)
        handleMidnightReset(prefs);

        long currentTotalUsage = getInstagramUsageMinutes();

        long lastTotalUsage =
                prefs.getLong("last_total_usage", 0);

        long deltaUsage = currentTotalUsage - lastTotalUsage;

        // Save current total for next run
        prefs.edit()
                .putLong("last_total_usage", currentTotalUsage)
                .apply();

        // ❌ Instagram not used since last check
        if (deltaUsage <= 0) {
            Log.d("UsageWorker", "No Instagram usage increment");
            return Result.success();
        }

        // ✅ Session-based counting
        long sessionUsage =
                prefs.getLong("session_usage", 0) + deltaUsage;

        prefs.edit()
                .putLong("session_usage", sessionUsage)
                .apply();

        Log.d(
                "UsageWorker",
                "Instagram session usage: " + sessionUsage + " minutes"
        );

        checkThresholdAndNotify(sessionUsage);

        return Result.success();
    }

    // 🔁 Reset session safely at midnight
    private void handleMidnightReset(SharedPreferences prefs) {

        Calendar today = Calendar.getInstance();
        int todayKey =
                today.get(Calendar.YEAR) * 10000
                        + (today.get(Calendar.MONTH) + 1) * 100
                        + today.get(Calendar.DAY_OF_MONTH);

        int lastResetDay =
                prefs.getInt("last_reset_day", -1);

        if (lastResetDay != todayKey) {
            prefs.edit()
                    .putInt("last_reset_day", todayKey)
                    .putLong("session_usage", 0)
                    .putInt("last_notified_min", 0)
                    .putLong("last_total_usage", 0)
                    .apply();
        }
    }

    // 🔍 Get today's total Instagram usage
    private long getInstagramUsageMinutes() {

        UsageStatsManager usageStatsManager =
                (UsageStatsManager)
                        getApplicationContext()
                                .getSystemService(Context.USAGE_STATS_SERVICE);

        if (usageStatsManager == null) return 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> stats =
                usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        startTime,
                        endTime
                );

        if (stats == null) return 0;

        for (UsageStats usageStats : stats) {
            if (INSTAGRAM_PACKAGE.equals(
                    usageStats.getPackageName())) {

                return usageStats.getTotalTimeInForeground()
                        / (1000 * 60);
            }
        }

        return 0;
    }

    // 🔔 Threshold logic (15 → +10), single notification
    private void checkThresholdAndNotify(long sessionUsage) {

        SharedPreferences prefs =
                getApplicationContext()
                        .getSharedPreferences(
                                "usage_prefs",
                                Context.MODE_PRIVATE
                        );

        int lastNotified =
                prefs.getInt("last_notified_min", 0);

        if (sessionUsage < FIRST_THRESHOLD) return;

        int nextNotifyAt =
                (lastNotified == 0)
                        ? FIRST_THRESHOLD
                        : lastNotified + REPEAT_INTERVAL;

        if (sessionUsage >= nextNotifyAt) {

            showUsageNotification(nextNotifyAt);

            prefs.edit()
                    .putInt("last_notified_min", nextNotifyAt)
                    .apply();
        }
    }

    // 🔔 SINGLE, UPDATING notification
    private void showUsageNotification(int minutes) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        getApplicationContext(),
                        CHANNEL_ID
                )
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Instagram usage")
                        .setContentText(
                                "You have used Instagram for "
                                        + minutes
                                        + " minutes"
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(false);

        NotificationManager manager =
                (NotificationManager)
                        getApplicationContext()
                                .getSystemService(
                                        Context.NOTIFICATION_SERVICE
                                );

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
