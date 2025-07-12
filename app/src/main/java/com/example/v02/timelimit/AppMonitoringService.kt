package com.example.v02.timelimit


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.v02.R

class AppMonitoringService : Service() {

    private val TAG = "AppMonitoringService"
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 5000L // Check every 5 seconds (just for UI updates)

    private val monitoringRunnable = object : Runnable {
        override fun run() {
            // This service now just keeps the app alive and updates UI
            // The actual blocking is done by the AccessibilityService
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLimits.initialize(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createServiceNotification())
        handler.post(monitoringRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitoringRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "App Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app usage and enforces time limits"
                setShowBadge(false)
                setSound(null, null)
            }

            // Limit reached channel
            val limitChannel = NotificationChannel(
                LIMIT_CHANNEL_ID,
                "App Blocking Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when apps are blocked due to time limits"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(limitChannel)
        }
    }

    private fun createServiceNotification(): Notification {
        // Create intent to open accessibility settings if service is not enabled
        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            settingsIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Time Limit Active")
            .setContentText("Monitoring app usage and enforcing time limits")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "app_monitoring_channel"
        const val LIMIT_CHANNEL_ID = "app_blocking_channel"

        fun start(context: Context) {
            val intent = Intent(context, AppMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitoringService::class.java)
            context.stopService(intent)
        }
    }
}