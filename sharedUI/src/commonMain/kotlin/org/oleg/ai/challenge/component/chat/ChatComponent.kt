package org.oleg.ai.challenge.component.chat

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.McpUiState
import org.oleg.ai.challenge.domain.rag.orchestrator.Citation
import org.oleg.ai.challenge.data.audio.RecordingState

interface ChatComponent {
    val messages: Value<List<ChatMessage>>
    val inputText: Value<String>
    val isLoading: Value<Boolean>

    val availableAgents: Value<List<Agent>>  // List of agent IDs (including "main")
    val selectedAgent: Value<Agent>  // Currently selected agent ID
    val currentAgentModel: Value<String>  // Model of the currently selected agent
    val currentTemperature: Value<Float>  // Temperature of the currently selected agent
    val mcpUiState: Value<McpUiState>

    // RAG-related state
    val isRagEnabled: Value<Boolean>  // Whether RAG mode is enabled for this chat
    val isDeveloperModeEnabled: Value<Boolean>  // Whether developer mode is enabled
    // For showing citation modal (nullable OK here, handled separately)
    val selectedCitationSource: Value<CitationState>

    // Audio recording state
    val recordingState: Value<RecordingState>

    fun onTextChanged(text: String)
    fun onSendMessage()
    fun onSummarizeConversation()
    fun onAgentSelected(agentId: String?)
    fun onModelChanged(model: String)  // Change model for current agent
    fun onTemperatureChanged(temperature: Float)  // Change temperature for current agent

    // RAG-related actions
    fun onToggleRagMode(enabled: Boolean)  // Toggle RAG mode for this chat
    fun onShowSource(citation: Citation, chunkContent: String)  // Show citation source in modal
    fun onHideSource()  // Hide citation source modal
    fun onToggleDeveloperMode(enabled: Boolean)  // Toggle developer mode

    // Audio recording actions
    fun onStartRecording()  // Start recording audio
    fun onStopRecording()  // Stop recording and transcribe
    fun onCancelRecording()  // Cancel recording without transcribing
}

/**
 * Detailed citation source information for display in modal.
 */
data class CitationSourceDetail(
    val citation: Citation,
    val chunkContent: String
)

sealed interface CitationState {
    object None : CitationState
    data class Detail(val data: CitationSourceDetail) : CitationState
}
