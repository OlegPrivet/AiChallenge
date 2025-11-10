package org.oleg.ai.challenge.data.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents an AI agent configuration for chat interactions.
 * Agents can have their own system prompts, assistant prompts,
 * AI model selection, and temperature settings.
 */
data class Agent @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class) constructor(
    val id: String = Uuid.random().toHexString(),
    val name: String,
    val systemPrompt: String? = null,
    val assistantPrompt: String? = null,
    val model: String,
    val temperature: Float = 1.0f,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    companion object {
        /**
         * Creates a main agent with default settings
         */
        fun createMain(
            systemPrompt: String? = null,
            assistantPrompt: String? = null,
            model: String
        ): Agent = Agent(
            id = "main",
            name = "Main Agent",
            systemPrompt = systemPrompt,
            assistantPrompt = assistantPrompt,
            model = model
        )
    }
}
