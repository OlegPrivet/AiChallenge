package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.McpProcessingPhase
import org.oleg.ai.challenge.data.model.McpUiState
import org.oleg.ai.challenge.data.model.MessageRole
import org.oleg.ai.challenge.data.network.ApiResult
import org.oleg.ai.challenge.data.network.createConversationRequest
import org.oleg.ai.challenge.data.network.json
import org.oleg.ai.challenge.data.network.model.ToolCall
import org.oleg.ai.challenge.data.network.model.ToolDefinition
import org.oleg.ai.challenge.data.network.model.ToolFunction
import org.oleg.ai.challenge.data.network.model.Usage
import org.oleg.ai.challenge.data.network.model.ChatMessage as ApiChatMessage
import org.oleg.ai.challenge.data.network.model.MessageRole as ApiMessageRole

/**
 * Orchestrator service that coordinates AI responses with MCP tool invocations.
 *
 * This service implements the following pipeline:
 * 1. Send user message to AI
 * 2. Intercept AI response (not displayed yet)
 * 3. Validate response against tool schema
 * 4. If valid: call MCP tool, send result to AI, get final response
 * 5. Display final response to user
 *
 * Per Task.md requirements:
 * - Intermediate responses are NEVER displayed
 * - Only the final AI response is shown
 * - MCP network calls are hidden from UI/history
 */
