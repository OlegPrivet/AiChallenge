package org.oleg.ai.challenge.component.chat

import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import org.oleg.ai.challenge.data.model.Agent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface ChatComponent {
    val messages: Value<List<ChatMessage>>
    val inputText: Value<String>
    val isLoading: Value<Boolean>

    val availableAgents: Value<List<Agent>>  // List of agent IDs (including "main")
    val selectedAgent: Value<Agent>  // Currently selected agent ID
    val currentAgentModel: Value<String>  // Model of the currently selected agent
    val currentTemperature: Value<Float>  // Temperature of the currently selected agent

    fun onTextChanged(text: String)
    fun onSendMessage()
    fun onAgentSelected(agentId: String?)
    fun onModelChanged(model: String)  // Change model for current agent
    fun onTemperatureChanged(temperature: Float)  // Change temperature for current agent
    fun onNavigateBack()
}

data class ChatMessage @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val role: MessageRole? = null,
    val isVisibleInUI: Boolean = true,
    val agentName: String? = null,  // ID of the agent that sent/received this message
    val agentId: String? = null,  // ID of the agent that sent/received this message
    val modelUsed: String? = null,  // AI model used to generate this response
    val usage: org.oleg.ai.challenge.data.network.model.Usage? = null  // Token usage statistics
)

sealed class InputText {
    data class User(val text: String) : InputText()
    @Serializable
    data class Assistant(
        val prompt: String? = null,
        val header: String? = null,
        val content: String? = null,
    ) : InputText()
    data class System(val text: String) : InputText()
}

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}
