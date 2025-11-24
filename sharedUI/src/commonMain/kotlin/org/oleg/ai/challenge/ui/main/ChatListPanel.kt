package org.oleg.ai.challenge.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.oleg.ai.challenge.data.model.Conversation

/**
 * Left panel UI showing the list of chats with a "Create Chat" button.
 *
 * @param chatList List of conversations to display
 * @param selectedChatId Currently selected chat ID (-1L means no selection)
 * @param onCreateChat Callback when user clicks "Create Chat"
 * @param onChatClick Callback when user selects a chat
 * @param onDeleteChat Callback when user deletes a chat
 * @param onNavigateToMcp Callback when user clicks "MCP Tools"
 * @param onNavigateToPlanner Callback when user clicks "Planner Mode"
 * @param onNavigateToDocuments Callback when user clicks "Documents"
 * @param onNavigateToRagSettings Callback when user clicks "RAG Settings"
 * @param onNavigateToStatistics Callback when user clicks "Statistics"
 * @param modifier Modifier for the panel
 */
@Composable
fun ChatListPanel(
    chatList: List<Conversation>,
    selectedChatId: Long,
    onCreateChat: () -> Unit,
    onChatClick: (Long) -> Unit,
    onDeleteChat: (Long) -> Unit,
    onNavigateToMcp: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToRagSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // "Create Chat" button at the top
        Button(
            onClick = onCreateChat,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Chat"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Chat")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "MCP Tools" button
        Button(
            onClick = onNavigateToMcp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "MCP Tools"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("MCP Tools")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "Planner Mode" button
        Button(
            onClick = onNavigateToPlanner,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Planner Mode"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Planner Mode")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "Documents" button
        Button(
            onClick = onNavigateToDocuments,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Documents"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Documents")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "RAG Settings" button
        Button(
            onClick = onNavigateToRagSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "RAG Settings"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("RAG Settings")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "Statistics" button
        Button(
            onClick = onNavigateToStatistics,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Statistics"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Statistics")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat list
        if (chatList.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No chats yet\nClick \"Create Chat\" to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = chatList,
                    key = { it.chatId }
                ) { chat ->
                    ChatListItem(
                        conversation = chat,
                        isSelected = chat.chatId == selectedChatId,
                        onClick = { onChatClick(chat.chatId) },
                        onDelete = { onDeleteChat(chat.chatId) }
                    )
                }
            }
        }
    }
}

/**
 * Individual chat item in the list.
 */
@Composable
private fun ChatListItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat info (name + timestamp)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conversation.chatName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(conversation.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete chat",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Format timestamp to a human-readable string.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatTimestamp(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    return "${dateTime.date} ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}
