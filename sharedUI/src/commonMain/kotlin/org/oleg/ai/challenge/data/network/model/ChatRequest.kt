package org.oleg.ai.challenge.data.network.model

import kotlinx.serialization.Serializable

/**
 * Represents a chat completion request to the OpenRouter API.
 *
 * @property model The AI model to use for the completion
 * @property messages List of messages in the conversation
 * @property temperature Controls randomness (0.0 = deterministic, 2.0 = highly creative). Default: 1.0
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val options: Options = Options(),
    val temperature: Float? = null,
    val max_tokens: Int = 150,
)

@Serializable
data class Options(
    val num_thread: Int = 4
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
