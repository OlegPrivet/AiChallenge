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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.oleg.ai.challenge.component.chat.ChatComponent
import org.oleg.ai.challenge.component.chat.ChatMessage
import org.oleg.ai.challenge.component.chat.PreviewChatComponent
import org.oleg.ai.challenge.theme.AppTheme
import org.oleg.ai.challenge.util.JsonFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    component: ChatComponent,
    modifier: Modifier = Modifier,
) {
    val messages by component.messages.subscribeAsState()
    val inputText by component.inputText.subscribeAsState()
    val isLoading by component.isLoading.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Chat") },
                navigationIcon = {
                    IconButton(onClick = { component.onNavigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    val systemPrompt by component.systemPrompt.subscribeAsState()
                    val assistantPrompt by component.assistantPrompt.subscribeAsState()
                    val hasActivePrompts by remember(systemPrompt, assistantPrompt) {
                        derivedStateOf {
                            systemPrompt.isNotEmpty() || assistantPrompt.isNotEmpty()
                        }
                    }

                    IconButton(onClick = { component.onShowPromptDialog() }) {
                        BadgedBox(
                            badge = {
                                if (hasActivePrompts) {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
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
            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                // Show loading indicator at the top of the list (bottom visually due to reverseLayout)
                if (isLoading) {
                    item(key = "loading_indicator") {
                        LoadingIndicator()
                    }
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
                    onValueChange = { component.onTextChanged(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    maxLines = 3,
                    enabled = !isLoading
                )

                IconButton(
                    onClick = { component.onSendMessage() },
                    enabled = inputText.isNotBlank() && !isLoading
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }

    PromptDialog(
        visible = component.isPromptDialogVisible.subscribeAsState().value,
        systemPrompt = component.systemPrompt.subscribeAsState().value,
        assistantPrompt = component.assistantPrompt.subscribeAsState().value,
        onDismiss = component::onDismissPromptDialog,
        onSave = component::onSavePrompts,
        onClear = component::onClearPrompts
    )
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
    val isJsonMessage = remember(message.text) {
        JsonFormatter.isJson(message.text)
    }

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
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = if (isJsonMessage) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    MaterialTheme.typography.bodyMedium
                }
            )
        }
    }
}

@Preview
@Composable
private fun ChatScreenPreview() {
    AppTheme {
        ChatScreen(PreviewChatComponent())
    }
}
