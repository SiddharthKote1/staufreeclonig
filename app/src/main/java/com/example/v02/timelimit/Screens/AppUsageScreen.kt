package com.example.v02.timelimit.Screens

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.v02.ReelsBlockingService.MainViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun AppUsageScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    var appStats by remember { mutableStateOf<List<AppStatsItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val limits by viewModel.getAppTimeLimits().collectAsState(initial = emptyMap())

    // Auto-refresh every 5 seconds
    LaunchedEffect(limits) {
        while (true) {
            loadAppUsageStats(context) { stats ->
                appStats = stats
                isLoading = false
            }
            delay(5000) // Refresh every 5 seconds
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading app usage statistics...")
                }
            }
        } else if (appStats.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No usage data available", fontSize = 16.sp)
                    Text("Use your apps and check back later", fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(appStats) { appStat ->
                    AppUsageItem(
                        appStat = appStat,
                        limitMinutes = limits[appStat.packageName] ?: 0,
                        onClick = {
                            if (appStat.packageName != context.packageName) {
                                val encodedPackageName = Uri.encode(appStat.packageName)
                                val encodedAppName = Uri.encode(appStat.appName)
                                navController.navigate("set_limit/$encodedPackageName/$encodedAppName")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppUsageItem(
    appStat: AppStatsItem,
    limitMinutes: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isCurrentApp = appStat.packageName == context.packageName
    val usageMinutes = appStat.usageTime / (1000 * 60)
    val isOverLimit = limitMinutes > 0 && usageMinutes > limitMinutes

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrentApp) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = if (isOverLimit) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberDrawablePainter(appStat.icon),
                contentDescription = appStat.appName,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStat.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Today: ${formatTime(appStat.usageTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isCurrentApp) {
                    Text(
                        text = "Cannot set limit for this app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (limitMinutes > 0) {
                    val remaining = limitMinutes - usageMinutes
                    if (remaining > 0) {
                        Text(
                            text = "Remaining: ${remaining}m (limit: ${limitMinutes}m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIMIT EXCEEDED! (${limitMinutes}m limit)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Tap to set limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!isCurrentApp) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Set limit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class AppStatsItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val usageTime: Long
)

private suspend fun loadAppUsageStats(
    context: Context,
    onResult: (List<AppStatsItem>) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val packageManager = context.packageManager

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60 * 60 * 24 // last 24h

            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val filteredStats = usageStatsList
                .filter { it.totalTimeInForeground > 5 * 1000 }
                .mapNotNull { stats ->
                    try {
                        val appInfo = launcherApps.getApplicationInfo(
                            stats.packageName, 0, Process.myUserHandle()
                        )
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val icon = packageManager.getApplicationIcon(appInfo)

                        AppStatsItem(
                            packageName = stats.packageName,
                            appName = appName,
                            icon = icon,
                            usageTime = stats.totalTimeInForeground
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedByDescending { it.usageTime }

            withContext(Dispatchers.Main) {
                onResult(filteredStats)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(emptyList())
            }
        }
    }
}

private fun formatTime(timeInMillis: Long): String {
    val hours = timeInMillis / (1000 * 60 * 60)
    val minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60)
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}
