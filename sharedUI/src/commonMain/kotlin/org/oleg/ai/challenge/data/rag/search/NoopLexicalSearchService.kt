package org.oleg.ai.challenge.data.rag.search

import org.oleg.ai.challenge.domain.rag.lexical.LexicalMatch
import org.oleg.ai.challenge.domain.rag.lexical.LexicalSearchService
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk

/**
 * Placeholder implementation for platforms without Lucene.
 */
class NoopLexicalSearchService : LexicalSearchService {
    override suspend fun index(document: Document, chunks: List<DocumentChunk>) = Unit

    override suspend fun delete(documentId: String) = Unit

    override suspend fun search(
        query: String,
        topK: Int,
        filters: Map<String, String>
    ): List<LexicalMatch> = emptyList()
}
