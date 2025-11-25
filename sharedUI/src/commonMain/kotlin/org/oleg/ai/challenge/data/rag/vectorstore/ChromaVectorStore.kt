package org.oleg.ai.challenge.data.rag.vectorstore

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.oleg.ai.challenge.data.network.service.ChromaClient
import org.oleg.ai.challenge.data.network.service.ChromaRecord
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.vector.VectorMatch

class ChromaVectorStore(
    private val client: ChromaClient,
    private val defaultCollection: String = "rag",
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: Logger = Logger.withTag("ChromaVectorStore")
) {

    private val collectionCache = mutableMapOf<String, String>()

    suspend fun upsertDocumentChunks(
        document: Document,
        chunks: List<DocumentChunk>,
        collectionName: String = defaultCollection
    ) = withContext(dispatcher) {
        val collectionId = ensureCollectionId(collectionName, document)

        val records = chunks.mapNotNull { chunk ->
            val embedding = chunk.embedding ?: return@mapNotNull null
            ChromaRecord(
                id = chunk.id,
                embedding = embedding.values,
                document = chunk.content,
                metadata = buildMap {
                    put("documentId", chunk.documentId)
                    put("chunkIndex", chunk.chunkIndex.toString())
                    put("chunkingVersion", chunk.chunkingStrategyVersion)
                    put("embeddingVersion", chunk.embeddingModelVersion)
                    put("sourceType", document.sourceType.name)
                    document.uri?.let { put("uri", it) }
                    document.metadata.forEach { (k, v) -> put(k, v) }
                }
            )
        }

        client.upsert(
            collectionId = collectionId,
            ids = records.map { it.id },
            embeddings = records.map { it.embedding },
            documents = records.map { it.document },
            metadatas = records.map { it.metadata }
        )
        logger.d { "Upserted ${records.size} chunks into Chroma collection=$collectionName" }
    }

    suspend fun query(
        embedding: List<Float>,
        topK: Int = 8,
        filters: Map<String, String> = emptyMap(),
        collectionName: String = defaultCollection
    ): List<VectorMatch> = withContext(dispatcher) {
        val collectionId = ensureCollectionId(collectionName, null)
        val response = client.query(
            collectionId = collectionId,
            queryEmbeddings = listOf(embedding),
            nResults = topK,
            where = filters.ifEmpty { null }
        )

        val ids = response.ids.firstOrNull().orEmpty()
        val metadatas = response.metadatas?.firstOrNull().orEmpty()
        val documents = response.documents?.firstOrNull().orEmpty()
        val distances = response.distances?.firstOrNull().orEmpty()

        ids.mapIndexed { index, id ->
            val metadata = metadatas.getOrNull(index) ?: emptyMap()
            VectorMatch(
                id = id,
                documentId = metadata["documentId"],
                chunkIndex = metadata["chunkIndex"]?.toIntOrNull(),
                score = 1.0 - (distances.getOrNull(index) ?: 0.0),
                metadata = metadata,
                text = documents.getOrNull(index)
            )
        }
    }

    suspend fun deleteDocument(
        documentId: String,
        collectionName: String = defaultCollection
    ) = withContext(dispatcher) {
        val collectionId = ensureCollectionId(collectionName, null)
        client.delete(
            collectionId = collectionId,
            where = mapOf("documentId" to documentId)
        )
        logger.d { "Deleted vectors for document=$documentId in collection=$collectionName" }
    }

    private suspend fun ensureCollectionId(
        name: String,
        document: Document?
    ): String {
        collectionCache[name]?.let { return it }
        val collection = client.getOrCreateCollection(
            name = name,
            metadata = document?.metadata
        )
        collectionCache[name] = collection.id
        return collection.id
    }
}
