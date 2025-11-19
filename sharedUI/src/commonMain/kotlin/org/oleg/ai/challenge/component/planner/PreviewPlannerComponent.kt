package org.oleg.ai.challenge.component.planner

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.MessageRole
import org.oleg.ai.challenge.data.model.PlannerFrequency
import org.oleg.ai.challenge.data.network.service.McpClientService

/**
 * Preview implementation of PlannerComponent for testing and Compose previews.
 */
class PreviewPlannerComponent : PlannerComponent {

    override val messages: Value<List<ChatMessage>> = MutableValue(
        listOf(
            ChatMessage(
                id = "1",
                text = "This is a sample AI response for the planner mode preview.",
                isFromUser = false,
                role = MessageRole.ASSISTANT,
                isVisibleInUI = true
            ),
            ChatMessage(
                id = "2",
                text = "Another response demonstrating periodic execution results.",
                isFromUser = false,
                role = MessageRole.ASSISTANT,
                isVisibleInUI = true
            )
        )
    )

    override val inputText: Value<String> = MutableValue("")
    override val isLoading: Value<Boolean> = MutableValue(false)
    override val showInputField: Value<Boolean> = MutableValue(true)

    override val availableTools: Value<List<McpClientService.ToolInfo>> = MutableValue(
        listOf(
            McpClientService.ToolInfo(
                name = "get_weather",
                title = "Get Weather",
                description = "Get current weather for a location",
                inputSchema = """{"location": "string"}"""
            ),
            McpClientService.ToolInfo(
                name = "search_web",
                title = "Search Web",
                description = "Search the web for information",
                inputSchema = """{"query": "string"}"""
            )
        )
    )

    override val selectedToolName: Value<String> = MutableValue("")
    override val selectedFrequency: Value<PlannerFrequency> = MutableValue(PlannerFrequency.DEFAULT)
    override val isPeriodicRunning: Value<Boolean> = MutableValue(false)

    override fun onTextChanged(text: String) {}
    override fun onSendMessage() {}
    override fun onSelectTool(tool: McpClientService.ToolInfo?) {}
    override fun onFrequencyChanged(frequency: PlannerFrequency) {}
    override fun onStartPeriodic() {}
    override fun onStopPeriodic() {}
    override fun onBack() {}
}
