package com.example.v02.screens

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

data class AppUsageInfo(
    val appName: String,
    val packageName: String,
    val usageTime: Long,
    val icon: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen() {
    val context = LocalContext.current
    var appUsageList by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRange by remember { mutableStateOf("Last 15 Days") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val rangeOptions = listOf("Today", "Yesterday", "Last 15 Days", "Month")

    // ✅ Handle Back Button (Close keyboard first)
    BackHandler(enabled = isSearchFocused) {
        focusManager.clearFocus()
        isSearchFocused = false
    }

    // Load Data
    LaunchedEffect(selectedRange) {
        if (!hasUsageStatsPermission(context)) {
            Toast.makeText(context, "Please grant Usage Access Permission", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return@LaunchedEffect
        }

        isLoading = true
        appUsageList = when (selectedRange) {
            "Today" -> getAppUsageStats(context, 0)
            "Yesterday" -> getAppUsageStats(context, 1)
            "Last 15 Days" -> getAppUsageStats(context, 15)
            "Month" -> getAppUsageStats(context, 30)
            else -> emptyList()
        }
        isLoading = false
    }

    val filteredList = remember(appUsageList, searchQuery) {
        if (searchQuery.isBlank()) appUsageList
        else appUsageList.filter {
            it.appName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    isSearchFocused = false
                })
            }
    ) {
        Text(
            text = "App Usage Statistics",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search apps") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isSearchFocused = focusState.isFocused
                },
            singleLine = true,
            shape= RoundedCornerShape(4.dp)
        )

        if (!isLoading && filteredList.isNotEmpty()) {
            val totalTime = filteredList.sumOf { it.usageTime }
            val topApp = filteredList.maxByOrNull { it.usageTime }

            // ✅ Dropdown
            Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.CenterStart) {
                OutlinedButton(
                    onClick = { dropdownExpanded = !dropdownExpanded },
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(selectedRange)
                    Icon(
                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    rangeOptions.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range) },
                            onClick = {
                                selectedRange = range
                                dropdownExpanded = false
                                focusManager.clearFocus()
                                isSearchFocused = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))


            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Screen Time", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        formatUsageTime(totalTime),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${filteredList.size} apps used", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        topApp?.let {
                            Text("Top: ${it.appName}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredList, key = { it.packageName }) { appUsage ->
                    AppUsageCard(appUsage = appUsage)
                }
            }
        }
    }
}

@Composable
fun AppUsageCard(appUsage: AppUsageInfo) {
    val iconBitmap = remember(appUsage.icon) {
        appUsage.icon?.toBitmap(48, 48)?.asImageBitmap()
    }
    val formattedTime = remember(appUsage.usageTime) {
        formatUsageTime(appUsage.usageTime)
    }

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
            iconBitmap?.let {
                Image(bitmap = it, contentDescription = "${appUsage.appName} icon", modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appUsage.appName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(formattedTime, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

suspend fun getAppUsageStats(context: Context, days: Int): List<AppUsageInfo> =
    withContext(Dispatchers.Default) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        if (days == 0) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, -days)
        }

        val startTime = calendar.timeInMillis

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val aggregatedStats = usageStats
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .mapValues { it.value.sumOf { it.totalTimeInForeground } }

        aggregatedStats.entries
            .sortedByDescending { it.value }
            .take(50)
            .mapNotNull { (packageName, totalUsageTime) ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(packageName)
                    AppUsageInfo(appName, packageName, totalUsageTime, icon)
                } catch (e: Exception) {
                    null
                }
            }
    }

fun formatUsageTime(timeInMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
