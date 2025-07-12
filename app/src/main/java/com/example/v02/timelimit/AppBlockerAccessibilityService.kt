package com.example.v02.timelimit

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.v02.MainActivity
import com.example.v02.R

class AppBlockerAccessibilityService : AccessibilityService() {

    private val TAG = "AppBlockerService"
    private val recentlyBlockedApps = mutableMapOf<String, Long>()

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onServiceConnected() {
        Log.d(TAG, "🟢 Accessibility service connected successfully!")
        AppLimits.initialize(this)

        // Show a notification that the service is active
        showServiceActiveNotification()

        // Log service info for debugging
        val serviceInfo = serviceInfo
        Log.d(TAG, "Service info: ${serviceInfo?.id}")
        Log.d(TAG, "Service package: ${serviceInfo?.resolveInfo?.serviceInfo?.packageName}")
        Log.d(TAG, "Service class: ${serviceInfo?.resolveInfo?.serviceInfo?.name}")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only process window state changes and content changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {

            val packageName = event.packageName?.toString() ?: return

            // Skip our own app and system UI
            if (packageName == "com.example.v02" ||
                packageName == "com.android.systemui" ||
                packageName.startsWith("com.android.launcher")
            ) {
                return
            }

            // Check if this app has a limit and if it's exceeded
            checkAndBlockAppIfNeeded(packageName)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun checkAndBlockAppIfNeeded(packageName: String) {
        try {
            val currentTime = System.currentTimeMillis()

            // Don't check the same app too frequently to avoid performance issues
            val lastChecked = recentlyBlockedApps[packageName] ?: 0
            if (currentTime - lastChecked < 3000) { // Only check every 3 seconds
                return
            }

            // Get usage stats manager
            val usageStatsManager =
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            // Check if app has exceeded its limit
            if (AppLimits.isAppLimitExceeded(packageName, usageStatsManager)) {
                // Update last checked time
                recentlyBlockedApps[packageName] = currentTime

                // Block the app by going to home screen
                blockApp(packageName)
            }

            // Clean up old entries from recently blocked apps
            recentlyBlockedApps.entries.removeAll { currentTime - it.value > 60000 } // Remove entries older than 1 minute

        } catch (e: Exception) {
            Log.e(TAG, "Error checking app limits: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun blockApp(packageName: String) {
        try {
            // Get app name for notification
            val appName = getAppName(packageName)

            // Show notification
            showTimeLimitNotification(packageName, appName)

            // Go to home screen (this effectively exits the app)
            val success = performGlobalAction(GLOBAL_ACTION_HOME)
            Log.d(TAG, "Blocked app: $appName ($packageName), home action success: $success")
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking app: ${e.message}")
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showServiceActiveNotification() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, AppMonitoringService.CHANNEL_ID)
                .setContentTitle("App Limit Service Active")
                .setContentText("Accessibility service is monitoring app usage")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(this).notify(999, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing service active notification: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showTimeLimitNotification(packageName: String, appName: String) {
        try {
            val usageStatsManager =
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val usageMinutes = AppLimits.getTodayUsageMinutes(packageName, usageStatsManager)
            val limitMinutes = AppLimits.getLimit(packageName)

            // Create intent to open our app
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val notification =
                NotificationCompat.Builder(this, AppMonitoringService.LIMIT_CHANNEL_ID)
                    .setContentTitle("🚫 App Blocked!")
                    .setContentText("$appName has been closed - time limit reached")
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("$appName has been automatically closed because you've reached your daily limit.\n\nUsage: ${usageMinutes} minutes\nLimit: ${limitMinutes} minutes\n\nTake a break and try again tomorrow!")
                    )
                    .setSmallIcon(R.drawable.ic_notification)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 500, 200, 500))
                    .build()

            // Show notification
            NotificationManagerCompat.from(this).notify(packageName.hashCode(), notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }
}