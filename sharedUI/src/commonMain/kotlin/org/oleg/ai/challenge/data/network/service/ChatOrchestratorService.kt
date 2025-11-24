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
import org.oleg.ai.challenge.data.network.model.Instructions
import org.oleg.ai.challenge.data.network.model.ResponseContent
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
    private val ragOrchestrator: org.oleg.ai.challenge.domain.rag.orchestrator.RagOrchestrator? = null,
    private val logger: Logger = Logger.withTag("ChatOrchestratorService"),
) {
    private val _mcpUiState = MutableStateFlow(McpUiState())
    val mcpUiState: StateFlow<McpUiState> = _mcpUiState.asStateFlow()

    companion object {
        private const val MAX_INSTRUCTION_ITERATIONS = 5
        private const val MAX_INSTRUCTION_DEPTH = 3
        private const val MAX_RESULT_LENGTH = 2_000
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
            val content: ResponseContent,
            val model: String,
            val usage: Usage?,
        ) : AiCallResult()

        data class Error(val message: String) : AiCallResult()
    }

    private sealed class InstructionExecutionResult {
        data class ContinueWith(val messages: MutableList<ApiChatMessage>) : InstructionExecutionResult()
        data class Error(val message: String) : InstructionExecutionResult()
    }

    private data class InstructionExecutionSummary(
        val title: String,
        val output: String,
    )

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
        if (depth > MAX_INSTRUCTION_DEPTH) {
            logger.w { "Instruction depth exceeded" }
            return OrchestratorResult.Error("Instruction depth exceeded")
        }

        var iteration = 0
        var messages = apiMessages

        while (iteration < MAX_INSTRUCTION_ITERATIONS) {
            iteration++

            when (val aiResult = sendToAi(messages, model, temperature)) {
                is AiCallResult.Error -> return OrchestratorResult.Error(aiResult.message)
                is AiCallResult.Success -> {
                    val responseContent = aiResult.content
                    val instructions = responseContent.instructions

                    if (instructions.isNullOrEmpty() || instructions.all { it.isCompleted }) {
                        logger.d { "All instructions completed or absent, returning final response" }
                        return OrchestratorResult.Success(
                            finalResponse = responseContent.message,
                            model = aiResult.model,
                            usage = aiResult.usage
                        )
                    }

                    val instructionExecution = executeInstructions(
                        instructions = instructions,
                        baseMessages = messages,
                        model = model,
                        temperature = temperature,
                        depth = depth
                    )

                    when (instructionExecution) {
                        is InstructionExecutionResult.Error -> return OrchestratorResult.Error(instructionExecution.message)
                        is InstructionExecutionResult.ContinueWith -> {
                            messages = instructionExecution.messages
                        }
                    }
                }
            }
        }

        logger.w { "Instruction processing exceeded $MAX_INSTRUCTION_ITERATIONS iterations" }
        return OrchestratorResult.Error("Instruction processing exceeded limit")
    }

    private suspend fun executeInstructions(
        instructions: List<Instructions>,
        baseMessages: MutableList<ApiChatMessage>,
        model: String,
        temperature: Float,
        depth: Int,
    ): InstructionExecutionResult {
        val executionSummaries = mutableListOf<InstructionExecutionSummary>()
        val updatedMessages = baseMessages.toMutableList()

        instructions.forEach { instruction ->
            if (instruction.isCompleted) return@forEach

            when (instruction) {
                is Instructions.CallMCPTool -> {
                    updateMcpState(
                        isMcpRunning = true,
                        phase = McpProcessingPhase.InvokingTool,
                        currentToolName = instruction.name
                    )

                    val argsMap = try {
                        instruction.arguments.toArgumentMap()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to parse arguments for ${instruction.name}" }
                        return InstructionExecutionResult.Error(
                            "Failed to parse arguments for ${instruction.name}: ${e.message ?: "Unknown error"}"
                        )
                    }

                    val mcpResult = mcpClientService.callTool(instruction.name, argsMap)
                    if (mcpResult.isFailure) {
                        val errorMessage = mcpResult.exceptionOrNull()?.message ?: "Unknown MCP error"
                        logger.e { "MCP tool call failed: $errorMessage" }
                        return InstructionExecutionResult.Error("MCP tool error: $errorMessage")
                    }

                    val resultText = mcpResult.getOrThrow().limitForReport()
                    executionSummaries.add(
                        InstructionExecutionSummary(
                            title = instruction.name,
                            output = resultText
                        )
                    )
                }

                is Instructions.CallAi -> {
                    if (depth >= MAX_INSTRUCTION_DEPTH) {
                        logger.w { "Max instruction depth reached for CallAi" }
                        return InstructionExecutionResult.Error("Instruction depth exceeded for AI instruction")
                    }

                    val instructionMessages = baseMessages.toMutableList()

                    if (executionSummaries.isNotEmpty()) {
                        val contextSummary = executionSummaries.joinToString(separator = "\n") { summary ->
                            "${summary.title}: ${summary.output}"
                        }.limitForReport()
                        instructionMessages.add(
                            ApiChatMessage(
                                role = ApiMessageRole.USER,
                                content = "Context from executed instructions:\n$contextSummary"
                            )
                        )
                    }

                    instructionMessages.add(
                        ApiChatMessage(
                            role = ApiMessageRole.USER,
                            content = instruction.expectedResultOfInstruction
                        )
                    )

                    val aiInstructionResult = runConversationFlow(
                        apiMessages = instructionMessages,
                        model = model,
                        temperature = temperature,
                        depth = depth + 1
                    )

                    when (aiInstructionResult) {
                        is OrchestratorResult.Error -> return InstructionExecutionResult.Error(aiInstructionResult.message)
                        is OrchestratorResult.Success -> executionSummaries.add(
                            InstructionExecutionSummary(
                                title = instruction.expectedResultOfInstruction,
                                output = aiInstructionResult.finalResponse.limitForReport()
                            )
                        )
                    }
                }
            }
        }

        if (executionSummaries.isEmpty()) {
            return InstructionExecutionResult.ContinueWith(updatedMessages)
        }

        updateMcpState(
            isMcpRunning = true,
            phase = McpProcessingPhase.GeneratingFinalResponse
        )

        val followUpPrompt = buildInstructionReport(executionSummaries)
        updatedMessages.add(
            ApiChatMessage(
                role = ApiMessageRole.USER,
                content = followUpPrompt
            )
        )

        return InstructionExecutionResult.ContinueWith(updatedMessages)
    }

    private fun buildInstructionReport(executions: List<InstructionExecutionSummary>): String {
        return buildString {
            append("The following instructions were executed automatically and marked as completed:\n")
            executions.forEachIndexed { index, summary ->
                append("${index + 1}. ${summary.title}\n")
                append("Result: ${summary.output}\n")
            }
            append("Return the next ResponseContent JSON. Mark executed instructions with isCompleted=true, ")
            append("fill actualResultOfInstruction where it applies, and include remaining instructions if more work is needed.")
        }
    }

    private fun JsonObject.toArgumentMap(): Map<String, Any> {
        return this.mapValues { (_, value) ->
            jsonElementToAny(value)
        }
    }

    private fun String.limitForReport(maxLength: Int = MAX_RESULT_LENGTH): String {
        return if (length > maxLength) {
            "${take(maxLength)}..."
        } else {
            this
        }
    }

    /**
     * Sends messages to AI and returns the response.
     */
    private suspend fun sendToAi(
        messages: List<ApiChatMessage>,
        model: String,
        temperature: Float,
    ): AiCallResult {
        val request = createConversationRequest(
            messages = messages,
            model = model,
            temperature = temperature
        )

        return when (val result = chatApiService.sendChatCompletion(request)) {
            is ApiResult.Success -> {
                val resultString = result.data.choices.firstOrNull()?.message?.content
                    ?: return AiCallResult.Error("Empty response from AI")

                val responseContent = json.decodeFromString<ResponseContent>(resultString)
                AiCallResult.Success(
                    content = responseContent,
                    model = result.data.model,
                    usage = result.data.usage
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
