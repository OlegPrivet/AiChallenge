package org.oleg.ai.challenge.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.oleg.ai.challenge.component.chat.ChatComponent
import org.oleg.ai.challenge.component.chat.PreviewChatComponent
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.McpProcessingPhase
import org.oleg.ai.challenge.data.model.McpUiState
import org.oleg.ai.challenge.data.network.model.Usage
import org.oleg.ai.challenge.theme.AppTheme
import org.oleg.ai.challenge.ui.agentcreation.ModelSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    component: ChatComponent,
    modifier: Modifier = Modifier,
) {
    val messages by component.messages.subscribeAsState()
    val inputText by component.inputText.subscribeAsState()
    val isLoading by component.isLoading.subscribeAsState()
    val availableAgents by component.availableAgents.subscribeAsState()
    val selectedAgent by component.selectedAgent.subscribeAsState()
    val currentAgentModel by component.currentAgentModel.subscribeAsState()
    val currentTemperature by component.currentTemperature.subscribeAsState()
    val mcpUiState by component.mcpUiState.subscribeAsState()
    val isRagEnabled by component.isRagEnabled.subscribeAsState()
    val isDeveloperModeEnabled by component.isDeveloperModeEnabled.subscribeAsState()

    val selectedCitationSource = component.getSelectedCitationSource()

    val lazyListState: LazyListState = rememberLazyListState()
    val isSummarizeVisibility by remember(isLoading) {
        derivedStateOf {
            !isLoading && messages.any { it.isVisibleInUI }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Agent Selector
            if (availableAgents.size > 1) {
                AgentSelector(
                    availableAgents = availableAgents,
                    selectedAgent = selectedAgent,
                    onAgentSelected = component::onAgentSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Model Selector
            ModelSelector(
                selectedModel = currentAgentModel,
                onModelSelected = { component.onModelChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = "Current AI Model"
            )

            // Temperature Slider
            TemperatureSlider(
                temperature = currentTemperature,
                onTemperatureChange = component::onTemperatureChanged,
                modifier = Modifier.fillMaxWidth()
            )

            // RAG mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isRagEnabled,
                    onClick = { component.onToggleRagMode(!isRagEnabled) },
                    label = { Text(if (isRagEnabled) "RAG: ON" else "RAG: OFF") },
                    leadingIcon = {
                        Text(if (isRagEnabled) "🔍" else "💬")
                    }
                )

                if (isRagEnabled) {
                    FilterChip(
                        selected = isDeveloperModeEnabled,
                        onClick = { component.onToggleDeveloperMode(!isDeveloperModeEnabled) },
                        label = { Text("Dev Mode") }
                    )
                }
            }

            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = lazyListState,
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                items(
                    messages.filter { it.isVisibleInUI }.reversed(),
                    key = { it.id }
                ) { message ->
                    ChatMessageItem(
                        message = message,
                        component = component,
                        isDeveloperModeEnabled = isDeveloperModeEnabled
                    )
                }
            }

            // Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = component::onTextChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    maxLines = 3,
                    enabled = !isLoading
                )

                Box {
                    IconButton(
                        onClick = { component.onSendMessage() },
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send"
                        )
                    }
                    if (isLoading) {
                        LoadingIndicator(mcpUiState = mcpUiState)
                    }
                }
                IconButton(
                    onClick = component::onSummarizeConversation,
                    enabled = isSummarizeVisibility
                ) {
                    Text(
                        text = "📝",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Source Modal
        selectedCitationSource?.let { source ->
            SourceModal(
                citationDetail = source,
                onDismiss = component::onHideSource
            )
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                lazyListState.animateScrollToItem(0)
            }
        }
    }
}

