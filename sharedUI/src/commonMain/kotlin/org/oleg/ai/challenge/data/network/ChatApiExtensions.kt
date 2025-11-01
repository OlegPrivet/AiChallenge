package org.oleg.ai.challenge.data.network

import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.network.model.ChatMessage
import org.oleg.ai.challenge.data.network.model.ChatRequest
import org.oleg.ai.challenge.data.network.model.MessageRole

/**
 * Extension functions for easier chat API usage.
 */

/**
 * Creates a ChatRequest with a single user message.
 *
 * @param userMessage The user's message content
 * @param model The AI model to use (default: BuildConfig.DEFAULT_MODEL)
 * @return A ChatRequest ready to be sent to the API
 */
fun createSimpleUserRequest(
    userMessage: String,
    model: String = BuildConfig.DEFAULT_MODEL
): ChatRequest {
    return ChatRequest(
        model = model,
        messages = listOf(
            ChatMessage(
                role = MessageRole.USER,
                content = userMessage
            )
        )
    )
}

/**
 * Creates a ChatRequest with a system prompt and user message.
 *
 * @param systemPrompt The system prompt to guide the AI's behavior
 * @param userMessage The user's message content
 * @param model The AI model to use (default: BuildConfig.DEFAULT_MODEL)
 * @return A ChatRequest ready to be sent to the API
 */
fun createRequestWithSystemPrompt(
    systemPrompt: String,
    userMessage: String,
    model: String = BuildConfig.DEFAULT_MODEL
): ChatRequest {
    return ChatRequest(
        model = model,
        messages = listOf(
            ChatMessage(role = MessageRole.SYSTEM, content = systemPrompt),
            ChatMessage(role = MessageRole.USER, content = userMessage)
        )
    )
}

/**
 * Creates a ChatRequest with a conversation history.
 * Useful for maintaining context across multiple turns.
 *
 * @param messages List of messages in the conversation
 * @param model The AI model to use (default: BuildConfig.DEFAULT_MODEL)
 * @return A ChatRequest ready to be sent to the API
 */
fun createConversationRequest(
    messages: List<ChatMessage>,
    model: String = BuildConfig.DEFAULT_MODEL
): ChatRequest {
    return ChatRequest(
        model = model,
        messages = messages
    )
}

/**
 * Builder function for creating ChatMessage instances.
 *
 * @param role The role of the message
 * @param content The content of the message
 * @return A ChatMessage instance
 */
fun chatMessage(role: MessageRole, content: String): ChatMessage {
    return ChatMessage(role = role, content = content)
}

/**
 * Creates a user message.
 */
fun userMessage(content: String): ChatMessage = chatMessage(MessageRole.USER, content)

/**
 * Creates an assistant message.
 */
fun assistantMessage(content: String): ChatMessage = chatMessage(MessageRole.ASSISTANT, content)

/**
 * Creates a system message.
 */
fun systemMessage(content: String): ChatMessage = chatMessage(MessageRole.SYSTEM, content)

/**
 * Extension function to add a user message to a list of messages.
 */
fun List<ChatMessage>.addUserMessage(content: String): List<ChatMessage> {
    return this + userMessage(content)
}

/**
 * Extension function to add an assistant message to a list of messages.
 */
fun List<ChatMessage>.addAssistantMessage(content: String): List<ChatMessage> {
    return this + assistantMessage(content)
}

/**
 * Extension function to add a system message to a list of messages.
 */
fun List<ChatMessage>.addSystemMessage(content: String): List<ChatMessage> {
    return this + systemMessage(content)
}
