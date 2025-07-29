package com.example.v02.timelimit

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.v02.MainActivity
import com.example.v02.R
import com.example.v02.ReelsBlockingService.BlockedKeywordLists
import com.example.v02.ReelsBlockingService.DataStoreManager
import com.example.v02.screens.KeywordLists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

            // ✅ Skip our own app, system UI, and keyboards
            if (packageName == "com.example.v02" ||
                packageName.startsWith("com.android.") ||
                packageName.contains("launcher", true) ||
                packageName.contains("inputmethod", true) ||
                packageName.contains("gboard", true) ||
                packageName.contains("keyboard", true)
            ) return

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

                val isParent = settings.accountMode == "Parent"

                // ✅ 1. Keyword Blocking
                val blockedKeywords = mutableListOf<String>().apply {
                    val keywordLists = if (isParent) {
                        settings.blockedKeywordLists
                    } else {
                        settings.childProfiles.find { it.id == settings.activeChildId }?.blockedKeywordLists
                            ?: BlockedKeywordLists()
                    }

                    if (keywordLists.adult) addAll(KeywordLists.adult)
                    if (keywordLists.gambling) addAll(KeywordLists.gambling)
                    if (keywordLists.violent) addAll(KeywordLists.violent)
                    if (keywordLists.hate) addAll(KeywordLists.hate)
                    if (keywordLists.drug) addAll(KeywordLists.drug)
                    if (keywordLists.scam) addAll(KeywordLists.scam)

                    val customKeywords = if (isParent) {
                        settings.customBlockedKeywords
                    } else {
                        settings.childProfiles.find { it.id == settings.activeChildId }?.customBlockedKeywords
                            ?: emptyList()
                    }
                    addAll(customKeywords)
                }

                if (blockedKeywords.isNotEmpty()) {
                    val eventText = getAllTextFromActiveWindow(packageName)
                    val matchedKeyword = blockedKeywords.firstOrNull { keyword ->
                        val regex = "\\b${Regex.escape(keyword)}\\b".toRegex(RegexOption.IGNORE_CASE)
                        regex.containsMatchIn(eventText)
                    }

                    if (matchedKeyword != null) {
                        recentlyBlockedApps[packageName] = currentTime
                        withContext(Dispatchers.Main) {
                            blockApp(
                                packageName,
                                keywordBlocked = true,
                                blockedKeyword = matchedKeyword
                            )
                        }
                        return@launch
                    }
                }

                // ✅ 2. Category Blocking (Parent vs Child)
                val blockedCategories = if (isParent) {
                    settings.blockedCategories
                } else {
                    settings.childProfiles.find { it.id == settings.activeChildId }?.blockedCategories
                        ?: emptySet()
                }

                if (blockedCategories.isNotEmpty()) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val appCategory = getAppCategory(appInfo, packageManager)

                        if (blockedCategories.contains(appCategory)) {
                            recentlyBlockedApps[packageName] = currentTime
                            withContext(Dispatchers.Main) {
                                blockApp(
                                    packageName,
                                    categoryBlocked = true,
                                    blockedCategory = appCategory
                                )
                            }
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Category detection failed: ${e.message}")
                    }
                }

                // ✅ 3. Permanent Block Check
                val isPermanentlyBlocked = if (isParent) {
                    settings.blockedApps.containsKey(packageName)
                } else {
                    settings.childProfiles.find { it.id == settings.activeChildId }
                        ?.blockedApps?.containsKey(packageName) ?: false
                }

                if (isPermanentlyBlocked) {
                    recentlyBlockedApps[packageName] = currentTime
                    withContext(Dispatchers.Main) {
                        blockApp(packageName, permanentlyBlocked = true)
                    }
                    return@launch
                }

                // ✅ 4. Time-Limit Check
                val limitMinutes = if (isParent) {
                    settings.parentAppTimeLimits[packageName] ?: 0
                } else {
                    settings.childProfiles.find { it.id == settings.activeChildId }
                        ?.appTimeLimits?.get(packageName) ?: 0
                }

                if (limitMinutes > 0) {
                    val usageStatsManager =
                        getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                    val usedMinutes = AppLimits.getTodayUsageMinutes(packageName, usageStatsManager)

                    if (usedMinutes >= limitMinutes) {
                        recentlyBlockedApps[packageName] = currentTime
                        withContext(Dispatchers.Main) {
                            blockApp(packageName, limitMinutes = limitMinutes)
                        }
                    }
                }

                recentlyBlockedApps.entries.removeAll { currentTime - it.value > 60000 }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking limits: ${e.message}")
            }
        }
    }

    private fun getAllTextFromActiveWindow(currentPackage: String): String {
        val rootNode = rootInActiveWindow ?: return ""
        val sb = StringBuilder()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.packageName == currentPackage) {
                node.text?.let { sb.append(it).append(" ") }
                for (i in 0 until node.childCount) {
                    traverse(node.getChild(i))
                }
            }
        }
        traverse(rootNode)
        return sb.toString()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun blockApp(
        packageName: String,
        permanentlyBlocked: Boolean = false,
        usedMinutes: Int = 0,
        limitMinutes: Int = 0,
        keywordBlocked: Boolean = false,
        blockedKeyword: String = "",
        categoryBlocked: Boolean = false,
        blockedCategory: String = ""
    ) {
        try {
            val appName = getAppName(packageName)
            when {
                keywordBlocked -> {
                    showKeywordBlockNotification(packageName, appName, blockedKeyword)
                }
                categoryBlocked -> {
                    showCategoryBlockNotification(packageName, appName, blockedCategory)
                }
                permanentlyBlocked -> {
                    showPermanentBlockNotification(packageName, appName)
                }
                else -> {
                    showTimeLimitNotification(packageName, appName, usedMinutes, limitMinutes)
                }
            }
            performGlobalAction(GLOBAL_ACTION_HOME)
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

    // ✅ Notifications
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showServiceActiveNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AppMonitoringService.CHANNEL_ID)
            .setContentTitle("✅ App Monitor Running")
            .setContentText("Monitoring apps for limits, categories, and keywords.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        NotificationManagerCompat.from(this).notify(999, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showTimeLimitNotification(
        packageName: String,
        appName: String,
        usedMinutes: Int,
        limitMinutes: Int
    ) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AppMonitoringService.LIMIT_CHANNEL_ID)
            .setContentTitle("⏰ Time Limit Reached")
            .setContentText("$appName blocked (Used: ${usedMinutes}m / Limit: ${limitMinutes}m)")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(packageName.hashCode(), notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showPermanentBlockNotification(packageName: String, appName: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AppMonitoringService.LIMIT_CHANNEL_ID)
            .setContentTitle("🚫 Permanently Blocked")
            .setContentText("$appName is permanently blocked.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(packageName.hashCode(), notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showKeywordBlockNotification(packageName: String, appName: String, keyword: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AppMonitoringService.LIMIT_CHANNEL_ID)
            .setContentTitle("🔒 Keyword Blocked")
            .setContentText("$appName blocked (keyword: \"$keyword\")")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(packageName.hashCode(), notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showCategoryBlockNotification(packageName: String, appName: String, category: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AppMonitoringService.LIMIT_CHANNEL_ID)
            .setContentTitle("📂 Category Blocked")
            .setContentText("$appName blocked (Category: $category)")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(packageName.hashCode(), notification)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility Service destroyed")
    }

    companion object {
        const val CATEGORY_SOCIAL = "Social Networking"
        const val CATEGORY_PRODUCTIVITY = "Education/Business"
        const val CATEGORY_GAME = "Game"
        const val CATEGORY_ENTERTAINMENT = "Entertainment"
        const val CATEGORY_FAMILY = "Family"
        const val CATEGORY_HEALTH = "Health & Fitness"
        const val CATEGORY_UTILITY = "Utility"
    }
}

// ✅ Util: Detect App Category
fun getAppCategory(appInfo: ApplicationInfo, pm: PackageManager): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        when (appInfo.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> "Social Networking"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Education/Business"
            ApplicationInfo.CATEGORY_GAME -> "Game"
            else -> {
                val pkg = appInfo.packageName.lowercase()
                when {
                    "video" in pkg || "music" in pkg || "youtube" in pkg -> "Entertainment"
                    "family" in pkg || "kids" in pkg -> "Family"
                    "health" in pkg || "fitness" in pkg || "workout" in pkg -> "Health & Fitness"
                    else -> "Utility"
                }
            }
        }
    } else {
        val pkg = appInfo.packageName.lowercase()
        when {
            "facebook" in pkg || "whatsapp" in pkg || "twitter" in pkg || "instagram" in pkg -> "Social Networking"
            "game" in pkg -> "Game"
            "edu" in pkg || "school" in pkg -> "Education/Business"
            "video" in pkg || "music" in pkg || "youtube" in pkg -> "Entertainment"
            "family" in pkg || "kids" in pkg -> "Family"
            "health" in pkg || "fitness" in pkg || "workout" in pkg -> "Health & Fitness"
            else -> "Utility"
        }
    }
}
