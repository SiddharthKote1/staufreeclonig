package com.example.v02.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.*
import com.example.v02.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ScaffoldScreen(navController: NavController) {
    var expanded by remember { mutableStateOf(false) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animationlock))

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Blocking controls") }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 20.dp), // ✅ Adjusted for Bottom Bar
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp) // ✅ Nice spacing between FABs
            ) {
                // ✅ FAB Option 1 (Keyword Block)
                AnimatedVisibility(
                    visible = expanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate("Keyword_Block") },
                        text = { Text("Block Keywords") },
                        icon = {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "Block Keywords"
                            )
                        },
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    )
                }

                // ✅ FAB Option 2 (App Blocking)
                AnimatedVisibility(
                    visible = expanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate("Main_Screen") },
                        text = { Text("App Blocking") },
                        icon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "App Blocking"
                            )
                        },
                        elevation = FloatingActionButtonDefaults.elevation(6.dp)
                    )
                }

                ExtendedFloatingActionButton(
                    onClick = { expanded = !expanded },
                    text = {
                        Text(if (expanded) "Close" else "Add Blocking")
                    },
                    icon = {
                        Icon(
                            imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Menu"
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )

            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val progress by animateLottieCompositionAsState(
                composition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(300.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScaffoldScreenPreview() {
    ScaffoldScreen(navController = rememberNavController())
}
