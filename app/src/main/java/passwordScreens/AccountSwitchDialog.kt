import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.v02.ReelsBlockingService.ChildProfile
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun AccountSwitcherDialog(
    currentMode: String,
    currentProfile: ChildProfile?,
    childProfiles: List<ChildProfile>,
    onAddOrUpdateChild: (ChildProfile) -> Unit,
    onDeleteChild: (String) -> Unit,
    verifyParentPin: suspend (String) -> Boolean,
    onSwitchToParent: () -> Unit,
    onSwitchToChild: (ChildProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editorName by remember { mutableStateOf(TextFieldValue("")) }
    var editingProfile by remember { mutableStateOf<ChildProfile?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Switch Account", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text("Close") }
            }

            Spacer(Modifier.height(12.dp))

            // Current user preview
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (currentMode == "Parent") "Parent Mode" else currentProfile?.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(24.dp))

            // Account list (only show in Parent Mode)
            if (currentMode == "Parent") {
                Text("Accounts:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                childProfiles.forEach { profile ->
                    val isActive = profile.id == currentProfile?.id

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSwitchToChild(profile)
                                onDismiss()
                            }
                            .padding(vertical = 6.dp)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(profile.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                editorName = TextFieldValue(profile.name)
                                editingProfile = profile
                                showEditor = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                onDeleteChild(profile.id)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Add Account Button
                Button(
                    onClick = {
                        editorName = TextFieldValue("")
                        editingProfile = null
                        showEditor = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Account")
                }
            } else {
                // Child Mode: Switch to Parent Mode
                Text("Tap below to switch to Parent Mode", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        pinError = null
                    },
                    label = { Text("Enter Parent PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                pinError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val isCorrect = verifyParentPin(pinInput)
                            if (isCorrect) {
                                onSwitchToParent()
                                onDismiss()
                            } else {
                                pinError = "Incorrect PIN"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White
                    )
                ) {
                    Text("Switch to Parent Account")
                }
            }

            // Add / Edit profile inline UI
            if (showEditor) {
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = editorName,
                    onValueChange = { editorName = it },
                    label = { Text("Child Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showEditor = false }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val updated = ChildProfile(
                            id = editingProfile?.id ?: UUID.randomUUID().toString(),
                            name = editorName.text.trim()
                        )
                        onAddOrUpdateChild(updated)
                        showEditor = false
                    }) {
                        Text(if (editingProfile == null) "Add" else "Update")
                    }
                }
            }
        }
    }
}

