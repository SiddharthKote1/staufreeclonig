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
import com.example.v02.ReelsBlockingService.MainViewModel
import com.example.v02.timelimit.AppLimits
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppLimitsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val appTimeLimits by viewModel.getAppTimeLimits().collectAsState(initial = emptyMap())
    val permanentlyBlockedApps by viewModel.permanentlyBlockedApps.collectAsState(initial = emptySet())
    val blockedCategories by viewModel.blockedCategories.collectAsState(initial = emptySet())

    var limitedApps by remember { mutableStateOf<List<LimitedAppItem>>(emptyList()) }
    var blockedApps by remember { mutableStateOf<List<BlockedAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(appTimeLimits, permanentlyBlockedApps) {
        isLoading = true
        limitedApps = loadLimitedAppsFromLimits(context, appTimeLimits)
        blockedApps = loadBlockedApps(context, permanentlyBlockedApps)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "App Restrictions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            limitedApps.isEmpty() && blockedApps.isEmpty() && blockedCategories.isEmpty() -> {
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
                        Text(
                            text = "No app or category restrictions set",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // ✅ Blocked Categories Section
                    if (blockedCategories.isNotEmpty()) {
                        item {
                            Text(
                                text = "Blocked Categories",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(blockedCategories.toList()) { category ->
                            BlockedCategoryItem(
                                category = category,
                                onUnblock = {
                                    scope.launch {
                                        viewModel.setCategoryBlocked(category, false)
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // ✅ Time-Limited Apps Section
                    if (limitedApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "Time-Limited Apps",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(limitedApps) { limitedApp ->
                            LimitedAppItem(
                                limitedApp = limitedApp,
                                onRemoveLimit = {
                                    scope.launch {
                                        viewModel.setAppTimeLimit(limitedApp.packageName, 0)
                                    }
                                }
                            )
                        }
                    }

                    // ✅ Permanently Blocked Apps Section
                    if (blockedApps.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Permanently Blocked Apps",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(blockedApps) { blockedApp ->
                            BlockedAppItem(
                                blockedApp = blockedApp,
                                onUnblock = {
                                    scope.launch {
                                        viewModel.setAppPermanentlyBlocked(blockedApp.packageName, false)
                                    }
                                }
                            )
                        }
                    }
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

            Column(modifier = Modifier.weight(1f)) {
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
                val usageStatsManager =
                    context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
            text = { Text("Remove the time limit for ${limitedApp.appName}?") },
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

@Composable
fun BlockedAppItem(blockedApp: BlockedAppItem, onUnblock: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

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
                painter = rememberDrawablePainter(blockedApp.icon),
                contentDescription = blockedApp.appName,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(blockedApp.appName, fontWeight = FontWeight.Medium)
                Text(
                    "Blocked Permanently",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
            OutlinedButton(onClick = { showDialog = true }) {
                Text("Unblock")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Unblock App") },
            text = { Text("Unblock ${blockedApp.appName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onUnblock()
                        showDialog = false
                    }
                ) {
                    Text("Unblock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BlockedCategoryItem(category: String, onUnblock: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

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
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category, fontWeight = FontWeight.Medium)
                Text(
                    "Category Blocked",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
            OutlinedButton(onClick = { showDialog = true }) {
                Text("Unblock")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Unblock Category") },
            text = { Text("Do you want to unblock $category?") },
            confirmButton = {
                Button(
                    onClick = {
                        onUnblock()
                        showDialog = false
                    }
                ) {
                    Text("Unblock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
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

data class BlockedAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)

private suspend fun loadLimitedAppsFromLimits(
    context: Context,
    limits: Map<String, Int>
): List<LimitedAppItem> = withContext(Dispatchers.IO) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val packageManager = context.packageManager

    limits.mapNotNull { (packageName, limitMinutes) ->
        try {
            val appInfo =
                launcherApps.getApplicationInfo(packageName, 0, Process.myUserHandle())
            LimitedAppItem(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                icon = packageManager.getApplicationIcon(appInfo),
                limitMinutes = limitMinutes
            )
        } catch (e: Exception) {
            null
        }
    }.sortedBy { it.appName }
}

private suspend fun loadBlockedApps(
    context: Context,
    blockedPackages: Set<String>
): List<BlockedAppItem> = withContext(Dispatchers.IO) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val packageManager = context.packageManager

    blockedPackages.mapNotNull { packageName ->
        try {
            val appInfo =
                launcherApps.getApplicationInfo(packageName, 0, Process.myUserHandle())
            BlockedAppItem(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                icon = packageManager.getApplicationIcon(appInfo)
            )
        } catch (e: Exception) {
            null
        }
    }.sortedBy { it.appName }
}
