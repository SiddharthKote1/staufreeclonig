package passwordScreens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.v02.ReelsBlockingService.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ChangePinScreen(viewModel: MainViewModel, requireCurrent: Boolean, onDone: () -> Unit) {
    var curPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    // 👁 Eye toggle states
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (requireCurrent) "Change Parent PIN" else "Set New Parent PIN",
            style = MaterialTheme.typography.titleLarge
        )

        if (requireCurrent) {
            OutlinedTextField(
                value = curPin,
                onValueChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) curPin = it
                },
                label = { Text("Current PIN") },
                singleLine = true,
                visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showCurrent = !showCurrent }) {
                        Icon(
                            if (showCurrent) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
        }

        OutlinedTextField(
            value = newPin,
            onValueChange = {
                if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it
            },
            label = { Text("New PIN (4 digits)") },
            singleLine = true,
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showNew = !showNew }) {
                    Icon(
                        if (showNew) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle visibility"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )

        OutlinedTextField(
            value = confirm,
            onValueChange = {
                if (it.length <= 4 && it.all { c -> c.isDigit() }) confirm = it
            },
            label = { Text("Confirm PIN") },
            singleLine = true,
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(
                        if (showConfirm) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle visibility"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )

        Button(
            onClick = {
                scope.launch {
                    // ✅ Empty & validation checks
                    if (requireCurrent && curPin.length != 4) {
                        Toast.makeText(context, "Enter current 4-digit PIN", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (newPin.length != 4 || confirm.length != 4) {
                        Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (newPin != confirm) {
                        Toast.makeText(context, "PINs do not match", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (requireCurrent) {
                        val stored = viewModel.pinCode.first()
                        if (curPin != stored) {
                            Toast.makeText(context, "Incorrect current PIN", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }
                    // ✅ Save
                    viewModel.setPinCode(newPin)
                    Toast.makeText(context, "Success! PIN changed.", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }
        ) {
            Text("Save PIN")
        }
    }
}
