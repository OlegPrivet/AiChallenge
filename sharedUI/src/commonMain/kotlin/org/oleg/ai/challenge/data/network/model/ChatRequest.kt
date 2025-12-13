package org.oleg.ai.challenge.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents a chat completion request for Ollama API with native tool calling support.
 *
 * @property model The AI model to use for the completion
 * @property messages List of messages in the conversation
 * @property tools Optional list of tools (functions) that the model can call
 * @property temperature Controls randomness (0.0 = deterministic, 2.0 = highly creative). Default: 1.0
 * @property stream Whether to stream the response
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val options: Options = Options(),
    val tools: List<ToolDefinition>? = null,
    val temperature: Float? = null,
    val stream: Boolean = false
)

@Serializable
data class Options(
    val num_thread: Int = 4
)

/**
 * Represents a tool definition for Ollama function calling.
 *
 * @property type The type of tool (always "function" for Ollama)
 * @property function The function details
 */
@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunction
)

/**
 * Represents a function that can be called by the AI model.
 *
 * @property name The unique name of the function
 * @property description Human-readable description of what the function does
 * @property parameters JSON Schema defining the function's parameters
 */
@Serializable
data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

/**
 * Represents a single message in a chat conversation.
 *
 * @property role The role of the message sender (user, assistant, system, or tool)
 * @property content The text content of the message
 * @property toolCallId The ID of the tool call this message is responding to (only for role=TOOL)
 */
@Serializable
data class ChatMessage(
    val role: MessageRole,
    val content: String,
    @SerialName("tool_call_id")
    val toolCallId: String? = null
)
