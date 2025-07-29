package com.example.v02

import BlockSelectionScreen
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.v02.ReelsBlockingService.MainViewModel
import com.example.v02.screens.BlockPermanentScreen
import com.example.v02.timelimit.Screens.*
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = "apps",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("apps") {
            AppUsageScreen(navController = navController, viewModel = mainViewModel)
        }
        composable("limits") {
            AppLimitsScreen(viewModel = mainViewModel)
        }
        composable("set_limit/{packageName}/{appName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            val appName = backStackEntry.arguments?.getString("appName") ?: ""
            SetLimitScreen(
                packageName = packageName,
                appName = appName,
                navController = navController,
                viewModel = mainViewModel
            )
        }
        composable("block_selection/{packageName}/{appName}") { backStackEntry ->
            val packageName = Uri.decode(backStackEntry.arguments?.getString("packageName") ?: "")
            val appName = Uri.decode(backStackEntry.arguments?.getString("appName") ?: "")
            BlockSelectionScreen(
                navController = navController,
                packageName = packageName,
                appName = appName
            )
        }
        composable("block_permanent/{packageName}/{appName}") { backStackEntry ->
            val packageName = Uri.decode(backStackEntry.arguments?.getString("packageName") ?: "")
            val appName = Uri.decode(backStackEntry.arguments?.getString("appName") ?: "")
            BlockPermanentScreen(
                navController = navController,
                packageName = packageName,
                appName = appName,
                viewModel = mainViewModel
            )
        }

        // ✅ Category screen navigation
        composable("category_apps/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            var apps by remember { mutableStateOf(emptyList<AppStatsItem>()) }

            LaunchedEffect(category) {
                coroutineScope.launch {
                    val grouped = loadAppsGroupedByCategory(context)
                    apps = grouped[category] ?: emptyList()
                }
            }

            CategoryAppsScreen(navController = navController, category = category, apps = apps)
        }
    }
}



data class TabItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)
