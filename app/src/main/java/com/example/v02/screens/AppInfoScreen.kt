package com.example.v02.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.v02.R
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppInfoScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    var d1 by remember { mutableStateOf("") }
    var d2 by remember { mutableStateOf("") }
    var d3 by remember { mutableStateOf("") }
    var d4 by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val requester1 = remember { FocusRequester() }
    val requester2 = remember { FocusRequester() }
    val requester3 = remember { FocusRequester() }
    val requester4 = remember { FocusRequester() }


    val birthYear = listOf(d1, d2, d3, d4).joinToString("")
    val isAdult = remember(birthYear) {
        val year = birthYear.toIntOrNull() ?: 0
        (year > 0) && ((currentYear - year) >= 18)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0))
                )
            )

    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imeNestedScroll()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(id = R.drawable.info),
                contentDescription = null,
                modifier = Modifier.size(300.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "StayFree helps you track and control screen time with reminders for healthier phone use. It's free, with no hidden fees, and we value your privacy — no personal data is collected or shared."
                ,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                color = Color.Black,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                "Enter your Year of Birth",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 20.dp, end = 20.dp),
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // 1st Digit
                UnderlinedDigit(
                    value = d1,
                    modifier = Modifier.focusRequester(requester1),
                    onValueChange = {
                        if (it.length <= 1 && it.all { c -> c.isDigit() }) {
                            d1 = it
                            if (it.isNotEmpty()) requester2.requestFocus()
                        }
                    }
                )
                Spacer(modifier = Modifier.width(20.dp))

                // 2nd Digit
                UnderlinedDigit(
                    value = d2,
                    modifier = Modifier.focusRequester(requester2),
                    onValueChange = {
                        if (it.length <= 1 && it.all { c -> c.isDigit() }) {
                            d2 = it
                            if (it.isNotEmpty()) requester3.requestFocus()
                        }
                    },
                    onBackspace = {
                        requester1.requestFocus() // ✅ Back to first when empty + backspace
                    }
                )
                Spacer(modifier = Modifier.width(20.dp))

                // 3rd Digit
                UnderlinedDigit(
                    value = d3,
                    modifier = Modifier.focusRequester(requester3),
                    onValueChange = {
                        if (it.length <= 1 && it.all { c -> c.isDigit() }) {
                            d3 = it
                            if (it.isNotEmpty()) requester4.requestFocus()
                        }
                    },
                    onBackspace = {
                        requester2.requestFocus() // ✅ Back to second
                    }
                )
                Spacer(modifier = Modifier.width(20.dp))

                // 4th Digit
                UnderlinedDigit(
                    value = d4,
                    modifier = Modifier.focusRequester(requester4),
                    onValueChange = {
                        if (it.length <= 1 && it.all { c -> c.isDigit() }) {
                            d4 = it
                            if (it.isNotEmpty()) focusManager.clearFocus()
                        }
                    },
                    onBackspace = {
                        requester3.requestFocus() // ✅ Back to third
                    }
                )
            }



            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    when {
                        birthYear.length < 4 -> {
                            Toast.makeText(
                                context,
                                "Please enter your full birth year",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnderlinedDigit(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onBackspace: (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .width(50.dp)
            .onKeyEvent {
                if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                    value.isEmpty()
                ) {
                    onBackspace?.invoke()
                    true
                } else {
                    false
                }
            },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 22.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Black,
            unfocusedIndicatorColor = Color.Black,
            cursorColor = Color.Black
        )
    )
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PreviewAppInfoScreen() {
    val navController = rememberNavController()
    AppInfoScreen(navController)
}
