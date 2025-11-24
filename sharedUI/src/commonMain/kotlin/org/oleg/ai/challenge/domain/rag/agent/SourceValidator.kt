package org.oleg.ai.challenge.domain.rag.agent

import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult

/**
 * Validates source freshness and quality for retrieved chunks.
 */
interface SourceValidator {
    fun validate(results: List<RetrievalResult>): List<ValidatedChunk>
}

data class ValidatedChunk(
    val result: RetrievalResult,
    val qualityScore: Double,
    val reasons: List<String> = emptyList()
)
