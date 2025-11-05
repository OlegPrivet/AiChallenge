package org.oleg.ai.challenge.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PromptSetupDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onStartChat: (systemPrompt: String, assistantPrompt: String) -> Unit
) {
    if (!visible) return

    var systemPrompt by remember { mutableStateOf("") }
    var assistantPrompt by remember { mutableStateOf("""
        {
            "header": "Заголовок ответа",
            "content":"Строка с ответом"
        }
    """.trimIndent()) }

    // Reset local state when dialog is shown
    LaunchedEffect(visible) {
        if (visible) {
            systemPrompt = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Setup Prompts"
            )
        },
        title = {
            Text("Setup AI Prompts")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure how the AI should behave in this chat session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // System Prompt Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "System Prompt *",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Enter system prompt (required)...") },
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        isError = systemPrompt.isEmpty()
                    )
                }

                // Assistant Prompt Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Assistant Prompt (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = assistantPrompt,
                        onValueChange = { assistantPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        enabled = false
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStartChat(systemPrompt, assistantPrompt)
                },
            ) {
                Text("Start Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
