package com.example.v02.timelimit

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import kotlin.collections.iterator

object AppLimits {
    private lateinit var sharedPreferences: SharedPreferences
    private val limits = mutableMapOf<String, Int>()

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("app_limits", Context.MODE_PRIVATE)
        loadLimits()
    }

    fun setLimit(packageName: String, limitMinutes: Int) {
        limits[packageName] = limitMinutes
    }

    fun getLimit(packageName: String): Int {
        return limits[packageName] ?: 0
    }

    fun removeLimit(packageName: String) {
        limits.remove(packageName)
    }

    fun getAllLimits(): Map<String, Int> {
        return limits.toMap()
    }

    fun saveLimits() {
        if (::sharedPreferences.isInitialized) {
            val editor = sharedPreferences.edit()
            editor.clear()
            for ((packageName, limit) in limits) {
                editor.putInt(packageName, limit)
            }
            editor.apply()
        }
    }

    private fun loadLimits() {
        if (::sharedPreferences.isInitialized) {
            limits.clear()
            val allEntries = sharedPreferences.all
            for ((key, value) in allEntries) {
                if (value is Int) {
                    limits[key] = value
                }
            }
        }
    }

    fun getUsageStats(usageStatsManager: UsageStatsManager): List<AppUsageStats> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        // Set to start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return usageStats.map { stats ->
            AppUsageStats(
                packageName = stats.packageName,
                totalTimeInForeground = stats.totalTimeInForeground
            )
        }.filter { it.totalTimeInForeground > 0 }
    }

    fun isAppLimitExceeded(packageName: String, usageStatsManager: UsageStatsManager): Boolean {
        val limit = getLimit(packageName)
        if (limit <= 0) return false

        val usageMinutes = getTodayUsageMinutes(packageName, usageStatsManager)
        return usageMinutes >= limit
    }

    fun getTodayUsageMinutes(packageName: String, usageStatsManager: UsageStatsManager): Long {
        val usageStats = getUsageStats(usageStatsManager)
        val appUsage = usageStats.find { it.packageName == packageName }
        return (appUsage?.totalTimeInForeground ?: 0) / (1000 * 60)
    }
}

data class AppUsageStats(
    val packageName: String,
    val totalTimeInForeground: Long
)
