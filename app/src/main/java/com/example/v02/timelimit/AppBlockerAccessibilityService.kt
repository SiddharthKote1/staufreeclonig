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
import com.example.v02.ReelsBlockingService.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private val TAG = "AppBlockerService"
    private val recentlyBlockedApps = mutableMapOf<String, Long>()
    private lateinit var dataStoreManager: DataStoreManager

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onServiceConnected() {
        Log.d(TAG, "🟢 Accessibility service connected successfully!")
        dataStoreManager = DataStoreManager(applicationContext)
        AppLimits.initialize(applicationContext)
        showServiceActiveNotification()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val packageName = event.packageName?.toString() ?: return

            // Skip our own app and launcher/system UI
            if (packageName == "com.example.v02" || packageName.startsWith("com.android.") || packageName.contains("launcher", true)) return

            checkAndBlockAppIfNeeded(packageName)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun checkAndBlockAppIfNeeded(packageName: String) {
        val currentTime = System.currentTimeMillis()
        val lastChecked = recentlyBlockedApps[packageName] ?: 0
        if (currentTime - lastChecked < 3000) return // throttle every 3 sec

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = dataStoreManager.getCurrentAppSettings()

                // Get correct limit depending on current mode
                val limitMinutes = if (settings.accountMode == "Parent") {
                    settings.parentAppTimeLimits[packageName] ?: 0
                } else {
                    val childId = settings.activeChildId
                    val child = settings.childProfiles.find { it.id == childId }
                    child?.appTimeLimits?.get(packageName) ?: 0
                }

                if (limitMinutes <= 0) return@launch

                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val usedMinutes = AppLimits.getTodayUsageMinutes(packageName, usageStatsManager)

                if (usedMinutes >= limitMinutes) {
                    recentlyBlockedApps[packageName] = currentTime
                    launch(Dispatchers.Main) {
                        blockApp(packageName)
                    }
                }

                // Clean old entries
                recentlyBlockedApps.entries.removeAll { currentTime - it.value > 60000 }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking limits: ${e.message}")
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun blockApp(packageName: String) {
        try {
            val appName = getAppName(packageName)
            showTimeLimitNotification(packageName, appName)
            performGlobalAction(GLOBAL_ACTION_HOME)
            Log.d(TAG, "⛔ Blocked $appName due to limit")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block app: ${e.message}")
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
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, AppMonitoringService.CHANNEL_ID)
                .setContentTitle("App Monitor Running")
                .setContentText("App usage is being monitored.")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            NotificationManagerCompat.from(this).notify(999, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show service notification: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showTimeLimitNotification(packageName: String, appName: String) {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val usedMinutes = AppLimits.getTodayUsageMinutes(packageName, usageStatsManager)

            val settings = dataStoreManager.getCurrentAppSettings()
            val limitMinutes = if (settings.accountMode == "Parent") {
                settings.parentAppTimeLimits[packageName] ?: 0
            } else {
                val childId = settings.activeChildId
                val child = settings.childProfiles.find { it.id == childId }
                child?.appTimeLimits?.get(packageName) ?: 0
            }

            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, AppMonitoringService.LIMIT_CHANNEL_ID)
                .setContentTitle("⏰ Limit Exceeded")
                .setContentText("$appName blocked (Used: ${usedMinutes}m / Limit: ${limitMinutes}m)")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(this).notify(packageName.hashCode(), notification)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show block notification: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility Service destroyed")
    }
}
