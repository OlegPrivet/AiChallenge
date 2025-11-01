package org.oleg.ai.challenge.data.network.model

import kotlinx.serialization.Serializable

/**
 * Represents a chat completion request to the OpenRouter API.
 *
 * @property model The AI model to use for the completion
 * @property messages List of messages in the conversation
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>
)

/**
 * Represents a single message in a chat conversation.
 *
 * @property role The role of the message sender (user, assistant, or system)
 * @property content The text content of the message
 */
@Serializable
data class ChatMessage(
    val role: MessageRole,
    val content: String
)