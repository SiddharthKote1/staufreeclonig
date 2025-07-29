package com.example.v02.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.v02.ReelsBlockingService.MainViewModel

@Composable
fun BlockPermanentScreen(
    navController: NavController,
    packageName: String,
    appName: String,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // ✅ UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Block Permanently",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Do you want to completely block $appName?",
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Confirm Permanent Block")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cancel")
        }
    }

    // ✅ Confirmation Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Block") },
            text = { Text("Are you sure you want to permanently block $appName?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAppPermanentlyBlocked(packageName, true)
                        Toast.makeText(
                            context,
                            "$appName permanently blocked",
                            Toast.LENGTH_SHORT
                        ).show()
                        showDialog = false
                        navController.popBackStack() // go back after blocking
                    }
                ) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}
