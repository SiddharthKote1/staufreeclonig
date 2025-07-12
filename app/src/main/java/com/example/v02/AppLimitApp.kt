package com.example.v02

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.v02.timelimit.AppMonitoringService
import com.example.v02.timelimit.Screens.PermissionScreen
import kotlinx.coroutines.delay

@Composable
fun AppLimitApp() {
    val context = LocalContext.current
    var hasUsagePermission by remember { mutableStateOf(false) }
    var hasAccessibilityPermission by remember { mutableStateOf(false) }
    var isCheckingPermission by remember { mutableStateOf(true) }

    val hasAllPermissions = hasUsagePermission && hasAccessibilityPermission

    // Check permissions periodically when on permission screen
    LaunchedEffect(hasAllPermissions) {
        while (!hasAllPermissions) {
            val activity = context as? MainActivity
            if (activity != null) {
                Log.d("AppLimitApp", "Checking permissions...")
                val (usage, accessibility) = activity.getPermissionStatus()
                Log.d("AppLimitApp", "Usage permission: $usage, Accessibility permission: $accessibility")
                hasUsagePermission = usage
                hasAccessibilityPermission = accessibility
            }
            isCheckingPermission = false

            if (hasAllPermissions) {
                Log.d("AppLimitApp", "All permissions granted! Starting monitoring service...")
                // Start monitoring service when permissions are granted
                AppMonitoringService.start(context)
                break
            }

            delay(2000) // Check every 2 seconds (increased from 1 second)
        }
    }

    if (hasAllPermissions) {
        MainScreen()
    } else {
        PermissionScreen(
            isCheckingPermission = isCheckingPermission,
            hasUsagePermission = hasUsagePermission,
            hasAccessibilityPermission = hasAccessibilityPermission,
            onRequestUsagePermission = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
            },
            onRequestAccessibilityPermission = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )
    }
}
