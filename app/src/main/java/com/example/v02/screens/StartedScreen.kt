package com.example.v02.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.v02.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartedScreen(
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFF5F5F5), // Light grey
                        Color(0xFFE0E0E0)  // Slightly darker grey
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ Keeping your original hourglass size
            Image(
                painter = painterResource(id = R.drawable.hourglass),
                contentDescription = null,
                modifier = Modifier.size(400.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ✅ Same FocusGuard Text but better contrast
                Text(
                    text = "FocusGuard",
                    color = Color(0xFF212121), // Dark grey instead of white
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Use your time efficiently " +
                            " and carefully",
                    color = Color(0xFF424242), // Slightly lighter dark grey
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "With FocusGuard",
                    color = Color(0xFF616161), // Softer grey
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // ✅ Discover Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color(0xFF00ACC1), // Same teal as button
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Discover your time spent across apps",
                    color = Color(0xFF212121),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Block Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFD84315), // Slight reddish-orange (warning vibe)
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Block the apps that distract you",
                    color = Color(0xFF212121),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        navController.navigate("app_info")
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.White,
                        containerColor = Color(0xFF00ACC1)
                    )
                ) {
                    Text("Get Started")
                }
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StartedScreenPreview() {
    StartedScreen(navController = rememberNavController())
}
