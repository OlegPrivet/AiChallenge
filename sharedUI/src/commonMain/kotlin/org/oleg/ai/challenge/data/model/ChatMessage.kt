package org.oleg.ai.challenge.data.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Domain model representing a chat message.
 *
 * This is the UI/business logic representation used throughout the app.
 * It maps to MessageEntity for database persistence.
 */
data class ChatMessage @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val role: MessageRole? = null,
    val isVisibleInUI: Boolean = true,
    val agentName: String? = null,
    val agentId: String? = null,
    val modelUsed: String? = null,
    val usage: org.oleg.ai.challenge.data.network.model.Usage? = null
)

/**
 * Message role enum distinguishing between different message types.
 */
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}