@Composable
private fun SourceModal(
    citationDetail: org.oleg.ai.challenge.component.chat.CitationSourceDetail,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Source [${citationDetail.citation.index}]")
                citationDetail.citation.sourceTitle?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        text = citationDetail.chunkContent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AgentSelector(
    availableAgents: List<Agent>,
    selectedAgent: Agent,
    onAgentSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Agent:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableAgents.forEach { agent ->
                val isSelected = selectedAgent == agent
                FilterChip(
                    selected = isSelected,
                    onClick = { onAgentSelected(agent.id) },
                    label = {
                        Text(
                            text = if (agent.id == "main") "Main Agent" else agent.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator(mcpUiState: McpUiState = McpUiState()) {
    Row(
        modifier = Modifier
            .wrapContentSize(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (mcpUiState.isMcpRunning)
                    MaterialTheme.colorScheme.tertiaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (mcpUiState.isMcpRunning)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.secondary
                    )

                    // Show MCP-specific status or default AI thinking message
                    val statusText = when {
                        mcpUiState.isMcpRunning -> {
                            when (mcpUiState.processingPhase) {
                                McpProcessingPhase.Validating -> "Validating..."
                                McpProcessingPhase.InvokingTool -> {
                                    mcpUiState.currentToolName?.let { "Calling tool: $it" }
                                        ?: "Invoking MCP tool..."
                                }

                                McpProcessingPhase.GeneratingFinalResponse -> "Generating response..."
                                McpProcessingPhase.Retrying -> "Retrying (${mcpUiState.retryCount})..."
                                McpProcessingPhase.Idle -> "MCP processing..."
                            }
                        }

                        else -> "AI is thinking..."
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Show tool name badge when MCP is running
                    if (mcpUiState.isMcpRunning && mcpUiState.currentToolName != null) {
                        Text(
                            text = "Tool: ${mcpUiState.currentToolName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    component: ChatComponent,
    isDeveloperModeEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(
                start = if (message.isFromUser) 100.dp else 0.dp,
                end = if (message.isFromUser) 0.dp else 100.dp,
            ),
        horizontalArrangement = if (message.isFromUser)
            Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.wrapContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else if (message.citations != null && message.citations.isNotEmpty()) {
                    // RAG-powered messages have a distinct color
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Show agent/model badge for AI messages
                if (!message.isFromUser) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        // RAG indicator
                        if (message.citations != null && message.citations.isNotEmpty()) {
                            Text(
                                text = "🔍 RAG",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        // Agent name badge (if sub-agent)
                        message.agentName?.let { agentId ->
                            if (agentId != "main") {
                                Text(
                                    text = "Agent: $agentId",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Magenta
                                )
                            }
                        }
                        // Model badge
                        message.modelUsed?.let { model ->
                            Text(
                                text = "Model: $model",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Magenta
                            )
                        }
                    }
                }

                // Token usage display for AI messages
                if (!message.isFromUser && message.usage != null) {
                    TokenUsageDisplay(
                        usage = message.usage,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Message content
                SelectionContainer {
                    Markdown(
                        content = message.text,
                        modifier = Modifier.align(if (message.isFromUser) Alignment.End else Alignment.Start)
                    )
                }

                // Citations display
                if (message.citations != null && message.citations.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sources:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )

                        message.citations.forEach { citation ->
                            androidx.compose.material3.AssistChip(
                                onClick = {
                                    // Get chunk content from retrieval trace
                                    val chunkContent = message.retrievalTrace?.results
                                        ?.getOrNull(citation.index - 1)?.chunk?.content
                                        ?: "Content not available"
                                    component.onShowSource(citation, chunkContent)
                                },
                                label = { Text("[${citation.index}]") }
                            )
                        }
                    }
                }

                // Developer mode: Retrieval trace
                if (isDeveloperModeEnabled && message.retrievalTrace != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Retrieval Trace (Dev Mode):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Query: ${message.retrievalTrace.query}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Retrieved: ${message.retrievalTrace.results.size} chunks (Top-${message.retrievalTrace.topK})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                        )
                        message.retrievalTrace.results.take(3).forEachIndexed { idx, result ->
                            Text(
                                text = "  [${idx + 1}] score: ${(result.score * 1000).toInt() / 1000.0} - ${result.chunk.content.take(100)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenUsageDisplay(
    usage: Usage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tokens:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = "${usage.promptTokens} in",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = "/",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
        )
        Text(
            text = "${usage.completionTokens} out",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = "/",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
        )
        Text(
            text = "${usage.totalTokens} total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Preview
@Composable
private fun ChatScreenPreview() {
    AppTheme {
        ChatScreen(PreviewChatComponent())
    }
}
