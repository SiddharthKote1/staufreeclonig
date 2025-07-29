package com.example.v02.timelimit.Screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun loadAppsGroupedByCategory(context: Context): Map<String, List<AppStatsItem>> {
    return withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val appsByCategory = mutableMapOf<String, MutableList<AppStatsItem>>()

        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in installedApps) {
            try {
                if (packageManager.getLaunchIntentForPackage(app.packageName) == null) continue

                val appName = packageManager.getApplicationLabel(app).toString()
                val icon = packageManager.getApplicationIcon(app)
                val category = getAppCategory(app, packageManager)

                val item = AppStatsItem(
                    packageName = app.packageName,
                    appName = appName,
                    icon = icon,
                    usageTime = 0L
                )
                appsByCategory.getOrPut(category) { mutableListOf() }.add(item)
            } catch (_: Exception) {
            }
        }
        appsByCategory
    }
}

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
