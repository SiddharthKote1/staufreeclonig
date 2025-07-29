package com.example.v02.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class AppTimeLimit(
    val appName: String,
    val packageName: String,
    val currentLimit: Int,
    val onLimitChange: (Int) -> Unit
)

@Composable
fun TimeLimitsScreen(navController: NavController) {
    var appTimeLimits by remember {
        mutableStateOf(
            listOf(
                AppTimeLimit(
                    appName = "Instagram",
                    packageName = "com.instagram.android",
                    currentLimit = 30,
                    onLimitChange = { /* Handle limit change */ }
                ),
                AppTimeLimit(
                    appName = "Facebook",
                    packageName = "com.facebook.katana",
                    currentLimit = 45,
                    onLimitChange = { /* Handle limit change */ }
                ),
                AppTimeLimit(
                    appName = "YouTube",
                    packageName = "com.google.android.youtube",
                    currentLimit = 60,
                    onLimitChange = { /* Handle limit change */ }
                ),
                AppTimeLimit(
                    appName = "X (Twitter)",
                    packageName = "com.twitter.android",
                    currentLimit = 20,
                    onLimitChange = { /* Handle limit change */ }
                ),
                AppTimeLimit(
                    appName = "WhatsApp",
                    packageName = "com.whatsapp",
                    currentLimit = 0, // No limit
                    onLimitChange = { /* Handle limit change */ }
                ),
                AppTimeLimit(
                    appName = "Snapchat",
                    packageName = "com.snapchat.android",
                    currentLimit = 25,
                    onLimitChange = { /* Handle limit change */ }
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "App Time Limits",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Set daily time limits for apps",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appTimeLimits) { appLimit ->
                TimeLimitCard(
                    appLimit = appLimit,
                    onLimitChange = { newLimit ->
                        appTimeLimits = appTimeLimits.map {
                            if (it.packageName == appLimit.packageName) {
                                it.copy(currentLimit = newLimit)
                            } else it
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TimeLimitCard(
    appLimit: AppTimeLimit,
    onLimitChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var tempLimit by remember { mutableStateOf(appLimit.currentLimit) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLimit.appName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (appLimit.currentLimit > 0) {
                            "${appLimit.currentLimit} minutes daily"
                        } else {
                            "No limit set"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        tempLimit = appLimit.currentLimit
                        showDialog = true
                    }
                ) {
                    Text("Edit")
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set Time Limit for ${appLimit.appName}") },
            text = {
                Column {
                    Text("Daily time limit (minutes):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempLimit.toString(),
                        onValueChange = {
                            tempLimit = it.toIntOrNull() ?: 0
                        },
                        label = { Text("Minutes") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set to 0 for no limit",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLimitChange(tempLimit)
                        showDialog = false
                    }
                ) {
                    Text("Save")
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