package com.example.v02

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.v02.timelimit.Screens.AppLimitsScreen
import com.example.v02.timelimit.Screens.AppUsageScreen
import com.example.v02.timelimit.Screens.SetLimitScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        TabItem("Apps", Icons.Default.Apps),
        TabItem("Limits", Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.height(48.dp) // Optional: reduce height
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            navController.navigate(
                                when (index) {
                                    0 -> "apps"
                                    1 -> "limits"
                                    else -> "apps"
                                }
                            ) {
                                popUpTo("apps") { inclusive = false }
                            }
                        },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "apps",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // ⬅️ important to avoid being overlapped by tabs
        ) {
            composable("apps") {
                selectedTab = 0
                AppUsageScreen(navController = navController)
            }
            composable("limits") {
                selectedTab = 1
                AppLimitsScreen()
            }
            composable("set_limit/{packageName}/{appName}") { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                val appName = backStackEntry.arguments?.getString("appName") ?: ""
                SetLimitScreen(
                    packageName = packageName,
                    appName = appName,
                    navController = navController
                )
            }
        }
    }
}


data class TabItem(
    val title: String,
    val icon: ImageVector
)