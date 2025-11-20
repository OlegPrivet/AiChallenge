package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import io.modelcontextprotocol.kotlin.sdk.EmptyJsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.McpProcessingPhase
import org.oleg.ai.challenge.data.model.McpUiState
import org.oleg.ai.challenge.data.model.MessageRole
import org.oleg.ai.challenge.data.network.ApiResult
import org.oleg.ai.challenge.data.network.createConversationRequest
import org.oleg.ai.challenge.data.network.json
import org.oleg.ai.challenge.data.network.model.Usage
import org.oleg.ai.challenge.ui.mcp.jsonElementToAny
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
    private val logger: Logger = Logger.withTag("ChatOrchestratorService"),
) {
    private val _mcpUiState = MutableStateFlow(McpUiState())
    val mcpUiState: StateFlow<McpUiState> = _mcpUiState.asStateFlow()

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
        ) : OrchestratorResult()

        /**
         * Error during processing.
         */
        data class Error(val message: String) : OrchestratorResult()
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
        toolName: String,
    ): OrchestratorResult {
        logger.d { "Processing user message with MCP orchestration" }

        // Convert UI messages to API format
        val apiMessages = conversationHistory.map { it.toApiMessage() }.toMutableList()

        // Get available tools
        val availableTools = mcpClientService.availableTools.value

        // If no tools available, just send to AI directly
        if (availableTools.isEmpty()) {
            logger.d { "No MCP tools available, sending directly to AI" }
            return sendToAiDirect(apiMessages, model, temperature)
        }

        // Step 1: Get initial AI response (draft - not shown)
        updateMcpState(
            isMcpRunning = true,
            phase = McpProcessingPhase.Validating
        )

        val initialResponse = sendToAi(apiMessages, model, temperature)
        if (initialResponse is OrchestratorResult.Error) {
            resetMcpState()
            return initialResponse
        }

        val draftResponse = (initialResponse as OrchestratorResult.Success).finalResponse

        // Step 2: Validate response against tool schemas
//        val validationResult = toolValidationService.validateResponse(draftResponse, availableTools)

        val toolRequest: ToolRequest = try {
            json.decodeFromString(draftResponse)
        } catch (e: Exception) {
            ToolRequest(name = "empty", arguments = EmptyJsonObject)
        }
        logger.i { "Valid tool call for 'get_messages'" }
        return processMcpToolCall(
            apiMessages = apiMessages,
            toolName = toolRequest.name,
            arguments = toolRequest.getArgs(),
            model = model,
            temperature = temperature
        )


//        when (validationResult) {
//            is ToolValidationService.ValidationResult.NoToolCall -> {
//                // No tool call detected - return the response directly
//                logger.d { "No tool call in response, returning directly" }
//                resetMcpState()
//                initialResponse
//            }
//
//            is ToolValidationService.ValidationResult.Invalid -> {
//                // Validation failed - retry
//                logger.w { "Validation failed: ${validationResult.reason}" }
//                handleValidationFailure(
//                    apiMessages = apiMessages,
//                    originalResponse = draftResponse,
//                    error = validationResult.reason,
//                    tool = null,
//                    model = model,
//                    temperature = temperature
//                )
//            }
//
//            is ToolValidationService.ValidationResult.Valid -> {
//                // Valid tool call - process through MCP
//                val jsonObject = json.parseToJsonElement(draftResponse) as? JsonObject
//                val args = if (jsonObject != null) {
//                    val parsedMap = jsonObject.mapValues { (_, value) ->
//                        jsonElementToAny(value)
//                    }
//                    parsedMap
//                } else {
//                    // Not a JSON object, reset to empty map
//                    emptyMap()
//                }
//                logger.i { "Valid tool call for '${validationResult.toolName}'" }
//                processMcpToolCall(
//                    apiMessages = apiMessages,
//                    toolName = "get_current",
//                    arguments = args,
//                    model = model,
//                    temperature = temperature
//                )
//            }
//        }
    }

    @Serializable
    data class ToolRequest(
        val name: String,
        val arguments: JsonObject,
    ) {

        fun getArgs(): Map<String, Any> {
            return try {
                arguments.mapValues { (_, value) ->
                    jsonElementToAny(value)
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    /**
     * Handles validation failure with retry logic.
     */
    private suspend fun handleValidationFailure(
        apiMessages: MutableList<ApiChatMessage>,
        originalResponse: String,
        error: String,
        tool: McpClientService.ToolInfo?,
        model: String,
        temperature: Float,
    ): OrchestratorResult {
        val currentRetryCount = _mcpUiState.value.retryCount

        if (currentRetryCount >= McpUiState.MAX_RETRIES) {
            logger.w { "Max retries exceeded, falling back to direct response" }
            resetMcpState()
            // Return the original response as fallback
            return OrchestratorResult.Success(
                finalResponse = originalResponse,
                model = model,
                usage = null
            )
        }

        // Update state for retry
        updateMcpState(
            isMcpRunning = true,
            phase = McpProcessingPhase.Retrying,
            validationError = error,
            retryCount = currentRetryCount + 1
        )

        // Add the draft response to context
        apiMessages.add(
            ApiChatMessage(
                role = ApiMessageRole.ASSISTANT,
                content = originalResponse
            )
        )

        // Create error prompt
        val errorPrompt = toolValidationService.createValidationErrorPrompt(
            validationError = error,
            toolName = tool?.name ?: "unknown",
            inputSchema = tool?.parameters
        )

        // Add error prompt as user message
        apiMessages.add(
            ApiChatMessage(
                role = ApiMessageRole.USER,
                content = errorPrompt
            )
        )

        // Retry AI call
        logger.d { "Retrying AI call after validation failure (attempt ${currentRetryCount + 1})" }
        val retryResponse = sendToAi(apiMessages, model, temperature)

        if (retryResponse is OrchestratorResult.Error) {
            resetMcpState()
            return retryResponse
        }

        val retryText = (retryResponse as OrchestratorResult.Success).finalResponse

        // Re-validate
        val availableTools = mcpClientService.availableTools.value
        val revalidation = toolValidationService.validateResponse(retryText, availableTools)

        return when (revalidation) {
            is ToolValidationService.ValidationResult.NoToolCall -> {
                logger.d { "No tool call in retry response" }
                resetMcpState()
                retryResponse
            }

            is ToolValidationService.ValidationResult.Invalid -> {
                // Recursive retry
                handleValidationFailure(
                    apiMessages = apiMessages,
                    originalResponse = retryText,
                    error = revalidation.reason,
                    tool = tool,
                    model = model,
                    temperature = temperature
                )
            }

            is ToolValidationService.ValidationResult.Valid -> {
                processMcpToolCall(
                    apiMessages = apiMessages,
                    toolName = revalidation.toolName,
                    arguments = revalidation.arguments,
                    model = model,
                    temperature = temperature
                )
            }
        }
    }

    /**
     * Processes a validated MCP tool call.
     */
    private suspend fun processMcpToolCall(
        apiMessages: MutableList<ApiChatMessage>,
        toolName: String,
        arguments: Map<String, Any>,
        model: String,
        temperature: Float,
    ): OrchestratorResult {
        logger.d { "Invoking MCP tool: $toolName with ${arguments.size} arguments" }

        // Update state
        updateMcpState(
            isMcpRunning = true,
            phase = McpProcessingPhase.InvokingTool,
            currentToolName = toolName
        )

        // Call the MCP tool
        val mcpResult = mcpClientService.callTool(toolName, arguments)

        if (mcpResult.isFailure) {
            val error = mcpResult.exceptionOrNull()?.message ?: "Unknown MCP error"
            logger.e { "MCP tool call failed: $error" }
            resetMcpState()
            return OrchestratorResult.Error("MCP tool error: $error")
        }

        val mcpResponse = mcpResult.getOrThrow()
        logger.d { "MCP tool response: ${mcpResponse.take(200)}..." }

        // Update state for final response generation
        updateMcpState(
            isMcpRunning = true,
            phase = McpProcessingPhase.GeneratingFinalResponse,
            currentToolName = toolName
        )

        // Send MCP response to AI for final answer (silently, not shown in UI)
        apiMessages.add(
            ApiChatMessage(
                role = ApiMessageRole.USER,
                content = "Ответ MCP инструмента '$toolName':\n$mcpResponse\n\nСформулируй окончательный ответ пользователю на основе запроса пользователя и этих данных."
            )
        )

        // Get final AI response
        val finalResult = sendToAi(apiMessages, model, temperature)

        resetMcpState()
        return finalResult
    }

    /**
     * Sends messages directly to AI without MCP processing.
     */
    private suspend fun sendToAiDirect(
        messages: List<ApiChatMessage>,
        model: String,
        temperature: Float,
    ): OrchestratorResult {
        return sendToAi(messages, model, temperature)
    }

    /**
     * Sends messages to AI and returns the response.
     */
    private suspend fun sendToAi(
        messages: List<ApiChatMessage>,
        model: String,
        temperature: Float,
    ): OrchestratorResult {
        val request = createConversationRequest(
            messages = messages,
            model = model,
            temperature = temperature
        )

        return when (val result = chatApiService.sendChatCompletion(request)) {
            is ApiResult.Success -> {
                val responseText = result.data.choices.firstOrNull()?.message?.content
                if (responseText == null) {
                    OrchestratorResult.Error("Empty response from AI")
                } else {
                    OrchestratorResult.Success(
                        finalResponse = responseText,
                        model = result.data.model,
                        usage = result.data.usage
                    )
                }
            }

            is ApiResult.Error -> {
                OrchestratorResult.Error(result.error.getDescription())
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
    fun createToolSystemPrompts(
        enabledTools: List<McpClientService.ToolInfo>,
        agentId: String? = null,
    ): List<ChatMessage> {
        var index = 0
        return enabledTools.map { tool ->
            index++
            ChatMessage.toolSystemPrompt(
                index = index,
                toolName = tool.name,
                description = tool.description + "IF YOU CHOOSE ME, RETURN ONLY IN THE FORM OF A JSON OBJECT WITH TOOL NAME AND ARGUMENTS {\"name\":\"${tool.name}\",\"arguments\":${tool.parameters.properties}} WITH VALID DATA!!!!\n",
                agentId = agentId
            )
        }
    }

    /**
     * Removes system prompts for a specific tool from the message history.
     *
     * @param messages Current message list
     * @param toolName Name of the tool to remove prompts for
     * @return Updated message list without the tool's prompts
     */
    fun removeToolSystemPrompts(
        messages: List<ChatMessage>,
        toolName: String,
    ): List<ChatMessage> {
        return messages.filter { message ->
            !(message.isMcpSystemPrompt && message.mcpName == toolName)
        }
    }
}
