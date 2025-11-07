package org.oleg.ai.challenge.ui.agentcreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.oleg.ai.challenge.component.agentcreation.AgentCreationComponent
import org.oleg.ai.challenge.data.model.Agent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentCreationScreen(
    component: AgentCreationComponent,
    modifier: Modifier = Modifier
) {
    val mainSystemPrompt by component.mainSystemPrompt.subscribeAsState()
    val mainAssistantPrompt by component.mainAssistantPrompt.subscribeAsState()
    val selectedModel by component.selectedModel.subscribeAsState()
    val subAgents by component.subAgents.subscribeAsState()
    val showSubAgentDialog by component.showSubAgentDialog.subscribeAsState()
    val editingSubAgentIndex by component.editingSubAgentIndex.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Agent Configuration") },
                navigationIcon = {
                    IconButton(onClick = { component.onNavigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { component.onAddSubAgent() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Sub-Agent")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Chat Configuration Section
            item {
                Text(
                    text = "Main Chat Configuration",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Text(
                    text = "Configure the main AI agent settings for this chat session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Model Selector
            item {
                ModelSelector(
                    selectedModel = selectedModel,
                    onModelSelected = { component.onModelSelected(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // System Prompt
            item {
                OutlinedTextField(
                    value = mainSystemPrompt,
                    onValueChange = { component.onMainSystemPromptChanged(it) },
                    label = { Text("System Prompt (Optional)") },
                    placeholder = { Text("Define how the AI should behave...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }

            // Assistant Prompt
            item {
                OutlinedTextField(
                    value = mainAssistantPrompt,
                    onValueChange = { component.onMainAssistantPromptChanged(it) },
                    label = { Text("Assistant Prompt (Optional)") },
                    placeholder = { Text("Define the response format...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }

            // Sub-Agents Section
            item {
                Text(
                    text = "Sub-Agents (${subAgents.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item {
                Text(
                    text = "Add specialized agents for specific tasks. Each can have its own prompts and model.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sub-Agents List
            items(subAgents, key = { it.id }) { agent ->
                SubAgentCard(
                    agent = agent,
                    onEdit = { component.onEditSubAgent(agent) },
                    onDelete = { component.onDeleteSubAgent(agent.id) }
                )
            }

            // Start Chat Button
            item {
                Button(
                    onClick = { component.onStartChat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Start Chat")
                }
            }
        }
    }

    // Sub-Agent Dialog
    val editingAgent = if (editingSubAgentIndex >= 0 && editingSubAgentIndex < subAgents.size) {
        subAgents[editingSubAgentIndex]
    } else {
        null
    }

    SubAgentDialog(
        visible = showSubAgentDialog,
        editingAgent = editingAgent,
        onDismiss = { component.onSubAgentDialogDismiss() },
        onConfirm = { name, systemPrompt, assistantPrompt, model ->
            component.onSubAgentDialogConfirm(name, systemPrompt, assistantPrompt, model)
        }
    )
}

@Composable
private fun SubAgentCard(
    agent: Agent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = agent.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Model: ${agent.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                if (agent.systemPrompt != null) {
                    Text(
                        text = "System prompt configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                if (agent.assistantPrompt != null) {
                    Text(
                        text = "Assistant prompt configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
