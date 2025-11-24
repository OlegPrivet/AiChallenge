package org.oleg.ai.challenge.component.main

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.Conversation

class PreviewMainComponent : MainComponent {
    override val chatList: Value<List<Conversation>> = MutableValue(emptyList())
    override val selectedChatId: Value<Long> = MutableValue(MainComponent.NO_SELECTION)
    override val rightPaneChild: Value<ChildSlot<*, MainComponent.RightPaneChild>> = MutableValue(
        ChildSlot<Any, MainComponent.RightPaneChild>()
    )

    override fun onCreateNewChat() = Unit
    override fun onSelectChat(chatId: Long) = Unit
    override fun onDeleteChat(chatId: Long) = Unit
    override fun onChatCreated() = Unit
    override fun onBackFromAgentCreation() = Unit
    override fun onNavigateToMcp() = Unit
    override fun onNavigateToPlanner() = Unit
    override fun onNavigateToDocuments() = Unit
    override fun onNavigateToRagSettings() = Unit
    override fun onNavigateToStatistics() = Unit
}
