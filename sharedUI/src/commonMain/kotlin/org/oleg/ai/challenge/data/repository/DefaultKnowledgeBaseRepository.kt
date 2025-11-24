package org.oleg.ai.challenge.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.oleg.ai.challenge.data.database.dao.DocumentDao
import org.oleg.ai.challenge.data.mapper.rag.toDomain
import org.oleg.ai.challenge.data.mapper.rag.toEntity
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.model.DocumentWithChunks
import org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository

/**
 * Default implementation of the knowledge base repository backed by Room.
 */
class DefaultKnowledgeBaseRepository(
    private val documentDao: DocumentDao
) : KnowledgeBaseRepository {

    override fun observeDocuments(): Flow<List<Document>> {
        return documentDao.observeDocuments().map { documents ->
            documents.map { it.toDomain() }
        }
    }

    override suspend fun getDocument(documentId: String): Document? {
        return documentDao.getDocument(documentId)?.toDomain()
    }

    override suspend fun getDocumentWithChunks(documentId: String): DocumentWithChunks? {
        return documentDao.getDocumentWithChunks(documentId)?.toDomain()
    }

    override suspend fun getChunks(documentId: String): List<DocumentChunk> {
        return documentDao.getChunks(documentId).map { it.toDomain() }
    }

    override suspend fun upsertDocument(document: Document, chunks: List<DocumentChunk>) {
        documentDao.replaceDocumentWithChunks(
            document = document.toEntity(),
            chunks = chunks.map { it.toEntity() }
        )
    }

    override suspend fun deleteDocument(documentId: String) {
        documentDao.deleteDocument(documentId)
    }

    override suspend fun findDocumentsWithOutdatedEmbeddings(expectedVersion: String): List<String> {
        return documentDao.findDocumentsWithOutdatedEmbeddings(expectedVersion)
    }

    override suspend fun findDocumentsWithOutdatedChunking(expectedVersion: String): List<String> {
        return documentDao.findDocumentsWithOutdatedChunking(expectedVersion)
    }
}
