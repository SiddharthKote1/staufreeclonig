package com.example.v02

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.v02.ReelsBlockingService.MainViewModel
import com.example.v02.screens.MainAppScreen
import com.example.v02.timelimit.AppMonitoringService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = MainViewModel(application = application)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainAppScreen(viewModel)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // Check permissions again when app resumes
        lifecycleScope.launch {
            delay(500) // Small delay to ensure settings are applied
            if (hasRequiredPermissions()) {
                AppMonitoringService.start(this@MainActivity)
            }
        }
    }
    fun hasRequiredPermissions(): Boolean {
        return hasUsageStatsPermission() && isAccessibilityServiceEnabled()
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

            val expectedServiceName = ComponentName(this, "com.example.v02.timelimit.AppBlockerAccessibilityService")

            // Method 1: Check using enabled services list
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

            Log.d("MainActivity", "Checking ${enabledServices.size} enabled accessibility services")

            for (serviceInfo in enabledServices) {
                val serviceId = serviceInfo.resolveInfo.serviceInfo
                val serviceName = "${serviceId.packageName}/${serviceId.name}"
                Log.d("MainActivity", "Found enabled service: $serviceName")

                if (serviceId.packageName == packageName &&
                    serviceId.name == "com.example.v02.timelimit.AppBlockerAccessibilityService") {
                    Log.d("MainActivity", "✅ Found our accessibility service via method 1")
                    return true
                }
            }

            // Method 2: Check using Settings.Secure (more reliable)
            val enabledServicesSetting = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            Log.d("MainActivity", "Enabled services setting: $enabledServicesSetting")

            if (enabledServicesSetting.isNullOrEmpty()) {
                Log.d("MainActivity", "No enabled services found in settings")
                return false
            }

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                Log.d("MainActivity", "Checking service: $componentNameString")

                val componentName = ComponentName.unflattenFromString(componentNameString)
                if (componentName != null) {
                    if (componentName.packageName == packageName &&
                        componentName.className == "com.example.v02.timelimit.AppBlockerAccessibilityService") {
                        Log.d("MainActivity", "✅ Found our accessibility service via method 2")
                        return true
                    }

                    // Also check with just the class name (sometimes the full path is used)
                    if (componentName.packageName == packageName &&
                        componentName.className.endsWith("AppBlockerAccessibilityService")) {
                        Log.d("MainActivity", "✅ Found our accessibility service via method 2 (partial match)")
                        return true
                    }
                }
            }

            Log.d("MainActivity", "❌ Accessibility service not found")
            false
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking accessibility service: ${e.message}")
            false
        }
    }

    fun getPermissionStatus(): Pair<Boolean, Boolean> {
        return Pair(hasUsageStatsPermission(), isAccessibilityServiceEnabled())
    }
}