class ChatOrchestratorService(
    private val chatApiService: ChatApiService,
    private val mcpClientService: McpClientService,
    private val toolValidationService: ToolValidationService,
    private val ragOrchestrator: org.oleg.ai.challenge.domain.rag.orchestrator.RagOrchestrator? = null,
    private val documentIngestionRepository: org.oleg.ai.challenge.data.rag.repository.DocumentIngestionRepository? = null,
    private val knowledgeBaseRepository: org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository? = null,
    private val logger: Logger = Logger.withTag("ChatOrchestratorService"),
) {
    private val _mcpUiState = MutableStateFlow(McpUiState())
    val mcpUiState: StateFlow<McpUiState> = _mcpUiState.asStateFlow()

    companion object {
        private const val MAX_INSTRUCTION_ITERATIONS = 5
        private const val MAX_INSTRUCTION_DEPTH = 3
    }

    /**
     * Result of the orchestration process.
     */
    sealed class OrchestratorResult {
        /**
         * Successful completion with final AI response.
         */
        data class Success(
            val finalResponse: String,
            val model: String,
            val usage: Usage?,
            val citations: List<org.oleg.ai.challenge.domain.rag.orchestrator.Citation>? = null,
            val retrievalTrace: org.oleg.ai.challenge.domain.rag.orchestrator.RetrievalTrace? = null,
        ) : OrchestratorResult()

        /**
         * Error during processing.
         */
        data class Error(val message: String) : OrchestratorResult()
    }

    private sealed class AiCallResult {
        data class Success(
            val content: String,
            val toolCalls: List<ToolCall>?,
            val model: String,
            val usage: Usage?,
        ) : AiCallResult()

        data class Error(val message: String) : AiCallResult()
    }

    /**
     * Handles a user message with full MCP orchestration.
     *
     * @param conversationHistory The full conversation history for context
     * @param model The AI model to use
     * @param temperature The temperature setting for the model
     * @return OrchestratorResult with the final response or error
     */
    suspend fun handleUserMessage(
        conversationHistory: List<ChatMessage>,
        model: String,
        temperature: Float,
        @Suppress("UNUSED_PARAMETER") toolName: String,
        isRagEnabled: Boolean = false,
    ): OrchestratorResult {
        logger.d { "Processing user message with MCP orchestration (RAG enabled: $isRagEnabled)" }

        val apiMessages = conversationHistory.map { it.toApiMessage() }.toMutableList()

        // RAG enhancement: retrieve context before sending to AI
        var ragResult: org.oleg.ai.challenge.domain.rag.orchestrator.RagResult? = null
        if (isRagEnabled && ragOrchestrator != null) {
            try {
                val userQuery = conversationHistory.lastOrNull { it.isFromUser }?.text
                if (userQuery != null) {
                    logger.d { "Retrieving RAG context for query: $userQuery" }
                    ragResult = ragOrchestrator.retrieve(
                        query = userQuery,
                        topK = 6
                    )

                    // Build augmented prompt with retrieved context
                    val contextPrompt = buildRagContextPrompt(ragResult, userQuery)

                    // Replace the last user message with augmented version
                    val lastUserMessageIndex = apiMessages.indexOfLast { it.role == ApiMessageRole.USER }
                    if (lastUserMessageIndex >= 0) {
                        apiMessages[lastUserMessageIndex] = apiMessages[lastUserMessageIndex].copy(
                            content = contextPrompt
                        )
                    }

                    logger.d { "RAG context retrieved: ${ragResult.results.size} chunks, ${ragResult.citations.size} citations" }
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to retrieve RAG context, continuing without RAG" }
                // Continue without RAG on error
            }
        }

        updateMcpState(
            isMcpRunning = true,
            phase = McpProcessingPhase.Validating
        )

        return try {
            val result = runConversationFlow(
                apiMessages = apiMessages,
                model = model,
                temperature = temperature,
                depth = 0
            )

            // Attach RAG data to successful result
            if (result is OrchestratorResult.Success && ragResult != null) {
                result.copy(
                    citations = ragResult.citations,
                    retrievalTrace = ragResult.trace
                )
            } else {
                result
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to handle user message" }
            OrchestratorResult.Error(e.message ?: "Unknown orchestration error")
        } finally {
            resetMcpState()
        }
    }

    /**
     * Build an augmented prompt with RAG context.
     */
    private fun buildRagContextPrompt(ragResult: org.oleg.ai.challenge.domain.rag.orchestrator.RagResult, userQuery: String): String {
        return buildString {
            appendLine("Context from knowledge base:")
            appendLine()
            appendLine(ragResult.context)
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("User question: $userQuery")
            appendLine()
            appendLine("Instructions: Answer the user's question using the provided context above. When referencing information from the context, cite the source using the citation number in square brackets (e.g., [1], [2]).")
        }
    }

    private suspend fun runConversationFlow(
        apiMessages: MutableList<ApiChatMessage>,
        model: String,
        temperature: Float,
        depth: Int,
    ): OrchestratorResult {
/*        if (depth > MAX_INSTRUCTION_DEPTH) {
            logger.w { "Tool call depth exceeded" }
            return OrchestratorResult.Error("Tool call depth exceeded")
        }*/

        when (val aiResult = sendToAi(apiMessages, model, temperature)) {
            is AiCallResult.Error -> return OrchestratorResult.Error(aiResult.message)
            is AiCallResult.Success -> {
                val toolCalls = aiResult.toolCalls

                // If AI wants to call tools, execute them and continue
                if (toolCalls != null && toolCalls.isNotEmpty()) {
                    logger.d { "AI requested ${toolCalls.size} tool calls" }
                    return handleToolCalls(
                        toolCalls = toolCalls,
                        conversationHistory = apiMessages,
                        assistantMessage = aiResult.content,
                        model = model,
                        temperature = temperature,
                        depth = depth
                    )
                }

                // No tool calls - this is the final response
                logger.d { "AI returned final response without tool calls" }
                return OrchestratorResult.Success(
                    finalResponse = aiResult.content,
                    model = aiResult.model,
                    usage = aiResult.usage
                )
            }
        }
    }

    /**
     * Builds tool definitions from MCP available tools.
     *
     * @return List of ToolDefinition or null if no tools available
     */
    private fun buildToolsFromMcp(): List<ToolDefinition>? {
        val mcpTools = mcpClientService.availableTools.value

        if (mcpTools.isEmpty()) {
            logger.d { "No MCP tools available" }
            return null
        }

        logger.d { "Building tool definitions for ${mcpTools.size} MCP tools" }
        return mcpTools.map { mcpTool ->
            ToolDefinition(
                type = "function",
                function = ToolFunction(
                    name = mcpTool.name,
                    description = mcpTool.description ?: "",
                    parameters = mcpTool.parameters.properties
                )
            )
        }
    }

    /**
     * Executes a single tool call and returns the result as a string.
     *
     * @param toolCall The tool call to execute
     * @return The result of the tool execution as a string
     */
    private suspend fun executeToolCall(toolCall: ToolCall): String {
        logger.d { "Executing tool call: ${toolCall.function.name}" }
        updateMcpState(
            isMcpRunning = true,
            phase = McpProcessingPhase.InvokingTool,
            currentToolName = toolCall.function.name
        )

        return try {
            val arguments = json.decodeFromString<JsonObject>(toolCall.function.arguments)
            val argsMap = arguments.mapValues { (_, value) ->
                when (value) {
                    is kotlinx.serialization.json.JsonPrimitive -> {
                        value.content
                    }
                    else -> value.toString()
                }
            }

            val result = mcpClientService.callTool(
                name = toolCall.function.name,
                arguments = argsMap
            )

            if (result.isSuccess) {
                logger.d { "Tool call succeeded: ${toolCall.function.name}" }
                result.getOrThrow()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                logger.e { "Tool call failed: ${toolCall.function.name} - $error" }
                "Error: $error"
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception executing tool call: ${toolCall.function.name}" }
            "Error executing tool: ${e.message}"
        }
    }

    /**
     * Handles tool calls from AI response.
     * Executes each tool, adds results to conversation history, and recursively calls AI again.
     *
     * @param toolCalls List of tool calls from AI
     * @param conversationHistory Current conversation history
     * @param model The AI model to use
     * @param temperature The temperature setting
     * @return OrchestratorResult with the final response
     */
    private suspend fun handleToolCalls(
        toolCalls: List<ToolCall>,
        conversationHistory: MutableList<ApiChatMessage>,
        assistantMessage: String,
        model: String,
        temperature: Float,
        depth: Int
    ): OrchestratorResult {
        logger.d { "Handling ${toolCalls.size} tool calls" }

        // Add assistant message (may be empty when tool calls are present)
        conversationHistory.add(
            ApiChatMessage(
                role = ApiMessageRole.ASSISTANT,
                content = assistantMessage
            )
        )

        // Execute each tool call and add results to history
        for (toolCall in toolCalls) {
            val toolResult = executeToolCall(toolCall)

            // Add tool result to conversation history
            conversationHistory.add(
                ApiChatMessage(
                    role = org.oleg.ai.challenge.data.network.model.MessageRole.TOOL,
                    content = toolResult,
                    toolCallId = toolCall.id
                )
            )
        }

        // Continue the conversation flow with updated history
        return runConversationFlow(
            apiMessages = conversationHistory,
            model = model,
            temperature = temperature,
            depth = depth + 1
        )
    }

    /**
     * Sends messages to AI and returns the response with tool calls if any.
     */
    private suspend fun sendToAi(
        messages: List<ApiChatMessage>,
        model: String,
        temperature: Float,
    ): AiCallResult {
        val tools = buildToolsFromMcp()

        val request = createConversationRequest(
            messages = messages,
            model = model,
            temperature = temperature,
            tools = tools
        )

        return when (val result = chatApiService.sendChatCompletion(request)) {
            is ApiResult.Success -> {
                val response = result.data
                val toolCalls = response.message.toolCalls

                AiCallResult.Success(
                    content = response.message.content,
                    toolCalls = toolCalls,
                    model = response.model,
                    usage = Usage(
                        promptTokens = response.promptEvalCount ?: 0,
                        completionTokens = response.evalCount ?: 0,
                        totalTokens = (response.promptEvalCount ?: 0) + (response.evalCount ?: 0)
                    )
                )
            }

            is ApiResult.Error -> {
                AiCallResult.Error(result.error.getDescription())
            }
        }
    }

    /**
     * Updates the MCP UI state.
     */
    private fun updateMcpState(
        isMcpRunning: Boolean,
        phase: McpProcessingPhase,
        currentToolName: String? = null,
        validationError: String? = null,
        retryCount: Int? = null,
    ) {
        _mcpUiState.value = _mcpUiState.value.copy(
            isMcpRunning = isMcpRunning,
            processingPhase = phase,
            currentToolName = currentToolName ?: _mcpUiState.value.currentToolName,
            validationError = validationError,
            retryCount = retryCount ?: _mcpUiState.value.retryCount
        )
    }

    /**
     * Resets the MCP UI state to idle.
     */
    private fun resetMcpState() {
        _mcpUiState.value = McpUiState()
    }

    /**
     * Converts a UI ChatMessage to API ChatMessage format.
     */
    private fun ChatMessage.toApiMessage(): ApiChatMessage {
        val apiRole = when (role) {
            MessageRole.SYSTEM -> ApiMessageRole.SYSTEM
            MessageRole.ASSISTANT -> ApiMessageRole.ASSISTANT
            MessageRole.USER -> ApiMessageRole.USER
            null -> if (isFromUser) ApiMessageRole.USER else ApiMessageRole.ASSISTANT
        }
        return ApiChatMessage(
            role = apiRole,
            content = text
        )
    }

    /**
     * Injects system prompts for enabled tools into the message history.
     *
     * @param enabledTools List of enabled tools
     * @param agentId The agent ID to associate prompts with
     * @return List of system prompt messages for the tools
     */
}
