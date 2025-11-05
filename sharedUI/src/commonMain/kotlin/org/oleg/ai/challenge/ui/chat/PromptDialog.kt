package org.oleg.ai.challenge.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PromptDialog(
    visible: Boolean,
    systemPrompt: String,
    assistantPrompt: String,
    onDismiss: () -> Unit,
    onSave: (systemPrompt: String, assistantPrompt: String) -> Unit,
    onClear: () -> Unit
) {
    if (!visible) return

    var localSystemPrompt by remember { mutableStateOf(systemPrompt) }
    var localAssistantPrompt by remember { mutableStateOf(assistantPrompt) }

    // Update local state when props change
    LaunchedEffect(systemPrompt, assistantPrompt) {
        localSystemPrompt = systemPrompt
        localAssistantPrompt = assistantPrompt
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configure Prompts"
            )
        },
        title = {
            Text("Configure AI Prompts")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // System Prompt Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "System Prompt",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = localSystemPrompt,
                        onValueChange = { localSystemPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Enter system prompt...") },
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                // Assistant Prompt Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Assistant Prompt",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = localAssistantPrompt,
                        onValueChange = { localAssistantPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Enter assistant prompt...") },
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                // Clear All Button
                TextButton(
                    onClick = {
                        localSystemPrompt = ""
                        localAssistantPrompt = ""
                        onClear()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(localSystemPrompt, localAssistantPrompt)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
