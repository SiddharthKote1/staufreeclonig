package passwordScreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.v02.ReelsBlockingService.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@Composable
fun ChangePinScreen(viewModel: MainViewModel, requireCurrent: Boolean, onDone: () -> Unit) {
    var curPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (requireCurrent) "Change Parent PIN" else "Set New Parent PIN", style = MaterialTheme.typography.titleLarge)
        if (requireCurrent) {
            OutlinedTextField(curPin, { curPin = it; msg = null }, label = { Text("Current PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        }
        OutlinedTextField(newPin, { newPin = it; msg = null }, label = { Text("New PIN (≥4)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(confirm, { confirm = it; msg = null }, label = { Text("Confirm PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
        msg?.let { Text(it, color = if (it.startsWith("Success")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error) }
        Button(onClick = {
            scope.launch {
                if (newPin.length < 4) {
                    msg = "New PIN must be ≥4 digits"; return@launch
                }
                if (newPin != confirm) {
                    msg = "PINs do not match"; return@launch
                }
                if (requireCurrent) {
                    val stored = viewModel.pinCode.first()
                    if (curPin != stored) {
                        msg = "Incorrect current PIN"; return@launch
                    }
                }
                viewModel.setPinCode(newPin)
                msg = "Success! PIN changed."
                onDone()
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save PIN") }
    }
}