package org.oleg.ai.challenge.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Room entity representing a chat/conversation.
 * Chat name is taken from the first message (max 50 chars).
 */
@Entity(tableName = "chats")
data class ChatEntity @OptIn(ExperimentalTime::class) constructor(
    @PrimaryKey(autoGenerate = true)
    val chatId: Long = 0,
    val chatName: String = "New chat",
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),

    /** Whether RAG mode is enabled for this chat */
    val isRagEnabled: Boolean = false
)
