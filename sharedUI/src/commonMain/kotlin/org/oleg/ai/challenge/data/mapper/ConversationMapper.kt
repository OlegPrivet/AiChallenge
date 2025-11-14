package org.oleg.ai.challenge.data.mapper

import org.oleg.ai.challenge.data.database.entity.ChatEntity
import org.oleg.ai.challenge.data.model.Conversation

/**
 * Mapper for converting between Conversation domain model and ChatEntity.
 */
object ConversationMapper {

    /**
     * Convert ChatEntity to Conversation domain model.
     */
    fun ChatEntity.toDomain(): Conversation {
        return Conversation(
            chatId = chatId,
            chatName = chatName,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Convert Conversation domain model to ChatEntity.
     */
    fun Conversation.toEntity(): ChatEntity {
        return ChatEntity(
            chatId = chatId,
            chatName = chatName,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Convert list of ChatEntity to list of Conversation.
     */
    fun List<ChatEntity>.toDomain(): List<Conversation> {
        return map { it.toDomain() }
    }
}
