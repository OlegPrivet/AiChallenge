package org.oleg.ai.challenge.data.mapper

import org.oleg.ai.challenge.data.database.entity.AgentEntity
import org.oleg.ai.challenge.data.model.Agent

/**
 * Mapper for converting between Agent domain model and AgentEntity.
 */
object AgentMapper {

    /**
     * Convert AgentEntity to Agent domain model.
     */
    fun AgentEntity.toDomain(): Agent {
        return Agent(
            id = agentId,
            name = agentName,
            systemPrompt = systemPrompt,
            assistantPrompt = assistantPrompt,
            model = model,
            temperature = temperature
        )
    }

    /**
     * Convert Agent domain model to AgentEntity.
     * Requires chatId parameter since Agent doesn't store it.
     */
    fun Agent.toEntity(
        chatId: Long,
        isMain: Boolean = false,
        parentAgentId: String? = null
    ): AgentEntity {
        return AgentEntity(
            agentId = id,
            chatId = chatId,
            agentName = name,
            systemPrompt = systemPrompt,
            assistantPrompt = assistantPrompt,
            model = model,
            temperature = temperature,
            isMain = isMain,
            parentAgentId = parentAgentId
        )
    }

    /**
     * Convert list of AgentEntity to list of Agent.
     */
    fun List<AgentEntity>.toDomain(): List<Agent> {
        return map { it.toDomain() }
    }
}
