package org.oleg.ai.challenge.data.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Domain model representing a chat/conversation.
 *
 * This is the UI/business logic representation of a chat.
 * It maps to ChatEntity for database persistence.
 */
data class Conversation @OptIn(ExperimentalTime::class) constructor(
    val chatId: Long = 0,
    val chatName: String = "New chat",
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isRagEnabled: Boolean = false
)
