package org.oleg.ai.challenge.component.chat

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

class PreviewChatComponent(
    override val messages: Value<List<ChatMessage>> = MutableValue(
        listOf(
            ChatMessage(id = "1", text = "Hello!", isFromUser = true),
            ChatMessage(id = "2", text = "Hi there! How can I help you?", isFromUser = false),
            ChatMessage(id = "3", text = "This is a preview", isFromUser = true)
        )
    ),
    override val inputText: Value<String> = MutableValue(""),
    override val isLoading: Value<Boolean> = MutableValue(false)
) : ChatComponent {
    override fun onTextChanged(text: String) = Unit
    override fun onSendMessage() = Unit
    override fun onNavigateBack() = Unit
}
