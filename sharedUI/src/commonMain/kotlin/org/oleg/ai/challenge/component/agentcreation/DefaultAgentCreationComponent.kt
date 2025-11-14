package org.oleg.ai.challenge.component.agentcreation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.AgentManager
import org.oleg.ai.challenge.data.model.Agent

class DefaultAgentCreationComponent(
    componentContext: ComponentContext,
    private val agentManager: AgentManager,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToChat: () -> Unit = {},
    private val onAgentsCreated: ((mainAgent: Agent, subAgents: List<Agent>) -> Unit)? = null
) : AgentCreationComponent, ComponentContext by componentContext {

    private val _mainSystemPrompt = MutableValue("")
    override val mainSystemPrompt: Value<String> = _mainSystemPrompt

    private val _mainAssistantPrompt = MutableValue("")
    override val mainAssistantPrompt: Value<String> = _mainAssistantPrompt

    private val _selectedModel = MutableValue(BuildConfig.DEFAULT_MODEL)
    override val selectedModel: Value<String> = _selectedModel

    private val _subAgents = MutableValue<List<Agent>>(emptyList())
    override val subAgents: Value<List<Agent>> = _subAgents

    private val _showSubAgentDialog = MutableValue(false)
    override val showSubAgentDialog: Value<Boolean> = _showSubAgentDialog

    private val _editingSubAgentIndex = MutableValue(-1)
    override val editingSubAgentIndex: Value<Int> = _editingSubAgentIndex

    override fun onMainSystemPromptChanged(text: String) {
        _mainSystemPrompt.value = text
    }

    override fun onMainAssistantPromptChanged(text: String) {
        _mainAssistantPrompt.value = text
    }

    override fun onModelSelected(model: String) {
        _selectedModel.value = model
    }

    override fun onAddSubAgent() {
        _editingSubAgentIndex.value = -1
        _showSubAgentDialog.value = true
    }

    override fun onEditSubAgent(agent: Agent) {
        _editingSubAgentIndex.value = _subAgents.value.indexOfFirst { it.id == agent.id }
        _showSubAgentDialog.value = true
    }

    override fun onDeleteSubAgent(agentId: String) {
        _subAgents.value = _subAgents.value.filter { it.id != agentId }
    }

    override fun onSubAgentDialogDismiss() {
        _showSubAgentDialog.value = false
        _editingSubAgentIndex.value = -1
    }

    override fun onSubAgentDialogConfirm(name: String, systemPrompt: String, assistantPrompt: String, model: String) {
        val editingIndex = _editingSubAgentIndex.value

        if (editingIndex >= 0 && editingIndex < _subAgents.value.size) {
            // Update existing agent
            val existing = _subAgents.value[editingIndex]
            val updated = existing.copy(
                name = name,
                systemPrompt = systemPrompt.ifBlank { null },
                assistantPrompt = assistantPrompt.ifBlank { null },
                model = model
            )
            _subAgents.value = _subAgents.value.toMutableList().apply {
                set(editingIndex, updated)
            }
        } else {
            // Create new agent
            val newAgent = Agent(
                name = name,
                systemPrompt = systemPrompt.ifBlank { null },
                assistantPrompt = assistantPrompt.ifBlank { null },
                model = model
            )
            _subAgents.value = _subAgents.value + newAgent
        }

        _showSubAgentDialog.value = false
        _editingSubAgentIndex.value = -1
    }

    override fun onStartChat() {
        // Create main agent
        val mainAgent = Agent.createMain(
            systemPrompt = _mainSystemPrompt.value.ifBlank { null },
            assistantPrompt = _mainAssistantPrompt.value.ifBlank { null },
            model = _selectedModel.value
        )

        // If onAgentsCreated callback is provided, use it (for new split-screen flow)
        if (onAgentsCreated != null) {
            onAgentsCreated.invoke(mainAgent, _subAgents.value)
        } else {
            // Otherwise, use legacy flow (save to AgentManager and navigate)
            agentManager.setMainAgent(mainAgent)

            // Save sub-agents
            _subAgents.value.forEach { agent ->
                agentManager.addSubAgent(agent)
            }

            // Navigate to chat
            onNavigateToChat()
        }
    }

    override fun onNavigateBack() {
        onNavigateBack.invoke()
    }
}
