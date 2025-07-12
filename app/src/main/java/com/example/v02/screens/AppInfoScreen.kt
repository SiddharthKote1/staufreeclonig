package com.example.v02.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.v02.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    navController : NavController
){
    val context = LocalContext.current
    var showDialog by remember {mutableStateOf(false)}
    var age by remember {mutableStateOf(15f)}
    var dateOfBirth by remember { mutableStateOf("") }
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val currentYear = calendar.get(Calendar.YEAR)


    val datePicker = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            dateOfBirth = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        },
        year, month, day
    ).apply {
        datePicker.maxDate = calendar.timeInMillis
    }

    val isAdult = remember(dateOfBirth) {
        if (dateOfBirth.isNotEmpty()) {
            val yearOfBirth = dateOfBirth.split("/").last().toIntOrNull() ?: 0
            (currentYear - yearOfBirth) >= 18
        } else {
            false
        }
    }
    Box(
        modifier=Modifier.fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFB0BEC5),
                        Color(0xFF81D4FA)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(
                    id = R.drawable.rightsiz
                ),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "StayFree is absolutely free to use with zero hidden fees. " +
                            "Your data and your privacy are our first concerns — " +
                            "we don't collect personal information or share your " +
                            "usage stats with anyone. Use all features securely and safely.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 5.dp),
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Column {
                    Text(
                        text = "StayFree allows you to keep track of and control your" +
                                " screen time by measuring app usage and providing " +
                                "reminders. It promotes healthier phone " +
                                "behaviors so you can be focused and balanced during your day.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text("Select your Date of Birth",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start)
                        .padding(start=20.dp,end=20.dp),
                    fontSize = 20.sp)
                Box(modifier=Modifier.fillMaxWidth()
                    .padding(start=10.dp,end=10.dp)
                    .clickable{ datePicker.show()}) {
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = {
                        },
                        label = { Text("Select your date of birth") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        readOnly = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE1F5FE),
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color(0xFF0288D1),
                            unfocusedIndicatorColor = Color(0xFF90A4AE),
                            focusedLabelColor = Color(0xFF0288D1),
                            unfocusedLabelColor = Color(0xFF607D8B),
                            cursorColor = Color(0xFF0288D1)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { datePicker.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select DOB")
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Row(modifier=Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center){

                    Button(
                        onClick = {
                            when {
                                dateOfBirth.isEmpty() -> {
                                    Toast.makeText(
                                        context,
                                        "Please select your date of birth",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                !isAdult -> {
                                    Toast.makeText(
                                        context,
                                        "You must be 18 or older to continue",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                else -> {
                                    navController.navigate("usage_permission")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color(0xFF00ACC1)
                        )
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewAppInfoScreen() {
    val navController = rememberNavController()
    AppInfoScreen(navController)
}