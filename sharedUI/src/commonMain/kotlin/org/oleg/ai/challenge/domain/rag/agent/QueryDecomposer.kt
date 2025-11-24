package org.oleg.ai.challenge.domain.rag.agent

/**
 * Decomposes complex queries into sub-queries for sequential retrieval.
 */
interface QueryDecomposer {
    suspend fun decompose(query: String): List<String>
}
