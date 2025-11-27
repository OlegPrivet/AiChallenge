package org.oleg.ai.challenge.component.chat

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.McpUiState

class PreviewChatComponent(
    override val messages: Value<List<ChatMessage>> = MutableValue(
        listOf(
            ChatMessage(id = "1", text = "Hello!", isFromUser = true),
            ChatMessage(id = "2", text = "Hi there! How can I help you?", isFromUser = false, modelUsed = "gpt-4"),
            ChatMessage(id = "3", text = "This is a preview", isFromUser = true)
        )
    ),
    override val inputText: Value<String> = MutableValue(""),
    override val isLoading: Value<Boolean> = MutableValue(false),
    override val availableAgents: Value<List<Agent>> = MutableValue(listOf(Agent(
        id = "per",
        name = "Rochelle Mills",
        systemPrompt = "consetetur",
        assistantPrompt = "impetus",
        model = "consetetur",
        temperature = 1.0f,
        timestamp = 5313
    ))),
    override val selectedAgent: Value<Agent> = MutableValue(Agent(
        id = "per",
        name = "Rochelle Mills",
        systemPrompt = "consetetur",
        assistantPrompt = "impetus",
        model = "consetetur",
        temperature = 1.0f,
        timestamp = 5313
    )),
    override val currentAgentModel: Value<String> = MutableValue("gpt-4"),
    override val currentTemperature: Value<Float> = MutableValue(1.0f),
    override val mcpUiState: Value<McpUiState> = MutableValue(McpUiState()),
    override val isRagEnabled: Value<Boolean> = MutableValue(false),
    override val isDeveloperModeEnabled: Value<Boolean> = MutableValue(false),
    override val selectedCitationSource: Value<CitationState> = MutableValue(CitationState.None)
) : ChatComponent {
    override fun onTextChanged(text: String) = Unit
    override fun onSendMessage() = Unit
    override fun onAgentSelected(agentId: String?) = Unit
    override fun onModelChanged(model: String) = Unit
    override fun onTemperatureChanged(temperature: Float) = Unit
    override fun onSummarizeConversation() = Unit
    override fun onToggleRagMode(enabled: Boolean) = Unit
    override fun onShowSource(citation: org.oleg.ai.challenge.domain.rag.orchestrator.Citation, chunkContent: String) = Unit
    override fun onHideSource() = Unit
    override fun onToggleDeveloperMode(enabled: Boolean) = Unit
}
