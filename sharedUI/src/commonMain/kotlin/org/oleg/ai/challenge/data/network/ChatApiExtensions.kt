package org.oleg.ai.challenge.data.network

import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.network.model.ChatMessage
import org.oleg.ai.challenge.data.network.model.ChatRequest
import org.oleg.ai.challenge.data.network.model.MessageRole
import org.oleg.ai.challenge.data.network.model.ToolDefinition

/**
 * Extension functions for easier chat API usage.
 */

/**
 * Creates a ChatRequest with a single user message.
 *
 * @param userMessage The user's message content
 * @param model The AI model to use (default: BuildConfig.DEFAULT_MODEL)
 * @param temperature Controls response variety (0.0 = deterministic, 2.0 = highly creative)
 * @param tools Optional list of tools the AI can call
 * @return A ChatRequest ready to be sent to the API
 */
fun createSimpleUserRequest(
    userMessage: String,
    model: String = BuildConfig.DEFAULT_MODEL,
    temperature: Float? = null,
    tools: List<ToolDefinition>? = null
): ChatRequest {
    return ChatRequest(
        model = model,
        messages = listOf(
            ChatMessage(
                role = MessageRole.USER,
                content = userMessage
            )
        ),
        temperature = temperature,
        tools = tools
    )
}

/**
 * Creates a ChatRequest with a system prompt and user message.
 *
 * @param systemPrompt The system prompt to guide the AI's behavior
 * @param userMessage The user's message content
 * @param model The AI model to use (default: BuildConfig.DEFAULT_MODEL)
 * @param temperature Controls response variety (0.0 = deterministic, 2.0 = highly creative)
 * @param tools Optional list of tools the AI can call
 * @return A ChatRequest ready to be sent to the API
 */
fun createRequestWithSystemPrompt(
    systemPrompt: String,
    userMessage: String,
    model: String = BuildConfig.DEFAULT_MODEL,
    temperature: Float? = null,
    tools: List<ToolDefinition>? = null
): ChatRequest {
    return ChatRequest(
        model = model,
        messages = listOf(
            ChatMessage(role = MessageRole.SYSTEM, content = systemPrompt),
            ChatMessage(role = MessageRole.USER, content = userMessage)
        ),
        temperature = temperature,
        tools = tools
    )
}

/**
 * Creates a ChatRequest with optional system and assistant prompts, and a user message.
 *
 * @param systemPrompt The optional system prompt to guide the AI's behavior
 * @param assistantPrompt The optional assistant prompt to guide the AI's behavior
 * @param userMessage The user's message content
 * @param model The AI model to use (default: BuildConfig.DEFAULT_MODEL)
 * @param temperature Controls response variety (0.0 = deterministic, 2.0 = highly creative)
 * @param tools Optional list of tools the AI can call
 * @return A ChatRequest ready to be sent to the API
 */
fun createRequestWithSystemAndAssistantPrompts(
    systemPrompt: String?,
    assistantPrompt: String?,
    userMessage: String,
    model: String = BuildConfig.DEFAULT_MODEL,
    temperature: Float? = null,
    tools: List<ToolDefinition>? = null
): ChatRequest {
    val messages = buildList {
        systemPrompt?.let { add(ChatMessage(role = MessageRole.SYSTEM, content = it)) }
        assistantPrompt?.let { add(ChatMessage(role = MessageRole.ASSISTANT, content = it)) }
        add(ChatMessage(role = MessageRole.USER, content = userMessage))
    }

    return ChatRequest(
        model = model,
        messages = messages,
        temperature = temperature,
        tools = tools
    )
}

/**
 * Creates a ChatRequest with a conversation history.
 * Useful for maintaining context across multiple turns.
 *
 * @param messages List of messages in the conversation
 * @param model The AI model to use (default: BuildConfig.DEFAULT_MODEL)
 * @param temperature Controls response variety (0.0 = deterministic, 2.0 = highly creative)
 * @param tools Optional list of tools the AI can call
 * @return A ChatRequest ready to be sent to the API
 */
fun createConversationRequest(
    messages: List<ChatMessage>,
    model: String = BuildConfig.DEFAULT_MODEL,
    temperature: Float? = null,
    tools: List<ToolDefinition>? = null
): ChatRequest {
    return ChatRequest(
        model = model,
        messages = messages,
        temperature = temperature,
        tools = tools
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
 * Creates a tool response message.
 *
 * @param content The result content from the tool execution
 * @param toolCallId The ID of the tool call this message is responding to
 * @return A ChatMessage with role=TOOL
 */
fun toolMessage(content: String, toolCallId: String): ChatMessage =
    ChatMessage(role = MessageRole.TOOL, content = content, toolCallId = toolCallId)

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

/**
 * Extension function to add a tool message to a list of messages.
 *
 * @param content The result content from the tool execution
 * @param toolCallId The ID of the tool call this message is responding to
 * @return A new list with the tool message appended
 */
fun List<ChatMessage>.addToolMessage(content: String, toolCallId: String): List<ChatMessage> {
    return this + toolMessage(content, toolCallId)
}
