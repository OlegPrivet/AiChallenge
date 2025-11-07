package org.oleg.ai.challenge.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.oleg.ai.challenge.data.model.Agent

/**
 * In-memory manager for AI agents.
 * Maintains the list of agents and the main agent configuration for the current session.
 */
class AgentManager {
    private val _mainAgent = MutableStateFlow<Agent?>(null)
    val mainAgent: StateFlow<Agent?> = _mainAgent.asStateFlow()

    private val _subAgents = MutableStateFlow<List<Agent>>(emptyList())
    val subAgents: StateFlow<List<Agent>> = _subAgents.asStateFlow()

    /**
     * Sets the main agent configuration
     */
    fun setMainAgent(agent: Agent) {
        _mainAgent.value = agent
    }

    /**
     * Gets the main agent or null if not set
     */
    fun getMainAgent(): Agent? = _mainAgent.value

    /**
     * Adds a sub-agent to the list
     */
    fun addSubAgent(agent: Agent) {
        _subAgents.value = _subAgents.value + agent
    }

    /**
     * Updates an existing sub-agent
     */
    fun updateSubAgent(agent: Agent) {
        _subAgents.value = _subAgents.value.map {
            if (it.id == agent.id) agent else it
        }
    }

    /**
     * Removes a sub-agent by ID
     */
    fun removeSubAgent(agentId: String) {
        _subAgents.value = _subAgents.value.filter { it.id != agentId }
    }

    /**
     * Gets a specific agent by ID (checks both main and sub-agents)
     */
    fun getAgent(agentId: String): Agent? {
        return when {
            _mainAgent.value?.id == agentId -> _mainAgent.value
            else -> _subAgents.value.find { it.id == agentId }
        }
    }

    /**
     * Gets all agents (main + sub-agents)
     */
    fun getAllAgents(): List<Agent> {
        val main = _mainAgent.value
        return if (main != null) listOf(main) + _subAgents.value else _subAgents.value
    }

    /**
     * Clears all agents (for new chat session)
     */
    fun clear() {
        _mainAgent.value = null
        _subAgents.value = emptyList()
    }
}
