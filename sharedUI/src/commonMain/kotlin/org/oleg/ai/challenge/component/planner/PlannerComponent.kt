package org.oleg.ai.challenge.component.planner

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.PlannerFrequency
import org.oleg.ai.challenge.data.network.service.McpClientService

/**
 * Component interface for Planner Mode.
 *
 * Planner Mode allows users to:
 * - Send a single message that will be periodically sent to AI
 * - Select an MCP tool to use for all requests in the session
 * - Configure the frequency of AI requests
 * - View only AI responses in the UI
 */
interface PlannerComponent {

    // State
    val messages: Value<List<ChatMessage>>
    val inputText: Value<String>
    val isLoading: Value<Boolean>
    val showInputField: Value<Boolean>

    // MCP tools
    val availableTools: Value<List<McpClientService.ToolInfo>>
    val selectedToolName: Value<String> // Empty string means no tool selected

    // Frequency settings
    val selectedFrequency: Value<PlannerFrequency>
    val isPeriodicRunning: Value<Boolean>

    // Events
    fun onTextChanged(text: String)
    fun onSendMessage()
    fun onSelectTool(tool: McpClientService.ToolInfo?)
    fun onFrequencyChanged(frequency: PlannerFrequency)
    fun onStartPeriodic()
    fun onStopPeriodic()
    fun onBack()
}
