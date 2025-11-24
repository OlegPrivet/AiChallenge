package org.oleg.ai.challenge.domain.rag.model

import kotlinx.serialization.Serializable

/**
 * Retrieval result returned by search pipelines.
 */
@Serializable
data class RetrievalResult(
    val document: Document,
    val chunk: DocumentChunk,
    val score: Double,
    val rerankedScore: Double? = null
)
