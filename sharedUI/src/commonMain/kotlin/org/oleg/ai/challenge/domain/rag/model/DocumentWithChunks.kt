package org.oleg.ai.challenge.domain.rag.model

import kotlinx.serialization.Serializable

/**
 * Aggregates a document with its persisted chunks for domain use.
 */
@Serializable
data class DocumentWithChunks(
    val document: Document,
    val chunks: List<DocumentChunk>
)
