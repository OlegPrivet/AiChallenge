package org.oleg.ai.challenge.domain.rag.lexical

import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk

data class LexicalMatch(
    val chunkId: String,
    val documentId: String,
    val chunkIndex: Int?,
    val score: Double,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Contract for lexical/BM25 search backends.
 */
interface LexicalSearchService {
    suspend fun index(document: Document, chunks: List<DocumentChunk>)
    suspend fun delete(documentId: String)
    suspend fun search(
        query: String,
        topK: Int = 10,
        filters: Map<String, String> = emptyMap()
    ): List<LexicalMatch>
}
