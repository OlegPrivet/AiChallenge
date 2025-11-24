package org.oleg.ai.challenge.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a chat message.
 *
 * Stores all fields from ChatMessage, including optional usage statistics.
 * Messages are linked to chats via foreign key, and optionally to agents.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chatId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Foreign key to the chat this message belongs to */
    val chatId: Long,

    /** Original message ID from ChatMessage.id */
    val messageId: String,

    /** Agent ID if the message is associated with a particular agent (nullable) */
    val agentId: String? = null,

    /** Message creation timestamp */
    val timestamp: Long,

    /** Message role (SYSTEM, USER, ASSISTANT) stored as String */
    val role: String? = null,

    /** Message text content */
    val text: String,

    /** Whether the message is from the user */
    val isFromUser: Boolean,

    /** Whether the message should be visible in UI */
    val isVisibleInUI: Boolean = true,

    /** Agent name (for display purposes) */
    val agentName: String? = null,

    /** Model used for this message (for AI responses) */
    val modelUsed: String? = null,

    /** Usage statistics - prompt tokens */
    val usagePromptTokens: Int? = null,

    /** Usage statistics - completion tokens */
    val usageCompletionTokens: Int? = null,

    /** Usage statistics - total tokens */
    val usageTotalTokens: Int? = null,

    /** JSON string of citations from RAG retrieval (serialized List<Citation>) */
    val citationsJson: String? = null,

    /** JSON string of retrieval trace for developer mode (serialized RetrievalTrace) */
    val retrievalTraceJson: String? = null
)
