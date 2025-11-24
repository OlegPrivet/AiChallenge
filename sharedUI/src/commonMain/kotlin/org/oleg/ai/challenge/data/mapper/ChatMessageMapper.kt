package org.oleg.ai.challenge.data.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.oleg.ai.challenge.data.database.entity.MessageEntity
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.MessageRole
import org.oleg.ai.challenge.data.network.model.Usage
import org.oleg.ai.challenge.domain.rag.orchestrator.Citation
import org.oleg.ai.challenge.domain.rag.orchestrator.RetrievalTrace

/**
 * Mapper for converting between ChatMessage domain model and MessageEntity.
 */
object ChatMessageMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Convert MessageEntity to ChatMessage domain model.
     */
    fun MessageEntity.toDomain(): ChatMessage {
        val citations = citationsJson?.let {
            try {
                json.decodeFromString<List<Citation>>(it)
            } catch (e: Exception) {
                null
            }
        }

        val retrievalTrace = retrievalTraceJson?.let {
            try {
                json.decodeFromString<RetrievalTrace>(it)
            } catch (e: Exception) {
                null
            }
        }

        return ChatMessage(
            id = messageId,
            text = text,
            isFromUser = isFromUser,
            timestamp = timestamp,
            role = role?.let { MessageRole.valueOf(it) },
            isVisibleInUI = isVisibleInUI,
            agentName = agentName,
            agentId = agentId,
            modelUsed = modelUsed,
            usage = if (usagePromptTokens != null && usageCompletionTokens != null && usageTotalTokens != null) {
                Usage(
                    promptTokens = usagePromptTokens,
                    completionTokens = usageCompletionTokens,
                    totalTokens = usageTotalTokens
                )
            } else {
                null
            },
            citations = citations,
            retrievalTrace = retrievalTrace
        )
    }

    /**
     * Convert ChatMessage domain model to MessageEntity.
     * Requires chatId parameter since ChatMessage doesn't store it.
     */
    fun ChatMessage.toEntity(chatId: Long): MessageEntity {
        val citationsJson = citations?.let {
            try {
                json.encodeToString(it)
            } catch (e: Exception) {
                null
            }
        }

        val retrievalTraceJson = retrievalTrace?.let {
            try {
                json.encodeToString(it)
            } catch (e: Exception) {
                null
            }
        }

        return MessageEntity(
            chatId = chatId,
            messageId = id,
            agentId = agentId,
            timestamp = timestamp,
            role = role?.name,
            text = text,
            isFromUser = isFromUser,
            isVisibleInUI = isVisibleInUI,
            agentName = agentName,
            modelUsed = modelUsed,
            usagePromptTokens = usage?.promptTokens,
            usageCompletionTokens = usage?.completionTokens,
            usageTotalTokens = usage?.totalTokens,
            citationsJson = citationsJson,
            retrievalTraceJson = retrievalTraceJson
        )
    }

    /**
     * Convert list of MessageEntity to list of ChatMessage.
     */
    fun List<MessageEntity>.toDomain(): List<ChatMessage> {
        return map { it.toDomain() }
    }

    /**
     * Convert list of ChatMessage to list of MessageEntity.
     */
    fun List<ChatMessage>.toEntity(chatId: Long): List<MessageEntity> {
        return map { it.toEntity(chatId) }
    }
}
