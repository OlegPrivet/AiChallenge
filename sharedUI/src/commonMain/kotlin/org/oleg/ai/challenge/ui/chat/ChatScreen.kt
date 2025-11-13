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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
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
import org.oleg.ai.challenge.component.chat.ChatMessage
import org.oleg.ai.challenge.component.chat.PreviewChatComponent
import org.oleg.ai.challenge.data.model.Agent
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

    val lazyListState: LazyListState = rememberLazyListState()
    val isSummarizeVisibility by remember(isLoading) {
        derivedStateOf {
            !isLoading && messages.any { it.isVisibleInUI }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Chat") },
                navigationIcon = {
                    IconButton(onClick = component::onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
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
            )
        }
    ) { paddingValues ->
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
            if (currentAgentModel.isNotEmpty()) {
                ModelSelector(
                    selectedModel = currentAgentModel,
                    onModelSelected = { component.onModelChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    label = "Current AI Model"
                )
            }

            // Temperature Slider
            TemperatureSlider(
                temperature = currentTemperature,
                onTemperatureChange = component::onTemperatureChanged,
                modifier = Modifier.fillMaxWidth()
            )

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
                    ChatMessageItem(message)
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
                        LoadingIndicator()
                    }
                }
            }
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                lazyListState.animateScrollToItem(0)
            }
        }
    }
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
private fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .wrapContentSize(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "AI is thinking...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
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
                containerColor = if (message.isFromUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Show agent/model badge for AI messages
                if (!message.isFromUser) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
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
            }
        }
    }
}

@Composable
private fun TokenUsageDisplay(
    usage: Usage,
    modifier: Modifier = Modifier
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
