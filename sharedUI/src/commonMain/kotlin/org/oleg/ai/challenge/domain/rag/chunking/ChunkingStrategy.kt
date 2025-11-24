package org.oleg.ai.challenge.domain.rag.chunking

import org.oleg.ai.challenge.domain.rag.model.DocumentChunk

/**
 * Base contract for converting raw text into versioned chunks.
 */
interface ChunkingStrategy {
    val name: String
    val version: String

    suspend fun chunk(documentId: String, text: String, embeddingModelVersion: String): List<DocumentChunk>
}
