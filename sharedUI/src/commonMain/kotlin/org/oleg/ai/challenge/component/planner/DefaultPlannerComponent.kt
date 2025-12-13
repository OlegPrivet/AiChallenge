package org.oleg.ai.challenge.component.planner

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.MessageRole
import org.oleg.ai.challenge.data.model.PlannerFrequency
import org.oleg.ai.challenge.data.network.service.ChatOrchestratorService
import org.oleg.ai.challenge.data.network.service.McpClientService
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DefaultPlannerComponent(
    componentContext: ComponentContext,
    private val mcpClientService: McpClientService,
    private val chatOrchestratorService: ChatOrchestratorService,
    private val onNavigateBack: () -> Unit,
    private val logger: Logger = Logger.withTag("PlannerComponent")
) : PlannerComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // State
    private val _messages = MutableValue<List<ChatMessage>>(emptyList())
    override val messages: Value<List<ChatMessage>> = _messages

    private val _inputText = MutableValue("")
    override val inputText: Value<String> = _inputText

    private val _isLoading = MutableValue(false)
    override val isLoading: Value<Boolean> = _isLoading

    private val _showInputField = MutableValue(true)
    override val showInputField: Value<Boolean> = _showInputField

    // MCP tools
    private val _availableTools = MutableValue<List<McpClientService.ToolInfo>>(emptyList())
    override val availableTools: Value<List<McpClientService.ToolInfo>> = _availableTools

    private val _selectedToolName = MutableValue("")
    override val selectedToolName: Value<String> = _selectedToolName

    // Frequency settings
    private val _selectedFrequency = MutableValue(PlannerFrequency.DEFAULT)
    override val selectedFrequency: Value<PlannerFrequency> = _selectedFrequency

    private val _isPeriodicRunning = MutableValue(false)
    override val isPeriodicRunning: Value<Boolean> = _isPeriodicRunning

    // Internal state
    private var userMessage: String = ""
    private var systemPrompt: String = """
        Ты агент, который умеет формировать запросы по описанию инструментов MCP сервера и обрабатывать ответы, полученные от MCP сервера
        Если в тексте пользователя есть информация, которая подходит под описание инструмента MCP сервера, тебе нужно вернуть в ответе ТОЛЬКО объект с данными, согласно описанию инструмента
        Если в новых сообщениях пользователя есть информация, которая подходит под описание инструмента MCP сервера, то нужно вернуть в ответе ТОЛЬКО объект с данными, согласно описанию инструмента
        Если пользоваетль в последнем сообщении просит сформулировать ответ, то выдай итоговый ответ
    """.trimIndent()
    private var periodicJob: Job? = null

    init {
        // Observe available tools from MCP client service
        mcpClientService.availableTools
            .onEach { tools ->
                _availableTools.value = tools
                logger.d { "Available tools updated: ${tools.size}" }
            }
            .launchIn(scope)
    }

    override fun onTextChanged(text: String) {
        _inputText.value = text
    }

    override fun onSendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        userMessage = text
        _inputText.value = ""
        _showInputField.value = false

        // Execute first AI request
        executeAiRequest()
    }

    override fun onSelectTool(tool: McpClientService.ToolInfo?) {
        _selectedToolName.value = tool?.name ?: ""
        logger.d { "Selected tool: ${tool?.name ?: "none"}" }
    }

    override fun onFrequencyChanged(frequency: PlannerFrequency) {
        _selectedFrequency.value = frequency
        logger.d { "Frequency changed to: ${frequency.displayName}" }
    }

    override fun onStartPeriodic() {
        if (_isPeriodicRunning.value || userMessage.isEmpty()) return

        _isPeriodicRunning.value = true
        logger.d { "Starting periodic execution with frequency: ${_selectedFrequency.value.displayName}" }

        // Use coroutines for periodic task scheduling
        periodicJob = scope.launch {
            while (_isPeriodicRunning.value) {
                executeAiRequest()
                delay(_selectedFrequency.value.milliseconds)
            }
        }
    }

    override fun onStopPeriodic() {
        _isPeriodicRunning.value = false
        periodicJob?.cancel()
        periodicJob = null
        logger.d { "Stopped periodic execution" }
    }

    override fun onBack() {
        // Stop any running periodic tasks
        onStopPeriodic()
        onNavigateBack()
    }

    @OptIn(ExperimentalTime::class)
    private fun executeAiRequest() {
        if (_isLoading.value) return
        if (userMessage.isEmpty()) return

        scope.launch {
            _isLoading.value = true

            try {
                // Build conversation history with only system prompt and user message
                val conversationHistory = buildConversationHistory()

                // Use ChatOrchestratorService for AI request with MCP support
                val result = chatOrchestratorService.handleUserMessage(
                    conversationHistory = conversationHistory,
                    model = BuildConfig.DEFAULT_MODEL,
                    temperature = 0.7f,
                    _selectedToolName.value
                )

                when (result) {
                    is ChatOrchestratorService.OrchestratorResult.Success -> {
                        // Add AI response to messages (visible in UI)
                        val aiMessage = ChatMessage(
                            id = "${Clock.System.now()}_ai_${(0..10000).random()}",
                            text = result.finalResponse,
                            isFromUser = false,
                            role = MessageRole.ASSISTANT,
                            isVisibleInUI = true,
                            modelUsed = result.model,
                            usage = result.usage
                        )
                        _messages.value = _messages.value + aiMessage
                        logger.d { "AI response received: ${result.finalResponse.take(100)}..." }
                    }

                    is ChatOrchestratorService.OrchestratorResult.Error -> {
                        // Add error message
                        val errorMessage = ChatMessage(
                            id = "${Clock.System.now()}_error_${(0..10000).random()}",
                            text = "Error: ${result.message}",
                            isFromUser = false,
                            role = MessageRole.ASSISTANT,
                            isVisibleInUI = true
                        )
                        _messages.value = _messages.value + errorMessage
                        logger.e { "AI request error: ${result.message}" }
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Exception during AI request" }
                val errorMessage = ChatMessage(
                    id = "${Clock.System.now()}_exception_${(0..10000).random()}",
                    text = "Error: ${e.message}",
                    isFromUser = false,
                    role = MessageRole.ASSISTANT,
                    isVisibleInUI = true
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun buildConversationHistory(): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        // Add system prompt
        messages.add(
            ChatMessage(
                id = "${Clock.System.now()}_system",
                text = systemPrompt,
                isFromUser = false,
                role = MessageRole.SYSTEM,
                isVisibleInUI = false
            )
        )

        // Note: With native tool calling, tool system prompts are no longer needed.
        // Tools are passed directly via ChatRequest.tools field.

        // Add user message
        messages.add(
            ChatMessage(
                id = "${Clock.System.now()}_user",
                text = userMessage,
                isFromUser = true,
                role = MessageRole.USER,
                isVisibleInUI = false
            )
        )

        return messages
    }
}
