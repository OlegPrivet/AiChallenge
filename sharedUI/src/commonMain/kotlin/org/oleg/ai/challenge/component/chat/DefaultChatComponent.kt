package org.oleg.ai.challenge.component.chat

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.oleg.ai.challenge.data.AgentManager
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.McpUiState
import org.oleg.ai.challenge.data.model.MessageRole
import org.oleg.ai.challenge.data.network.ApiResult
import org.oleg.ai.challenge.data.network.createConversationRequest
import org.oleg.ai.challenge.data.network.service.ChatApiService
import org.oleg.ai.challenge.data.network.service.ChatOrchestratorService
import org.oleg.ai.challenge.data.network.service.McpClientService
import org.oleg.ai.challenge.data.repository.ChatRepository
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

class DefaultChatComponent(
    componentContext: ComponentContext,
    private val chatApiService: ChatApiService,
    private val chatRepository: ChatRepository,
    private val agentManager: AgentManager,
    private val chatOrchestratorService: ChatOrchestratorService,
    private val mcpClientService: McpClientService,
    private val chatId: Long? = null,
) : ChatComponent, ComponentContext by componentContext {

    private val logger = Logger.withTag("DefaultChatComponent")
    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // Single list for all messages (sorted by timestamp)
    private val allMessages = mutableListOf<ChatMessage>()

    private val _messages = MutableValue<List<ChatMessage>>(emptyList())
    override val messages: Value<List<ChatMessage>> = _messages

    private val _inputText = MutableValue("")
    override val inputText: Value<String> = _inputText

    private val _isLoading = MutableValue(false)
    override val isLoading: Value<Boolean> = _isLoading

    private val _availableAgents = MutableValue<List<Agent>>(emptyList())
    override val availableAgents: Value<List<Agent>> = _availableAgents

    private val _selectedAgent = MutableValue(
        agentManager.getMainAgent() ?: Agent.createMain(model = "gpt-4o-mini")
    )
    override val selectedAgent: Value<Agent> = _selectedAgent

    private val _currentAgentModel = MutableValue("")
    override val currentAgentModel: Value<String> = _currentAgentModel

    private val _currentTemperature = MutableValue(1.0f)
    override val currentTemperature: Value<Float> = _currentTemperature

    private val _mcpUiState = MutableValue(McpUiState())
    override val mcpUiState: Value<McpUiState> = _mcpUiState

    // RAG-related state
    private val _isRagEnabled = MutableValue(false)
    override val isRagEnabled: Value<Boolean> = _isRagEnabled

    private var _selectedCitationSource: MutableValue<CitationState> = MutableValue(CitationState.None)
    override val selectedCitationSource: Value<CitationState> = _selectedCitationSource

    private val _isDeveloperModeEnabled = MutableValue(false)
    override val isDeveloperModeEnabled: Value<Boolean> = _isDeveloperModeEnabled


    init {
        // Observe MCP UI state changes from the orchestrator
        scope.launch {
            setZygotePrompt()

            chatOrchestratorService.mcpUiState
                .onEach { state ->
                    _mcpUiState.value = state
                }
                .launchIn(scope)

            // If chatId is provided, load from repository
            if (chatId != null) {
                loadFromRepository(chatId)
            }

            setupFromAgentManager()

            // Show main agent's messages initially
            updateDisplayedMessages()
            updateCurrentAgentModel()
            updateCurrentTemperature()

            // Inject tool system prompts when tools become available
            mcpClientService.availableTools
                .onEach { tools ->
                    if (tools.isNotEmpty()) {
                        injectToolSystemPrompts(tools)
                    }
                }
                .launchIn(scope)
        }
    }

    /**
     * Injects system prompts for available MCP tools into the message history.
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun injectToolSystemPrompts(tools: List<McpClientService.ToolInfo>) {
        // Remove existing tool prompts
        allMessages.removeAll { it.isMcpSystemPrompt }

        allMessages.add(
            0, ChatMessage(
                id = "${System.now()}_tool_system",
                text = """In the instructions for the user, YOU MUST request the following tools""".trimIndent(),
                isFromUser = false,
                timestamp = 0,
                role = MessageRole.SYSTEM,
                isVisibleInUI = false,
                agentName = "",
                agentId = "",
                modelUsed = "",
                usage = null,
                mcpName = "",
                isMcpSystemPrompt = true,
                isMcpIntermediate = false

            )
        )
        // Create and add new tool prompts (no agentId - they're global)
        val toolPrompts = chatOrchestratorService.createToolSystemPrompts(
            enabledTools = tools,
            agentId = null
        )

        // Insert tool prompts at the beginning (before all other messages)
        allMessages.addAll(toolPrompts)
        chatRepository.saveMessages(chatId!!, allMessages)

        logger.d { "Injected ${toolPrompts.size} tool system prompts" }

        // Update displayed messages after injection
        updateDisplayedMessages()
    }

    /**
     * Load agents and messages from the repository for an existing chat.
     */
    private suspend fun loadFromRepository(chatId: Long) {
        try {
            // Load chat entity to get RAG state
            val conversation = chatRepository.getConversationById(chatId)
            if (conversation != null) {
                _isRagEnabled.value = conversation.isRagEnabled
                logger.d { "Loaded chat $chatId with RAG mode: ${conversation.isRagEnabled}" }
            }

            // Load agents
            val agents = chatRepository.getAgentsForChatSuspend(chatId)
            if (agents.isEmpty()) {
                logger.w { "No agents found for chat $chatId, falling back to AgentManager" }
                setupFromAgentManager()
                return
            }

            _availableAgents.value = agents

            // Set selected agent to main agent if available
            val mainAgent = agents.firstOrNull { it.id == "main" } ?: agents.first()
            _selectedAgent.value = mainAgent

            // Load messages directly into allMessages
            val messages = chatRepository.getMessagesForChatSuspend(chatId)
            allMessages.clear()
            allMessages.addAll(messages.sortedBy { it.timestamp })

            // Update displayed messages after loading from repository
            updateDisplayedMessages()

            logger.d { "Loaded chat $chatId with ${agents.size} agents and ${messages.size} messages" }

            // Listen to reactive updates from repository
            chatRepository.getMessagesForChat(chatId)
                .onEach { updatedMessages ->
                    // Only update if messages changed from external source
                    if (updatedMessages.size != allMessages.size) {
                        logger.d { "Messages updated from repository, reloading..." }
                        allMessages.clear()
                        updateDisplayedMessages()
                        allMessages.addAll(updatedMessages.sortedBy { it.timestamp })
                        updateDisplayedMessages()
                    }
                }
                .launchIn(scope)

        } catch (e: Exception) {
            logger.e(e) { "Failed to load chat $chatId from repository" }
            setupFromAgentManager()
        }
    }

    /**
     * Setup component using AgentManager (legacy flow for new chats).
     */
    private suspend fun setupFromAgentManager() {
        val agents = chatRepository.getAgentsForChatSuspend(chatId!!)
        _availableAgents.value = agents

        val agentIds = agents.map { it.id }
        val messagesAgentIds = allMessages.map { it.agentId }
        if (messagesAgentIds.any { agentIds.contains(it) }) return

        agents.forEach { agent ->
            // Add system prompt if exists
            agent.systemPrompt?.let { prompt ->
                allMessages.add(
                    ChatMessage(
                        id = generateId(),
                        text = prompt,
                        isFromUser = false,
                        role = MessageRole.SYSTEM,
                        isVisibleInUI = false,
                        agentId = agent.id
                    )
                )
            }

            // Add assistant prompt if exists
            agent.assistantPrompt?.let { prompt ->
                allMessages.add(
                    ChatMessage(
                        id = generateId(),
                        text = prompt,
                        isFromUser = false,
                        role = MessageRole.ASSISTANT,
                        isVisibleInUI = false,
                        agentId = agent.id
                    )
                )
            }
        }
    }

    override fun onTextChanged(text: String) {
        _inputText.value = text
    }

    override fun onAgentSelected(agentId: String?) {
        agentId ?: return
        val selectedAgent = agentManager.getAgent(agentId) ?: return
        _selectedAgent.value = selectedAgent
        updateDisplayedMessages()
        updateCurrentAgentModel()
        updateCurrentTemperature()
    }

    override fun onModelChanged(model: String) {
        val currentAgent = getCurrentAgent()

        // Update the agent's model
        val updatedAgent = currentAgent.copy(model = model)

        // Update in AgentManager
        if (currentAgent.id == "main") {
            agentManager.setMainAgent(updatedAgent)
        } else {
            agentManager.updateSubAgent(updatedAgent)
        }

        _currentAgentModel.value = model
    }

    override fun onTemperatureChanged(temperature: Float) {
        val currentAgent = getCurrentAgent()

        // Update the agent's temperature
        val updatedAgent = currentAgent.copy(temperature = temperature)

        // Update in AgentManager
        if (currentAgent.id == "main") {
            agentManager.setMainAgent(updatedAgent)
        } else {
            agentManager.updateSubAgent(updatedAgent)
        }
        _selectedAgent.update { it.copy(temperature = temperature) }

        _currentTemperature.value = temperature
    }

    // Convenience method for getting current agent ID
    private fun getCurrentAgent(): Agent = _selectedAgent.value

    // Update the displayed model for the current agent
    private fun updateCurrentAgentModel() {
        val currentAgent = getCurrentAgent()
        _currentAgentModel.value = currentAgent.model
    }

    // Update the displayed temperature for the current agent
    private fun updateCurrentTemperature() {
        val currentAgent = getCurrentAgent()
        _currentTemperature.value = currentAgent.temperature
    }

    override fun onSendMessage() {
        val text = _inputText.value
        if (text.isEmpty() || _isLoading.value) return

        val currentAgent = getCurrentAgent()
        val currentAgentId = currentAgent.id

        // Add user message to the message history
        val userMessage = ChatMessage(
            id = generateId(),
            text = text,
            isFromUser = true,
            role = MessageRole.USER,
            isVisibleInUI = true,
            agentName = currentAgent.name,
            agentId = currentAgentId
        )

        allMessages.add(userMessage)
        updateDisplayedMessages()
        _inputText.value = ""
        _isLoading.value = true

        // Save user message to repository if chatId is available
        if (chatId != null) {
            scope.launch {
                try {
                    chatRepository.saveMessage(chatId, userMessage)
                    // Update chat name from first message
                    chatRepository.updateChatNameFromFirstMessage(chatId)
                    logger.d { "Saved user message to chat $chatId" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to save user message to repository" }
                }
            }
        }

        logger.d { "Sending user message to agent $currentAgentId: $text" }

        // Send API request through orchestrator
        scope.launch {
            try {
                // Get messages to send based on whether this is the main agent
                val messagesToSend = if (currentAgentId == "main") {
                    // Main agent gets ALL messages
                    allMessages.toList()
                } else {
                    // Sub-agent gets only messages related to this specific agent
                    // plus MCP system prompts (which have no agentId)
                    allMessages.filter { message ->
                        message.agentId == currentAgentId ||
                                message.agentId == null ||
                                message.isMcpSystemPrompt
                    }
                }

                // Use orchestrator to handle message with MCP integration
                val result = chatOrchestratorService.handleUserMessage(
                    conversationHistory = messagesToSend,
                    model = currentAgent.model,
                    temperature = currentAgent.temperature,
                    toolName = "",
                    isRagEnabled = _isRagEnabled.value
                )

                when (result) {
                    is ChatOrchestratorService.OrchestratorResult.Success -> {
                        val aiMessage = ChatMessage(
                            id = generateId(),
                            text = result.finalResponse,
                            isFromUser = false,
                            role = MessageRole.ASSISTANT,
                            isVisibleInUI = true,
                            agentName = currentAgent.name,
                            agentId = currentAgentId,
                            modelUsed = result.model,
                            usage = result.usage,
                            citations = result.citations,
                            retrievalTrace = result.retrievalTrace
                        )
                        allMessages.add(aiMessage)

                        // Save AI message to repository if chatId is available
                        if (chatId != null) {
                            scope.launch {
                                try {
                                    chatRepository.saveMessage(chatId, aiMessage)
                                    logger.d { "Saved AI message to chat $chatId" }
                                } catch (e: Exception) {
                                    logger.e(e) { "Failed to save AI message to repository" }
                                }
                            }
                        }
                        updateDisplayedMessages()
                    }

                    is ChatOrchestratorService.OrchestratorResult.Error -> {
                        logger.e { "Orchestrator error: ${result.message}" }
                        addErrorMessage(currentAgentId, "Error: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Unexpected error during API call" }
                addErrorMessage(currentAgentId, "Unexpected error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onSummarizeConversation() {
        if (_isLoading.value) return

        val currentAgent = getCurrentAgent()
        val currentAgentId = currentAgent.id

        // Create hidden summary request message
        val summaryRequestMessage = ChatMessage(
            id = generateId(),
            text = "Сделай краткое резюме нашей истории общения: выдели основные темы, ключевые решения, важные детали и следующие шаги, если они упоминались.",
            isFromUser = true,
            role = MessageRole.USER,
            isVisibleInUI = false,  // Hidden from UI
            agentId = currentAgentId
        )

        allMessages.add(summaryRequestMessage)
        _isLoading.value = true

        // Save hidden summary request message to repository if chatId is available
        if (chatId != null) {
            scope.launch {
                try {
                    chatRepository.saveMessage(chatId, summaryRequestMessage)
                    logger.d { "Saved summary request message to chat $chatId" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to save summary request message to repository" }
                }
            }
        }

        logger.d { "Requesting conversation summary from agent $currentAgentId" }

        scope.launch {
            try {
                // Get all messages for summary
                val messagesToSend = allMessages.toList()

                // Convert UI messages to API messages
                val apiMessages = messagesToSend.map { uiMessage ->
                    val apiRole = when (uiMessage.role) {
                        MessageRole.SYSTEM -> org.oleg.ai.challenge.data.network.model.MessageRole.SYSTEM
                        MessageRole.ASSISTANT -> org.oleg.ai.challenge.data.network.model.MessageRole.ASSISTANT
                        MessageRole.USER -> org.oleg.ai.challenge.data.network.model.MessageRole.USER
                        null -> if (uiMessage.isFromUser) {
                            org.oleg.ai.challenge.data.network.model.MessageRole.USER
                        } else {
                            org.oleg.ai.challenge.data.network.model.MessageRole.ASSISTANT
                        }
                    }
                    org.oleg.ai.challenge.data.network.model.ChatMessage(
                        role = apiRole,
                        content = uiMessage.text
                    )
                }

                val request = createConversationRequest(
                    messages = apiMessages,
                    model = currentAgent.model,
                    temperature = currentAgent.temperature
                )
                val result = chatApiService.sendChatCompletion(request)

                when (result) {
                    is ApiResult.Success -> {
                        val summaryText = result.data.choices.firstOrNull()?.message?.content
                        if (summaryText != null) {
                            // Add summary message (visible)
                            val summaryMessage = ChatMessage(
                                id = generateId(),
                                text = summaryText,
                                isFromUser = false,
                                role = MessageRole.ASSISTANT,
                                isVisibleInUI = true,
                                agentName = currentAgent.name,
                                agentId = currentAgentId,
                                modelUsed = result.data.model,
                                usage = result.data.usage
                            )

                            // Save summary message to repository if chatId is available
                            if (chatId != null) {
                                scope.launch {
                                    try {
                                        chatRepository.saveMessage(chatId, summaryMessage)
                                        logger.d { "Saved summary message to chat $chatId" }
                                    } catch (e: Exception) {
                                        logger.e(e) { "Failed to save summary message to repository" }
                                    }
                                }
                            }

                            // Clear history and rebuild with preserved messages
                            clearHistoryKeepingPrompts(currentAgentId, summaryMessage)
                            logger.d { "Conversation summarized and history cleared for agent $currentAgentId" }
                        } else {
                            logger.w { "Summary response is null" }
                            addErrorMessage(currentAgentId, "Failed to generate summary")
                        }
                    }

                    is ApiResult.Error -> {
                        logger.e { "Summary API error: ${result.error.getDescription()}" }
                        addErrorMessage(currentAgentId, "Summary failed: ${result.error.getDescription()}")
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Unexpected error during summary request" }
                addErrorMessage(currentAgentId, "Summary error: ${e.message ?: "Unknown error"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears the message history while preserving:
     * - System prompts (role = SYSTEM)
     * - Assistant prompts (role = ASSISTANT, not visible)
     * - MCP system prompts
     * - The provided summary message
     */
    private fun clearHistoryKeepingPrompts(agentId: String, summaryMessage: ChatMessage) {
        // Keep only system prompts, assistant prompts, and MCP prompts
        val preservedMessages = allMessages.filter { message ->
            message.role == MessageRole.SYSTEM ||
                    (message.role == MessageRole.ASSISTANT && !message.isVisibleInUI) ||
                    message.isMcpSystemPrompt
        }.toMutableList()

        // Add the summary
        preservedMessages.add(summaryMessage)

        // Replace all messages
        allMessages.clear()
        allMessages.addAll(preservedMessages)

        // Update displayed messages
        updateDisplayedMessages()
    }

    /**
     * Updates the displayed messages.
     * Shows all messages sorted chronologically by timestamp.
     */
    private fun updateDisplayedMessages() {
        _messages.value = allMessages.sortedBy { it.timestamp }
    }

    /**
     * Adds an error message to the chat history.
     */
    private fun addErrorMessage(agentId: String, errorText: String) {
        val errorMessage = ChatMessage(
            id = generateId(),
            text = errorText,
            isFromUser = false,
            agentId = agentId
        )
        allMessages.add(errorMessage)
        updateDisplayedMessages()
    }

    @OptIn(ExperimentalTime::class)
    private fun generateId(): String =
        "${System.now()}_${(0..1000).random()}_${(0..500).random()}"

    private suspend fun setZygotePrompt() {
        val messages = chatRepository.getMessagesForChatSuspend(chatId!!)
        if (messages.any { it.id == "RULE 1" || messages.any { it.id == "RULE 2" } }) return

        allMessages.clear()
        allMessages.add(
            ChatMessage(
                id = "RULE 1",
                text = """
#MAIN RULE
IN YOUR RESPONSES, YOU MUST ALWAYS FOLLOW THE RESPONSE STRUCTURE IN THE JSON OBJECT
THE CONTENT FIELD IN THE RESPONSE MUST BE OF TYPE JSON OBJECT
{
  "type": "object",
  "name": "ResponseContent",
  "description": "Container with a message and optional list of execution instructions.",
  "properties": {
    "message": {
      "type": "string",
      "description": "Primary text message returned by the system."
    },
    "instructions": {
      "type": "array",
      "description": "Optional ordered list of instructions to perform.",
      "items": {
        "type": "object",
        "description": "Polymorphic instruction. Selected by the `type` discriminator.",
        "discriminator": {
          "propertyName": "type",
          "mapping": {
            "CallMCPTool": "#/components/schemas/CallMCPTool",
            "CallAi": "#/components/schemas/CallAi"
          }
        },
        "oneOf": [
          {
            "type": "object",
            "name": "CallMCPTool",
            "description": "Instruction to call an MCP tool with arguments.",
            "properties": {
              "type": {
                "type": "string",
                "const": "CallMCPTool"
              },
              "name": {
                "type": "string",
                "description": "The name of the tool to invoke."
              },
              "arguments": {
                "type": "object",
                "description": "Raw JSON object of tool arguments."
              },
              "isCompleted": {
                "type": "boolean",
                "description": "Indicates whether this instruction has completed."
              }
            },
            "required": ["type", "name", "arguments", "isCompleted"]
          },
          {
            "type": "object",
            "name": "CallAi",
            "description": "Instruction to execute an AI step and compare expected vs actual output.",
            "properties": {
              "type": {
                "type": "string",
                "const": "CallAi"
              },
              "expectedResultOfInstruction": {
                "type": "string",
                "description": "Description of the expected output from this AI instruction."
              },
              "actualResultOfInstruction": {
                "type": "string",
                "description": "Actual output produced by the AI instruction."
              },
              "isCompleted": {
                "type": "boolean",
                "description": "Indicates whether this instruction has completed."
              }
            },
            "required": ["type", "expectedResultOfInstruction", "isCompleted"]
          }
        ]
      }
    }
  },
  "required": ["message"]
}
IF YOU SEND A DIFFERENT TYPE OF RESPONSE, I WILL NOT BE ABLE TO PROCESS IT AND THIS WILL CAUSE THE PROGRAM TO CRASH.
IF YOU HAVE RECEIVED ANSWERS TO ALL QUESTIONS AND YOU HAVE ENOUGH INFORMATION FOR A FINAL ANSWER, THEN THE LIST OF INSTRUCTIONS SHOULD BE SENT NULL.
                """.trimIndent(),
                isFromUser = false,
                timestamp = 0,
                role = MessageRole.SYSTEM,
                isVisibleInUI = false,
                agentName = "",
                agentId = "",
                modelUsed = "",
                usage = null,
                mcpName = "",
                isMcpSystemPrompt = false,
                isMcpIntermediate = false
            )
        )

        allMessages.add(
            ChatMessage(
                id = "RULE 2",
                text = """
                    You are an agent - please keep going until the user’s query is completely resolved, before ending your turn and yielding back to the user. Only terminate your turn when you are sure that the problem is solved.
                    If you are not sure about file content or codebase structure pertaining to the user’s request, use your tools to read files and gather the relevant information: do NOT guess or make up an answer.
                    You MUST plan extensively before each function call, and reflect extensively on the outcomes of the previous function calls. DO NOT do this entire process by making function calls only, as this can impair your ability to solve the problem and think insightfully.
                    To solve the problem, YOU MUST write instructions for execution and present them to the user in your answer.
                """.trimIndent(),
                isFromUser = false,
                timestamp = 0,
                role = MessageRole.SYSTEM,
                isVisibleInUI = false,
                agentName = "",
                agentId = "",
                modelUsed = "",
                usage = null,
                mcpName = "",
                isMcpSystemPrompt = false,
                isMcpIntermediate = false
            )
        )
        chatRepository.saveMessages(chatId!!, allMessages)
    }

    // RAG-related methods

    override fun onToggleRagMode(enabled: Boolean) {
        _isRagEnabled.value = enabled

        // Save to repository if chatId exists
        if (chatId != null) {
            scope.launch {
                try {
                    chatRepository.updateChatRagMode(chatId, enabled)
                    logger.d { "Updated RAG mode for chat $chatId: $enabled" }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to update RAG mode for chat $chatId" }
                }
            }
        }
    }

    override fun onShowSource(citation: org.oleg.ai.challenge.domain.rag.orchestrator.Citation, chunkContent: String) {
        _selectedCitationSource.value = CitationState.Detail(
            CitationSourceDetail(
                citation = citation,
                chunkContent = chunkContent
            )
        )
    }

    override fun onHideSource() {
        _selectedCitationSource.value = CitationState.None
    }

    override fun onToggleDeveloperMode(enabled: Boolean) {
        _isDeveloperModeEnabled.value = enabled
    }
}
