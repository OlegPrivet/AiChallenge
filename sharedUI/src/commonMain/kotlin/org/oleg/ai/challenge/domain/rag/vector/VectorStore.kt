package org.oleg.ai.challenge.domain.rag.vector

import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk

data class VectorMatch(
    val id: String,
    val documentId: String?,
    val chunkIndex: Int?,
    val score: Double,
    val metadata: Map<String, String>,
    val text: String?
)

/**
 * Abstraction for vector storage operations.
 */
interface VectorStore {
    suspend fun upsert(document: Document, chunks: List<DocumentChunk>)

    suspend fun delete(documentId: String)

    suspend fun query(
        embedding: List<Float>,
        topK: Int = 8,
        filters: Map<String, String> = emptyMap()
    ): List<VectorMatch>
}
