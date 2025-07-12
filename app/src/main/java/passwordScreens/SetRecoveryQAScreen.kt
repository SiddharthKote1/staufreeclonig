package passwordScreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.v02.ReelsBlockingService.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun SetRecoveryQAScreen(viewModel: MainViewModel, onDone: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Set Recovery Question",
            style = MaterialTheme.typography.titleLarge,
            modifier=Modifier.align(Alignment.CenterHorizontally))
        OutlinedTextField(q, { q = it; err = null }, label = { Text("Secret Question") }, modifier = Modifier.fillMaxWidth(),)
        OutlinedTextField(a, { a = it; err = null }, label = { Text("Answer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            if (q.isBlank() || a.isBlank()) {
                err = "Both fields required"
            } else scope.launch {
                viewModel.setSecretQA(q, a)
                onDone()
            }
        }, ){
            Text("Save") }
    }
}

