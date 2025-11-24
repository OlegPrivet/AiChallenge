package org.oleg.ai.challenge.domain.rag.repository

import kotlinx.coroutines.flow.Flow
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.model.DocumentWithChunks

/**
 * Repository interface for managing persisted RAG documents and chunks.
 */
interface KnowledgeBaseRepository {
    fun observeDocuments(): Flow<List<Document>>

    suspend fun getDocument(documentId: String): Document?

    suspend fun getDocumentWithChunks(documentId: String): DocumentWithChunks?

    suspend fun getChunks(documentId: String): List<DocumentChunk>

    suspend fun upsertDocument(document: Document, chunks: List<DocumentChunk>)

    suspend fun deleteDocument(documentId: String)

    suspend fun findDocumentsWithOutdatedEmbeddings(expectedVersion: String): List<String>

    suspend fun findDocumentsWithOutdatedChunking(expectedVersion: String): List<String>
}
