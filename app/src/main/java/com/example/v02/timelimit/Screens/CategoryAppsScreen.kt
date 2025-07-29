package com.example.v02.timelimit.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun CategoryAppsScreen(
    navController: NavController,
    category: String,
    apps: List<AppStatsItem>
) {
    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    val allSelected = selectedApps.size == apps.size

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            text = "Select Apps - $category",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(8.dp)
        )

        // ✅ Select All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = allSelected,
                onCheckedChange = {
                    selectedApps = if (it) apps.map { a -> a.packageName }.toSet() else emptySet()
                }
            )
            Text("Select All")
        }

        // ✅ Apps List
        LazyColumn {
            items(apps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            selectedApps = if (selectedApps.contains(app.packageName)) {
                                selectedApps - app.packageName
                            } else {
                                selectedApps + app.packageName
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedApps.contains(app.packageName),
                        onCheckedChange = {
                            selectedApps = if (it) selectedApps + app.packageName else selectedApps - app.packageName
                        }
                    )
                    Image(
                        painter = rememberDrawablePainter(app.icon),
                        contentDescription = app.appName,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(app.appName)
                }
            }
        }

        // ✅ Select Button
        Button(
            onClick = {
                // Handle your selected apps here
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text("Select (${selectedApps.size})")
        }
    }
}

