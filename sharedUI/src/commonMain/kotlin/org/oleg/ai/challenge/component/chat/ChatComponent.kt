package org.oleg.ai.challenge.component.chat

import com.arkivanov.decompose.value.Value
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ChatComponent {
    val messages: Value<List<ChatMessage>>
    val inputText: Value<String>
    val isLoading: Value<Boolean>

    fun onTextChanged(text: String)
    fun onSendMessage()
    fun onNavigateBack()
}

data class ChatMessage @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)
