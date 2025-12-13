package org.oleg.ai.challenge.data.rag.agent

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.data.network.ApiResult
import org.oleg.ai.challenge.data.network.model.ChatRequest
import org.oleg.ai.challenge.data.network.model.MessageRole
import org.oleg.ai.challenge.data.network.service.ChatApiService
import org.oleg.ai.challenge.domain.rag.agent.Conflict
import org.oleg.ai.challenge.domain.rag.agent.ConflictResolution
import org.oleg.ai.challenge.domain.rag.agent.ConflictResolver
import org.oleg.ai.challenge.domain.rag.agent.ValidatedChunk
import org.oleg.ai.challenge.data.network.model.ChatMessage as ApiChatMessage

/**
 * LLM-enhanced conflict detector that performs semantic contradiction analysis
 * on top of heuristic conflict detection.
 *
 * This resolver:
 * 1. Uses the base resolver for initial heuristic detection
 * 2. Analyzes detected conflicts semantically using LLM
 * 3. Adds semantic scores and detection methods to conflicts
 */
class LlmBasedConflictDetector(
    private val baseResolver: ConflictResolver,
    private val chatApiService: ChatApiService,
    private val model: String = "anthropic/claude-3.5-haiku",
    private val enableSemanticAnalysis: Boolean = true,
    private val logger: Logger = Logger.withTag("LlmBasedConflictDetector")
) : ConflictResolver {

    override suspend fun resolve(results: List<ValidatedChunk>): ConflictResolution {
        // First get heuristic conflicts
        val baseResolution = baseResolver.resolve(results)

        // If semantic analysis is disabled or no conflicts found, return base resolution
        if (!enableSemanticAnalysis || baseResolution.conflicts.isEmpty()) {
            return baseResolution
        }

        // Enhance conflicts with semantic analysis
        val enhancedConflicts = baseResolution.conflicts.mapNotNull { conflict ->
            analyzeConflictSemantically(conflict, results)
        }

        logger.d { "Enhanced ${baseResolution.conflicts.size} conflicts with semantic analysis" }
        return baseResolution.copy(conflicts = enhancedConflicts)
    }

    /**
     * Analyzes a conflict semantically using LLM to determine if there's an actual contradiction.
     */
    private suspend fun analyzeConflictSemantically(
        conflict: Conflict,
        allResults: List<ValidatedChunk>
    ): Conflict? {
        try {
            // Find the chunks involved in this conflict
            val conflictChunks = allResults.filter {
                it.result.chunk.id in conflict.chunkIds
            }.take(5) // Limit to 5 chunks to avoid token overflow

            if (conflictChunks.size < 2) {
                // Not enough chunks to analyze
                return conflict
            }

            // Build comparison text
            val chunkTexts = conflictChunks.mapIndexed { idx, chunk ->
                "Chunk ${idx + 1} (from '${chunk.result.document.title}'):\n${chunk.result.chunk.content}"
            }.joinToString("\n\n---\n\n")

            val systemPrompt = """
                You are a fact-checking expert. Analyze the following text chunks to determine if they contain contradicting information.

                Respond with ONLY a JSON object in this format:
                {
                  "has_contradiction": true/false,
                  "confidence": 0.0-1.0,
                  "explanation": "brief explanation"
                }

                Consider a contradiction only if the chunks make incompatible claims about the same topic.
            """.trimIndent()

            val request = ChatRequest(
                model = model,
                messages = listOf(
                    ApiChatMessage(role = MessageRole.SYSTEM, content = systemPrompt),
                    ApiChatMessage(role = MessageRole.USER, content = chunkTexts)
                ),
                temperature = 0.1f // Very low temperature for consistent analysis
            )

            when (val result = chatApiService.sendChatCompletion(request)) {
                is ApiResult.Success -> {
                    val responseText = result.data.message.content
                        ?: return conflict

                    // Try to parse the JSON response
                    val hasContradiction = responseText.contains("\"has_contradiction\": true", ignoreCase = true)
                    val confidence = extractConfidence(responseText)

                    if (hasContradiction && confidence > 0.5) {
                        // Confirmed contradiction
                        return conflict.copy(
                            semanticScore = confidence,
                            detectionMethod = "llm-semantic",
                            reason = "${conflict.reason} (LLM confirmed contradiction with ${(confidence * 100).toInt()}% confidence)"
                        )
                    } else {
                        // False positive - not a real contradiction
                        logger.d { "Conflict dismissed by semantic analysis: $conflict" }
                        return null // Filter out this conflict
                    }
                }
                is ApiResult.Error -> {
                    logger.w { "Failed to analyze conflict semantically: ${result.error}" }
                    return conflict // Return original conflict
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception during semantic conflict analysis" }
            return conflict // Return original conflict on error
        }
    }

    /**
     * Extracts confidence score from LLM response.
     */
    private fun extractConfidence(responseText: String): Double {
        return try {
            val confidenceRegex = """"confidence":\s*([\d.]+)""".toRegex()
            val match = confidenceRegex.find(responseText)
            match?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.5
        } catch (e: Exception) {
            0.5 // Default confidence
        }
    }
}
