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
    val usage: org.oleg.ai.challenge.data.network.model.Usage? = null,
    val mcpName: String? = null,
    val isMcpSystemPrompt: Boolean = false,
    val isMcpIntermediate: Boolean = false
) {
    companion object {
        /**
         * Creates a system prompt message for an MCP tool.
         */
        @OptIn(ExperimentalTime::class)
        fun toolSystemPrompt(
            toolName: String,
            description: String,
            agentId: String? = null
        ): ChatMessage {
            return ChatMessage(
                id = "${Clock.System.now()}_tool_$toolName",
                text = description,
                isFromUser = false,
                role = MessageRole.SYSTEM,
                isVisibleInUI = false,
                agentId = agentId,
                mcpName = toolName,
                isMcpSystemPrompt = true
            )
        }

        /**
         * Creates an intermediate MCP message (hidden from UI).
         */
        @OptIn(ExperimentalTime::class)
        fun mcpIntermediate(
            text: String,
            isFromUser: Boolean,
            role: MessageRole,
            agentId: String? = null
        ): ChatMessage {
            return ChatMessage(
                id = "${Clock.System.now()}_mcp_${(0..1000).random()}",
                text = text,
                isFromUser = isFromUser,
                role = role,
                isVisibleInUI = false,
                agentId = agentId,
                isMcpIntermediate = true
            )
        }
    }
}

/**
 * Message role enum distinguishing between different message types.
 */
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}
