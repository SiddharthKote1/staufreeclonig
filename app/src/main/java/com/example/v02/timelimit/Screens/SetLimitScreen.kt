package com.example.v02.timelimit.Screens

import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.v02.ReelsBlockingService.MainViewModel
import com.example.v02.screens.ScaffoldScreen
import com.example.v02.timelimit.NumberPicker
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetLimitScreen(
    packageName: String,
    appName: String,
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val decodedPackageName = Uri.decode(packageName)
    val decodedAppName = Uri.decode(appName)

    var appIcon by remember { mutableStateOf<Drawable?>(null) }
    var selectedMinutes by remember { mutableIntStateOf(30) }

    val timeLimits by viewModel.getAppTimeLimits().collectAsState(initial = emptyMap())
    val currentLimit = timeLimits[decodedPackageName] ?: 0

    LaunchedEffect(decodedPackageName) {
        selectedMinutes = if (currentLimit > 0) currentLimit else 30

        withContext(Dispatchers.IO) {
            try {
                val launcherApps = context.getSystemService(LauncherApps::class.java)
                val applicationInfo = launcherApps.getApplicationInfo(
                    decodedPackageName, 0, Process.myUserHandle()
                )
                val icon = context.packageManager.getApplicationIcon(applicationInfo)
                withContext(Dispatchers.Main) {
                    appIcon = icon
                }
            } catch (_: Exception) {
                // Ignore missing icons
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Set Time Limit") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    appIcon?.let { icon ->
                        Image(
                            painter = rememberDrawablePainter(icon),
                            contentDescription = decodedAppName,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = decodedAppName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentLimit > 0)
                                "Current limit: $currentLimit minutes"
                            else "No limit set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Picker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Daily Time Limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NumberPicker(
                        value = selectedMinutes,
                        onValueChange = { selectedMinutes = it },
                        range = 1..240
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$selectedMinutes minutes per day",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.setAppTimeLimit(decodedPackageName, selectedMinutes)
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (currentLimit > 0) "Update Limit" else "Set Limit",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (currentLimit > 0) {
                    OutlinedButton(
                        onClick = {
                            viewModel.setAppTimeLimit(decodedPackageName, 0)
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Remove Limit",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

