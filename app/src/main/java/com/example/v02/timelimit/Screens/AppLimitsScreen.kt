package com.example.v02.timelimit.Screens

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.example.v02.timelimit.AppLimits
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
fun AppLimitsScreen() {
    val context = LocalContext.current
    var limitedApps by remember { mutableStateOf<List<LimitedAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        limitedApps = loadLimitedApps(context)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "App Time Limits",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading app limits...")
                }
            }
        } else if (limitedApps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    Text(
                        text = "No app limits set",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Go to Apps tab to set limits",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(limitedApps) { limitedApp ->
                    LimitedAppItem(
                        limitedApp = limitedApp,
                        onRemoveLimit = {
                            AppLimits.removeLimit(limitedApp.packageName)
                            AppLimits.saveLimits()
                            scope.launch {
                                limitedApps = loadLimitedApps(context)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LimitedAppItem(
    limitedApp: LimitedAppItem,
    onRemoveLimit: () -> Unit
) {

    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberDrawablePainter(limitedApp.icon),
                contentDescription = limitedApp.appName,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = limitedApp.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Daily limit: ${limitedApp.limitMinutes} minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                val context = LocalContext.current
                val usageStatsManager = remember {
                    context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                }
                val usageStats = remember { AppLimits.getUsageStats(usageStatsManager) }
                val currentUsage = usageStats.find { it.packageName == limitedApp.packageName }

                currentUsage?.let { usage ->
                    val usedMinutes = usage.totalTimeInForeground / (1000 * 60)
                    val remaining = limitedApp.limitMinutes - usedMinutes

                    if (remaining > 0) {
                        Text(
                            text = "${remaining}m remaining today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Over limit by ${-remaining}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove limit",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Time Limit") },
            text = {
                Text("Are you sure you want to remove the time limit for ${limitedApp.appName}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveLimit()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class LimitedAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val limitMinutes: Int
)

private suspend fun loadLimitedApps(context: Context): List<LimitedAppItem> =
    withContext(Dispatchers.IO) {
        try {
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val packageManager = context.packageManager

            val limits = AppLimits.getAllLimits()

            limits.mapNotNull { (packageName, limitMinutes) ->
                try {
                    val applicationInfo = launcherApps.getApplicationInfo(
                        packageName, 0, Process.myUserHandle()
                    )
                    val appName = packageManager.getApplicationLabel(applicationInfo).toString()
                    val icon = packageManager.getApplicationIcon(applicationInfo)

                    LimitedAppItem(
                        packageName = packageName,
                        appName = appName,
                        icon = icon,
                        limitMinutes = limitMinutes
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.appName }
        } catch (e: Exception) {
            emptyList()
        }
    }