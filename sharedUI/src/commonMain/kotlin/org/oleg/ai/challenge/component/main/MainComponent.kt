package org.oleg.ai.challenge.component.main

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.component.agentcreation.AgentCreationComponent
import org.oleg.ai.challenge.component.chat.ChatComponent
import org.oleg.ai.challenge.data.model.Conversation

/**
 * Main component for the split-screen UI.
 *
 * Left pane: Chat list
 * Right pane: Agent creation (when creating new chat) OR Chat screen (when chat is selected)
 */
interface MainComponent {

    // State for chat list (left pane)
    val chatList: Value<List<Conversation>>
    val selectedChatId: Value<Long>  // -1L means no selection

    // Right pane child slot (managed by Decompose)
    val rightPaneChild: Value<ChildSlot<*, RightPaneChild>>

    // Events
    fun onCreateNewChat()
    fun onSelectChat(chatId: Long)
    fun onDeleteChat(chatId: Long)
    fun onChatCreated()  // Called when agent creation is completed
    fun onBackFromAgentCreation()  // Called when user cancels agent creation

    companion object {
        const val NO_SELECTION = -1L
    }

    /**
     * Represents the child component displayed in the right pane.
     */
    sealed class RightPaneChild {
        /**
         * Agent creation screen for creating a new chat.
         */
        data class AgentCreation(val component: AgentCreationComponent) : RightPaneChild()

        /**
         * Chat screen for an existing chat.
         */
        data class Chat(val component: ChatComponent) : RightPaneChild()

        /**
         * Empty/placeholder state (no chat selected).
         */
        data object Empty : RightPaneChild()
    }
}
