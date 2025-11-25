package org.oleg.ai.challenge.data.rag.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.oleg.ai.challenge.domain.rag.chunking.ChunkRequest
import org.oleg.ai.challenge.domain.rag.chunking.ChunkingService
import org.oleg.ai.challenge.domain.rag.chunking.ChunkingStrategy
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingRequest
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingService
import org.oleg.ai.challenge.domain.rag.lexical.LexicalSearchService
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository
import org.oleg.ai.challenge.domain.rag.vector.VectorStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Handles document ingestion pipeline: chunk -> embed -> persist -> index.
 */
class DocumentIngestionRepository(
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val chunkingService: ChunkingService,
    private val embeddingService: EmbeddingService,
    private val vectorStore: VectorStore,
    private val lexicalSearchService: LexicalSearchService,
    private val defaultEmbeddingModel: String,
    private val ingestionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val logger: Logger = Logger.withTag("DocumentIngestionRepository")
) {

    fun enqueueIngestion(
        document: Document,
        content: String,
        strategy: ChunkingStrategy
    ) = ingestionScope.launch {
        ingestDocument(document, content, strategy)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun ingestDocument(
        document: Document,
        content: String,
        strategy: ChunkingStrategy
    ) {
        logger.d { "Ingesting document=${document.id} with strategy=${strategy.name}:${strategy.version}" }
        val embeddingVersion = document.embeddingModelVersion.ifBlank { defaultEmbeddingModel }
        val chunks = chunkingService.chunkDocument(
            ChunkRequest(
                document = document,
                content = content,
                embeddingModelVersion = embeddingVersion
            ),
            strategy = strategy
        )

        val embeddings = embeddingService.embed(
            EmbeddingRequest(
                texts = chunks.map { it.content },
                model = defaultEmbeddingModel,
                embeddingModelVersion = embeddingVersion,
                normalize = true,
                useFp16Quantization = false,
                batchSize = 8
            )
        )

        val enrichedChunks = attachEmbeddings(chunks, embeddings, embeddingVersion)
        val updatedDocument = document.copy(
            chunkingStrategyVersion = strategy.version,
            embeddingModelVersion = embeddingVersion,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )

        knowledgeBaseRepository.upsertDocument(updatedDocument, enrichedChunks)
        vectorStore.upsert(updatedDocument, enrichedChunks)
        lexicalSearchService.index(updatedDocument, enrichedChunks)
        logger.d { "Document=${document.id} ingested with ${enrichedChunks.size} chunks" }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun reembedOutdatedDocuments(expectedVersion: String) {
        val outdated = knowledgeBaseRepository.findDocumentsWithOutdatedEmbeddings(expectedVersion)
        for (documentId in outdated) {
            val docWithChunks = knowledgeBaseRepository.getDocumentWithChunks(documentId) ?: continue
            logger.i { "Re-embedding document=$documentId to version=$expectedVersion" }

            val embeddings = embeddingService.embed(
                EmbeddingRequest(
                    texts = docWithChunks.chunks.map { it.content },
                    model = defaultEmbeddingModel,
                    embeddingModelVersion = expectedVersion,
                    normalize = true
                )
            )

            val updatedChunks = attachEmbeddings(
                docWithChunks.chunks,
                embeddings,
                expectedVersion
            )

            val updatedDocument = docWithChunks.document.copy(
                embeddingModelVersion = expectedVersion,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )

            knowledgeBaseRepository.upsertDocument(updatedDocument, updatedChunks)
            vectorStore.upsert(updatedDocument, updatedChunks)
            lexicalSearchService.index(updatedDocument, updatedChunks)
        }
    }

    suspend fun rechunkOutdatedDocuments(
        expectedVersion: String,
        strategy: ChunkingStrategy
    ) {
        val outdated = knowledgeBaseRepository.findDocumentsWithOutdatedChunking(expectedVersion)
        for (documentId in outdated) {
            val docWithChunks = knowledgeBaseRepository.getDocumentWithChunks(documentId) ?: continue
            val combined = docWithChunks.chunks.joinToString("\n\n") { it.content }
            logger.i { "Re-chunking document=$documentId to version=$expectedVersion" }
            val updatedDocument = docWithChunks.document.copy(
                chunkingStrategyVersion = expectedVersion
            )
            ingestDocument(updatedDocument, combined, strategy)
        }
    }

    suspend fun deleteDocument(documentId: String) {
        logger.d { "Deleting document=$documentId from knowledge base and vector store" }
        vectorStore.delete(documentId)
        lexicalSearchService.delete(documentId)
        knowledgeBaseRepository.deleteDocument(documentId)
    }

    private fun attachEmbeddings(
        chunks: List<DocumentChunk>,
        embeddings: List<org.oleg.ai.challenge.domain.rag.model.Embedding>,
        embeddingVersion: String
    ): List<DocumentChunk> {
        return chunks.mapIndexed { index, chunk ->
            val embedding = embeddings.getOrNull(index)
                ?: error("Missing embedding for chunk index=$index")
            chunk.copy(
                embedding = embedding,
                embeddingModelVersion = embeddingVersion
            )
        }
    }
}
