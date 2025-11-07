package org.oleg.ai.challenge.component.agentcreation

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.Agent

interface AgentCreationComponent {
    // Main agent configuration
    val mainSystemPrompt: Value<String>
    val mainAssistantPrompt: Value<String>
    val selectedModel: Value<String>

    // Sub-agents list
    val subAgents: Value<List<Agent>>

    // Dialog state
    val showSubAgentDialog: Value<Boolean>
    val editingSubAgentIndex: Value<Int>  // -1 means no editing, otherwise index in subAgents list

    // Main agent prompt changes
    fun onMainSystemPromptChanged(text: String)
    fun onMainAssistantPromptChanged(text: String)
    fun onModelSelected(model: String)

    // Sub-agent management
    fun onAddSubAgent()
    fun onEditSubAgent(agent: Agent)
    fun onDeleteSubAgent(agentId: String)
    fun onSubAgentDialogDismiss()
    fun onSubAgentDialogConfirm(name: String, systemPrompt: String, assistantPrompt: String, model: String)

    // Navigation
    fun onStartChat()
    fun onNavigateBack()
}
