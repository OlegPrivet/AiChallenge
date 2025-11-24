package org.oleg.ai.challenge.domain.rag

import kotlinx.serialization.Serializable
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult

@Serializable
data class QueryEmbedding(
    val vector: List<Float>,
    val model: String,
    val version: String
)

/**
 * Unified search abstraction for vector, lexical, and hybrid pipelines.
 */
interface SearchPipeline {
    suspend fun search(
        query: String,
        embedding: QueryEmbedding? = null,
        topK: Int = 10,
        filters: Map<String, String> = emptyMap()
    ): List<RetrievalResult>
}
