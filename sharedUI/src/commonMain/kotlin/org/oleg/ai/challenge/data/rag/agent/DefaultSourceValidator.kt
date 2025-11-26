package org.oleg.ai.challenge.data.rag.agent

import org.oleg.ai.challenge.domain.rag.agent.SourceValidator
import org.oleg.ai.challenge.domain.rag.agent.ValidatedChunk
import org.oleg.ai.challenge.domain.rag.model.KnowledgeSourceType
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * Basic source validator that scores freshness and source type reliability.
 */
class DefaultSourceValidator(
    private val freshnessHalfLifeMs: Long = 1000L * 60L * 60L * 24L * 30L, // ~30 days
) : SourceValidator {

    @OptIn(kotlin.time.ExperimentalTime::class)
    override fun validate(results: List<RetrievalResult>): List<ValidatedChunk> {
        val now = Clock.System.now().toEpochMilliseconds()
        return results.map { result ->
            val freshnessAge = (now - result.document.updatedAt).coerceAtLeast(0)
            val freshnessScore = 1.0 / (1.0 + freshnessAge.toDouble() / freshnessHalfLifeMs)

            val sourceScore = when (result.document.sourceType) {
                KnowledgeSourceType.INTERNAL -> 1.0
                KnowledgeSourceType.USER -> 0.9
                KnowledgeSourceType.REMOTE -> 0.8
                KnowledgeSourceType.CACHED -> 0.6
            }

            val combined = (freshnessScore * 0.6) + (sourceScore * 0.4)

            ValidatedChunk(
                result = result,
                qualityScore = combined,
                reasons = listOf(
                    "freshness=${(freshnessScore * 100).roundToInt() / 100.0}",
                    "source=${result.document.sourceType} score=${(sourceScore * 100).roundToInt() / 100.0}"
                )
            )
        }
    }
}
