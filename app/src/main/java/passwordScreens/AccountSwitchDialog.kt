package passwordScreens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.v02.ReelsBlockingService.ChildProfile
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherDialog(
    currentMode: String,
    currentProfile: ChildProfile?,
    childProfiles: List<ChildProfile>,
    hasPin: Boolean,
    savedRecoveryQuestion: String,
    verifyParentPin: suspend (String) -> Boolean,
    verifyRecoveryAnswer: suspend (String) -> Boolean,
    onResetParentPin: (String) -> Unit,
    onAddOrUpdateChild: (ChildProfile) -> Unit,
    onDeleteChild: (String) -> Unit,
    onSwitchToParent: () -> Unit,
    onSwitchToChild: (ChildProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEditor by remember { mutableStateOf(false) }
    var editorName by remember { mutableStateOf(TextFieldValue("")) }
    var editingProfile by remember { mutableStateOf<ChildProfile?>(null) }

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var expandedProfileMenu by remember { mutableStateOf<String?>(null) }

    var forgotPasswordMode by remember { mutableStateOf(false) }
    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var allowReset by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ✅ Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentMode == "Parent")
                                "P" else currentProfile?.name?.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (currentMode == "Parent") "Parent Mode"
                        else currentProfile?.name.orEmpty(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(12.dp))
                Divider()

                if (currentMode == "Parent") {
                    // ✅ Child Profiles List
                    Spacer(Modifier.height(12.dp))
                    childProfiles.forEach { profile ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onSwitchToChild(profile) // ✅ No checks here anymore
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(profile.name.firstOrNull()?.uppercase() ?: "?", color = Color.Black)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                    color = Color.Black
                                )
                                Box {
                                    IconButton(onClick = { expandedProfileMenu = profile.id }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                    }
                                    DropdownMenu(
                                        expanded = expandedProfileMenu == profile.id,
                                        onDismissRequest = { expandedProfileMenu = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                expandedProfileMenu = null
                                                editorName = TextFieldValue(profile.name)
                                                editingProfile = profile
                                                showEditor = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                expandedProfileMenu = null
                                                onDeleteChild(profile.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                editorName = TextFieldValue("")
                                editingProfile = null
                                showEditor = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Add account",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    if (!forgotPasswordMode) {
                        Text("Switch to Parent Mode", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it; pinError = null },
                            label = { Text("Enter PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        pinError?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    when {
                                        pinInput.isBlank() -> pinError = "Please enter your PIN"
                                        else -> {
                                            val isCorrect = verifyParentPin(pinInput)
                                            if (isCorrect) {
                                                onSwitchToParent()
                                                onDismiss()
                                            } else pinError = "Incorrect PIN"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Switch to Parent")
                        }
                        TextButton(onClick = {
                            if (savedRecoveryQuestion.isNotBlank()) {
                                forgotPasswordMode = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "No recovery question set",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Text("Forgot Password?")
                        }
                    } else {
                        if (!allowReset) {
                            Text(savedRecoveryQuestion, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = recoveryAnswer,
                                onValueChange = { recoveryAnswer = it; recoveryError = null },
                                label = { Text("Your Answer") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            recoveryError?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val correct = verifyRecoveryAnswer(recoveryAnswer)
                                        if (correct) {
                                            allowReset = true
                                        } else {
                                            recoveryError = "Incorrect Answer"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Verify Answer")
                            }
                        } else {
                            Text("Reset Parent PIN", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPin,
                                onValueChange = { newPin = it },
                                label = { Text("Enter New PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (newPin.isNotBlank()) {
                                        onResetParentPin(newPin)
                                        onSwitchToParent()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset & Switch to Parent")
                            }
                        }
                    }
                }

                // ✅ Add / Edit Child Dialog
                if (showEditor) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editorName,
                        onValueChange = { editorName = it },
                        label = { Text("Child Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showEditor = false }) { Text("Cancel") }
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
    )
}

