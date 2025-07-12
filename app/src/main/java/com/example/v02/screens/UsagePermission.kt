package com.example.v02.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.v02.navigation.BottomNavItem

@Composable
fun UsagePermission(navController: NavController, function: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // State variables to recompose UI if permissions change
    var usageAccessGranted by remember { mutableStateOf(isUsageAccessGranted(context)) }
    var accessibilityEnabled by remember { mutableStateOf(isAccessibilityEnabled(context)) }
    val allPermissionsGranted = usageAccessGranted && accessibilityEnabled

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _: LifecycleOwner, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageAccessGranted = isUsageAccessGranted(context)
                accessibilityEnabled = isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFB0BEC5), Color(0xFF81D4FA))
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Permissions Required",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(10.dp))

            // Usage Access Permission Section
            Column {
                Text(
                    text = "1. Usage Access Permission",
                    color = Color(0xFF0D47A1),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "To track your screen time and help control app usage, StayFree requires Usage Access permission. Please allow it to proceed.",
                    color = Color(0xFF0D47A1),
                    fontSize = 15.sp
                )

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "2.Grant Usage Access", color = Color.White)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Accessibility Permission Section
            Column {
                Text(
                    text = "Accessibility Permission",
                    color = Color(0xFF1B5E20),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "To detect volume button presses and enable advanced controls, StayFree requires Accessibility permission. Please enable it in settings.",
                    color = Color(0xFF1B5E20),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Grant Accessibility Access", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = {
                    if (allPermissionsGranted) {
                        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                            .edit {
                                putBoolean("setup_completed", true)
                            }

                        navController.navigate(BottomNavItem.UsageStats.route) {
                            popUpTo("started_screen") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        Toast.makeText(context, "Please grant all permissions", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allPermissionsGranted) Color(0xFF00ACC1) else Color.Gray
                ),
                enabled = allPermissionsGranted
            ) {
                Text("Continue")
            }

        }
    }
}

fun isUsageAccessGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun isAccessibilityEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return !enabledServices.isNullOrEmpty() && enabledServices.contains(context.packageName)
}
