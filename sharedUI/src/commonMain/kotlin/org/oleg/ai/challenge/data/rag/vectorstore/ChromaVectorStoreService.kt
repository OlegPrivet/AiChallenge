package org.oleg.ai.challenge.data.rag.vectorstore

import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.vector.VectorMatch
import org.oleg.ai.challenge.domain.rag.vector.VectorStore

class ChromaVectorStoreService(
    private val store: ChromaVectorStore
) : VectorStore {

    override suspend fun upsert(document: Document, chunks: List<DocumentChunk>) {
        store.upsertDocumentChunks(document, chunks)
    }

    override suspend fun delete(documentId: String) {
        store.deleteDocument(documentId)
    }

    override suspend fun query(
        embedding: List<Float>,
        topK: Int,
        filters: Map<String, String>
    ): List<VectorMatch> {
        return store.query(
            embedding = embedding,
            topK = topK,
            filters = filters
        )
    }
}
