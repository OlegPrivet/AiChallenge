package org.oleg.ai.challenge.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.oleg.ai.challenge.component.main.MainComponent
import org.oleg.ai.challenge.ui.agentcreation.AgentCreationScreen
import org.oleg.ai.challenge.ui.chat.ChatScreen

/**
 * Main screen with split-screen layout.
 *
 * Left pane (30%): Chat list
 * Right pane (70%): Agent creation or Chat screen
 */
@Composable
fun MainScreen(
    component: MainComponent,
    modifier: Modifier = Modifier
) {
    val chatList by component.chatList.subscribeAsState()
    val selectedChatId by component.selectedChatId.subscribeAsState()
    val rightPaneChild by component.rightPaneChild.subscribeAsState()

    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // Left pane: Chat list (30% width)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            ChatListPanel(
                chatList = chatList,
                selectedChatId = selectedChatId,
                onCreateChat = component::onCreateNewChat,
                onChatClick = component::onSelectChat,
                onDeleteChat = component::onDeleteChat
            )
        }

        // Divider
        VerticalDivider()

        // Right pane: Content based on state (70% width)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            // ChildSlot returns ChildSlot.Child? where child contains instance
            val slotChild = rightPaneChild.child

            if (slotChild != null) {
                when (val instance = slotChild.instance) {
                    is MainComponent.RightPaneChild.AgentCreation -> {
                        // Show agent creation screen
                        AgentCreationScreen(
                            component = instance.component
                        )
                    }

                    is MainComponent.RightPaneChild.Chat -> {
                        // Show chat screen for selected chat
                        ChatScreen(
                            component = instance.component
                        )
                    }

                    is MainComponent.RightPaneChild.Empty -> {
                        // Show empty state / placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a chat or create a new one",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // No child active - show empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a chat or create a new one",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
