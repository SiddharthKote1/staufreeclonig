package com.example.v02.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object UsageStats : BottomNavItem(
        route = "usage_stats",
        title = "Usage Stats",
        icon = Icons.Default.BarChart
    )

    object InAppBlocking : BottomNavItem(
        route = "in_app_blocking",
        title = "App Blocking",
        icon = Icons.Default.Block
    )
/*
    object TimeLimits : BottomNavItem(
        route = "time_limits",
        title = "Time Limits",
        icon = Icons.Default.Timer
    )
}
*/
object BlockingControls : BottomNavItem(
    route = "blocking_controls",
    title = "Blocking Controls",
    icon = Icons.Default.Timer // Or Icons.Default.Settings if you want
)
}