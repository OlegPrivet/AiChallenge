package org.oleg.ai.challenge.component.chat

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.model.ChatMessage

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
    fun onSummarizeConversation()
    fun onAgentSelected(agentId: String?)
    fun onModelChanged(model: String)  // Change model for current agent
    fun onTemperatureChanged(temperature: Float)  // Change temperature for current agent
}
