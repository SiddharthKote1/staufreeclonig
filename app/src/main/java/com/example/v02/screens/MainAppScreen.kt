package com.example.v02.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.v02.MainScreen
import com.example.v02.ReelsBlockingService.ChildProfile
import com.example.v02.ReelsBlockingService.MainViewModel
import com.example.v02.navigation.BottomNavItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import passwordScreens.AccountSwitcherDialog
import passwordScreens.ChangePinScreen
import passwordScreens.SetRecoveryQAScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val accountMode by viewModel.accountMode.collectAsState(initial = "Parent")
    val isParent = accountMode == "Parent"

    val hasPin by viewModel.hasPin.collectAsState(initial = false)
    val hasQA by viewModel.hasSecretQA.collectAsState(initial = false)

    val expandMenu = remember { mutableStateOf(false) }
    val showSwitchDialog = remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    val setupCompleted = remember { mutableStateOf(prefs.getBoolean("setup_completed", false)) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val fullScreenRoutes = listOf("started_screen", "app_info", "usage_permission")
    val showBottomBar = remember { mutableStateOf(false) }

    val childProfiles by viewModel.childProfiles.collectAsState(initial = emptyList())
    val activeChildId by viewModel.activeChildId.collectAsState(initial = "")
    val activeChildProfile = childProfiles.find { it.id == activeChildId }

    val isUnlocked = remember { mutableStateOf(isParent) }

    // ✅ States for PIN Dialog
    val showPinDialog = remember { mutableStateOf(false) }
    val pinInput = remember { mutableStateOf("") }
    val pinError = remember { mutableStateOf<String?>(null) }
    val pendingTabToOpen = remember { mutableStateOf<BottomNavItem?>(null) }

    LaunchedEffect(accountMode) {
        isUnlocked.value = isParent
    }

    LaunchedEffect(currentRoute) {
        showBottomBar.value = currentRoute !in fullScreenRoutes
        if (showBottomBar.value) delay(80)
    }

    Box {
        Scaffold(
            topBar = {
                if (currentRoute !in fullScreenRoutes) {
                    TopAppBar(
                        title = { Text("App Blocker") },
                        actions = {
                            IconButton(onClick = { expandMenu.value = true }) {
                                Icon(Icons.Default.Menu, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expandMenu.value,
                                onDismissRequest = { expandMenu.value = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    enabled = isUnlocked.value,
                                    onClick = {
                                        expandMenu.value = false
                                        if (isUnlocked.value) {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Switch Account") },
                                    onClick = {
                                        expandMenu.value = false
                                        showSwitchDialog.value = true
                                    }
                                )
                                if (isUnlocked.value) {
                                    DropdownMenuItem(
                                        text = { Text(if (hasPin) "Change PIN" else "Set PIN") },
                                        onClick = {
                                            expandMenu.value = false
                                            navController.navigate(if (hasPin) "change_pin" else "change_pin_no_current")
                                        }
                                    )
                                    if (!hasQA) {
                                        DropdownMenuItem(
                                            text = { Text("Set Recovery Q/A") },
                                            onClick = {
                                                expandMenu.value = false
                                                navController.navigate("set_qa")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (showBottomBar.value) {
                    NavigationBar {
                        val curDest = navBackStackEntry?.destination
                        listOf(
                            BottomNavItem.UsageStats,
                            BottomNavItem.InAppBlocking,
                            BottomNavItem.TimeLimits
                        ).forEach { tab ->
                            NavigationBarItem(
                                selected = curDest?.hierarchy?.any { it.route == tab.route } == true,
                                onClick = {
                                    if (tab == BottomNavItem.UsageStats || isUnlocked.value) {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } else {
                                        // ✅ Show PIN dialog in Child Mode
                                        showPinDialog.value = true
                                        pinInput.value = ""
                                        pinError.value = null
                                        pendingTabToOpen.value = tab
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (setupCompleted.value) BottomNavItem.UsageStats.route else "started_screen",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("started_screen") { StartedScreen(navController) }
                composable("app_info") { AppInfoScreen(navController) }
                composable("usage_permission") {
                    UsagePermission(navController) {
                        prefs.edit().putBoolean("setup_completed", true).apply()
                        setupCompleted.value = true
                        navController.navigate(BottomNavItem.UsageStats.route) {
                            popUpTo("started_screen") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                composable(BottomNavItem.UsageStats.route) { UsageStatsScreen() }
                composable(BottomNavItem.InAppBlocking.route) { InAppBlockingScreen(viewModel) }
                composable(BottomNavItem.TimeLimits.route) { MainScreen() }
                composable("change_pin") {
                    ChangePinScreen(viewModel, requireCurrent = true) { navController.popBackStack() }
                }
                composable("change_pin_no_current") {
                    ChangePinScreen(viewModel, requireCurrent = false) { navController.popBackStack() }
                }
                composable("reset_pin") {
                    ChangePinScreen(viewModel, requireCurrent = false) { navController.popBackStack() }
                }
                composable("set_qa") {
                    SetRecoveryQAScreen(viewModel) {
                        navController.popBackStack()
                        Toast.makeText(context, "Recovery Question Set!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // ✅ Parent PIN Dialog for Child Mode
        if (showPinDialog.value) {
            AlertDialog(
                onDismissRequest = { showPinDialog.value = false },
                title = { Text("Enter Parent PIN") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pinInput.value,
                            onValueChange = { pinInput.value = it; pinError.value = null },
                            label = { Text("PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        pinError.value?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val correct = viewModel.pinCode.first() == pinInput.value
                            if (correct) {
                                isUnlocked.value = true
                                showPinDialog.value = false
                                pendingTabToOpen.value?.let { tab ->
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } else {
                                pinError.value = "Incorrect PIN"
                            }
                        }
                    }) { Text("Unlock") }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog.value = false }) { Text("Cancel") }
                }
            )
        }

        if (showSwitchDialog.value) {
            AccountSwitcherDialog(
                currentMode = accountMode,
                currentProfile = activeChildProfile,
                childProfiles = childProfiles,
                hasPin = hasPin,
                savedRecoveryQuestion = viewModel.secretQuestion.collectAsState(initial = "").value,
                verifyParentPin = { enteredPin -> viewModel.pinCode.first() == enteredPin },
                verifyRecoveryAnswer = { answer -> viewModel.isSecretAnswerCorrect(answer) },
                onResetParentPin = { newPin ->
                    scope.launch {
                        viewModel.setPinCode(newPin)
                        Toast.makeText(context, "Parent PIN Reset Successfully", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddOrUpdateChild = { scope.launch { viewModel.addOrUpdateChild(it) } },
                onDeleteChild = { scope.launch { viewModel.deleteChild(it) } },
                onSwitchToParent = {
                    scope.launch {
                        viewModel.setAccountMode("Parent")
                        isUnlocked.value = true
                    }
                },
                onSwitchToChild = { profile ->
                    scope.launch {
                        val hasPinSet = viewModel.hasPin.first()
                        val hasQASet = viewModel.hasSecretQA.first()

                        when {
                            !hasPinSet -> {
                                Toast.makeText(context, "Please set a Parent PIN before switching to Child mode", Toast.LENGTH_LONG).show()
                                showSwitchDialog.value = false
                                navController.navigate("change_pin_no_current")
                            }

                            !hasQASet -> {
                                Toast.makeText(context, "Please set Recovery Q/A before switching to Child mode", Toast.LENGTH_LONG).show()
                                showSwitchDialog.value = false
                                navController.navigate("set_qa")
                            }

                            else -> {
                                viewModel.setActiveChild(profile.id)
                                viewModel.setAccountMode("Child")
                                isUnlocked.value = false
                                showSwitchDialog.value = false
                            }
                        }
                    }
                },
                onDismiss = { showSwitchDialog.value = false }
            )
        }
    }
}

