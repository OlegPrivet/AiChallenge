package org.oleg.ai.challenge.ui.agentcreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.model.Agent

@Composable
fun SubAgentDialog(
    visible: Boolean,
    editingAgent: Agent? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, systemPrompt: String, assistantPrompt: String, model: String) -> Unit
) {
    if (!visible) return

    var name by remember(editingAgent) { mutableStateOf(editingAgent?.name ?: "") }
    var systemPrompt by remember(editingAgent) { mutableStateOf(editingAgent?.systemPrompt ?: "") }
    var assistantPrompt by remember(editingAgent) { mutableStateOf(editingAgent?.assistantPrompt ?: "") }
    var model by remember(editingAgent) { mutableStateOf(editingAgent?.model ?: BuildConfig.DEFAULT_MODEL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Sub-Agent"
            )
        },
        title = {
            Text(if (editingAgent != null) "Edit Sub-Agent" else "Add Sub-Agent")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configure a specialized agent for specific tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Name field (required)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = name.isBlank()
                )

                // Model selector
                ModelSelector(
                    selectedModel = model,
                    onModelSelected = { model = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // System Prompt (optional)
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Prompt (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                // Assistant Prompt (optional)
                OutlinedTextField(
                    value = assistantPrompt,
                    onValueChange = { assistantPrompt = it },
                    label = { Text("Assistant Prompt (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, systemPrompt, assistantPrompt, model)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (editingAgent != null) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
