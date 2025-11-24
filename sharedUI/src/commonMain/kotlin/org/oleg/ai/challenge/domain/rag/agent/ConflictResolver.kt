package org.oleg.ai.challenge.domain.rag.agent

import org.oleg.ai.challenge.domain.rag.model.RetrievalResult

/**
 * Detects conflicting facts across chunks and ranks by reliability.
 */
interface ConflictResolver {
    suspend fun resolve(results: List<ValidatedChunk>): ConflictResolution
}

data class ConflictResolution(
    val resolved: List<ValidatedChunk>,
    val conflicts: List<Conflict>
)

data class Conflict(
    val chunkIds: List<String>,
    val reason: String,
    val semanticScore: Double? = null,
    val detectionMethod: String = "heuristic"
)
