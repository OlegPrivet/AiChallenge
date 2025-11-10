package org.oleg.ai.challenge.component.chat

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.oleg.ai.challenge.data.AgentManager
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.network.ApiResult
import org.oleg.ai.challenge.data.network.createConversationRequest
import org.oleg.ai.challenge.data.network.service.ChatApiService
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DefaultChatComponent(
    componentContext: ComponentContext,
    private val chatApiService: ChatApiService,
    private val agentManager: AgentManager,
    private val onNavigateBack: () -> Unit
) : ChatComponent, ComponentContext by componentContext {

    private val logger = Logger.withTag("DefaultChatComponent")
    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // Store messages per agent
    private val messagesByAgent = mutableMapOf<String, MutableList<ChatMessage>>()

    private val _messages = MutableValue<List<ChatMessage>>(emptyList())
    override val messages: Value<List<ChatMessage>> = _messages

    private val _inputText = MutableValue("")
    override val inputText: Value<String> = _inputText

    private val _isLoading = MutableValue(false)
    override val isLoading: Value<Boolean> = _isLoading

    private val _availableAgents = MutableValue<List<Agent>>(emptyList())
    override val availableAgents: Value<List<Agent>> = _availableAgents

    private val _selectedAgent = MutableValue(agentManager.getMainAgent()!!)
    override val selectedAgent: Value<Agent> = _selectedAgent

    private val _currentAgentModel = MutableValue("")
    override val currentAgentModel: Value<String> = _currentAgentModel

    private val _currentTemperature = MutableValue(1.0f)
    override val currentTemperature: Value<Float> = _currentTemperature

    init {
        // Setup available agents
        val allAgents = agentManager.getAllAgents()
        _availableAgents.value = allAgents

        // Initialize message storage for each agent
        allAgents.forEach { agent ->
            val agentMessages = mutableListOf<ChatMessage>()

            // Add system prompt if exists
            agent.systemPrompt?.let { prompt ->
                agentMessages.add(
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
                agentMessages.add(
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

            messagesByAgent[agent.id] = agentMessages
        }

        // Show main agent's messages initially
        updateDisplayedMessages()
        updateCurrentAgentModel()
        updateCurrentTemperature()
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

        // Add user message to the current agent's history
        val userMessage = ChatMessage(
            id = generateId(),
            text = text,
            isFromUser = true,
            role = MessageRole.USER,
            isVisibleInUI = true,
            agentName = currentAgent.name,
            agentId = currentAgentId
        )

        messagesByAgent[currentAgentId]?.add(userMessage)
        updateDisplayedMessages()
        _inputText.value = ""
        _isLoading.value = true

        logger.d { "Sending user message to agent $currentAgentId: $text" }

        // Send API request
        scope.launch {
            try {
                // Get messages to send based on whether this is the main agent
                val messagesToSend = if (currentAgentId == "main") {
                    // Main agent gets ALL messages from all agents, sorted by timestamp
                    getAllMessagesMerged()
                } else {
                    // Sub-agent gets only messages related to this specific agent:
                    // - Its system/assistant prompts (isVisibleInUI = false)
                    // - Only user messages and AI responses for THIS agent (agentId matches)
                    getAllMessagesMerged().filter { message ->
                        message.agentId == currentAgentId
                    }
                }

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

                // Create request with the agent's model and temperature
                val request = createConversationRequest(
                    messages = apiMessages,
                    model = currentAgent.model,
                    temperature = currentAgent.temperature
                )
                val result = chatApiService.sendChatCompletion(request)

                when (result) {
                    is ApiResult.Success -> {
                        if (result.data.choices.isEmpty()) {
                            logger.w { "AI response content is null" }
                            addErrorMessage(currentAgentId, "No response received from AI")
                        } else {
                            result.data.choices.forEach { choice ->
                                val aiMessage = ChatMessage(
                                    id = generateId(),
                                    text = choice.message.content,
                                    isFromUser = false,
                                    role = MessageRole.ASSISTANT,
                                    isVisibleInUI = true,
                                    agentName = currentAgent.name,
                                    agentId = currentAgentId,
                                    modelUsed = result.data.model
                                )
                                messagesByAgent[currentAgentId]?.add(aiMessage)
                            }
                            updateDisplayedMessages()
                        }
                    }

                    is ApiResult.Error -> {
                        logger.e { "API error: ${result.error.getDescription()}" }
                        addErrorMessage(currentAgentId, "Error: ${result.error.getDescription()}")
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

    override fun onNavigateBack() {
        agentManager.clear()
        onNavigateBack.invoke()
    }

    /**
     * Updates the displayed messages.
     * ALWAYS shows all messages merged chronologically regardless of selected agent.
     * Agent selection only affects which messages are sent in API requests.
     */
    private fun updateDisplayedMessages() {
        // Always show all messages from all agents merged and sorted by timestamp
        _messages.value = getAllMessagesMerged()
    }

    /**
     * Gets all messages from all agents merged and sorted chronologically by timestamp.
     */
    private fun getAllMessagesMerged(): List<ChatMessage> {
        return messagesByAgent.values
            .flatten()
            .sortedBy { it.timestamp }
    }

    /**
     * Adds an error message to the specified agent's chat history.
     */
    private fun addErrorMessage(agentId: String, errorText: String) {
        val errorMessage = ChatMessage(
            id = generateId(),
            text = errorText,
            isFromUser = false,
            agentId = agentId
        )
        messagesByAgent[agentId]?.add(errorMessage)
        updateDisplayedMessages()
    }

    @OptIn(ExperimentalTime::class)
    private fun generateId(): String =
        "${Clock.System.now()}_${(0..1000).random()}"
}
