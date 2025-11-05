package org.oleg.ai.challenge.component.chat

import com.arkivanov.decompose.value.Value
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ChatComponent {
    val messages: Value<List<ChatMessage>>
    val inputText: Value<String>
    val isLoading: Value<Boolean>

    // Dialog state properties
    val isPromptDialogVisible: Value<Boolean>
    val systemPrompt: Value<String>
    val assistantPrompt: Value<String>

    fun onTextChanged(text: String)
    fun onSendMessage()
    fun onNavigateBack()

    // Dialog control methods
    fun onShowPromptDialog()
    fun onDismissPromptDialog()
    fun onSavePrompts(systemPrompt: String, assistantPrompt: String)
    fun onClearPrompts()
}

data class ChatMessage @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val role: MessageRole? = null,
    val isVisibleInUI: Boolean = true
)

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}
