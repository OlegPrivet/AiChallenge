package org.oleg.ai.challenge.data.rag.agent

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.data.network.ApiResult
import org.oleg.ai.challenge.data.network.model.ChatRequest
import org.oleg.ai.challenge.data.network.model.MessageRole
import org.oleg.ai.challenge.data.network.service.ChatApiService
import org.oleg.ai.challenge.domain.rag.agent.QueryDecomposer
import org.oleg.ai.challenge.data.network.model.ChatMessage as ApiChatMessage

/**
 * LLM-based query decomposer that intelligently breaks down complex queries
 * into logical sub-queries using AI.
 *
 * Falls back to SimpleQueryDecomposer on errors or for simple queries.
 */
class LlmBasedQueryDecomposer(
    private val chatApiService: ChatApiService,
    private val fallbackDecomposer: QueryDecomposer,
    private val model: String = "anthropic/claude-3.5-haiku",
    private val logger: Logger = Logger.withTag("LlmBasedQueryDecomposer")
) : QueryDecomposer {

    override suspend fun decompose(query: String): List<String> {
        val normalized = query.trim()

        // For very short queries, use fallback immediately
        if (normalized.length < 50) {
            logger.d { "Query too short for LLM decomposition, using fallback" }
            return fallbackDecomposer.decompose(query)
        }

        return try {
            val systemPrompt = """
                You are a query decomposition expert. Your task is to break down complex queries into simpler, independent sub-queries that can be answered separately.

                Rules:
                1. Only decompose if the query is truly complex (multiple topics or questions)
                2. Each sub-query should be complete and answerable independently
                3. Return sub-queries one per line
                4. If the query is simple, return it as-is
                5. Maximum 5 sub-queries

                Example:
                Input: "What is the capital of France and what is its population?"
                Output:
                What is the capital of France?
                What is the population of Paris?

                Now decompose the following query:
            """.trimIndent()

            val request = ChatRequest(
                model = model,
                messages = listOf(
                    ApiChatMessage(role = MessageRole.SYSTEM, content = systemPrompt),
                    ApiChatMessage(role = MessageRole.USER, content = normalized)
                ),
                temperature = 0.3f // Low temperature for consistent, deterministic decomposition
            )

            when (val result = chatApiService.sendChatCompletion(request)) {
                is ApiResult.Success -> {
                    val responseText = result.data.choices.firstOrNull()?.message?.content
                        ?: throw IllegalStateException("No response content")

                    val subQueries = responseText.lines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() && !it.startsWith("#") }
                        .take(5) // Limit to 5 sub-queries

                    if (subQueries.isEmpty()) {
                        logger.w { "LLM returned empty decomposition, using fallback" }
                        fallbackDecomposer.decompose(query)
                    } else {
                        logger.d { "Successfully decomposed query into ${subQueries.size} sub-queries" }
                        subQueries
                    }
                }
                is ApiResult.Error -> {
                    logger.e { "Error calling LLM for query decomposition: ${result.error}" }
                    fallbackDecomposer.decompose(query)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception during LLM query decomposition" }
            fallbackDecomposer.decompose(query)
        }
    }
}
