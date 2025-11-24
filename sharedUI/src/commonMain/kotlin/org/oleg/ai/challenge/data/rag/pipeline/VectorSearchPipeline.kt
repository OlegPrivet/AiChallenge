package org.oleg.ai.challenge.data.rag.pipeline

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.domain.rag.QueryEmbedding
import org.oleg.ai.challenge.domain.rag.SearchPipeline
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingRequest
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingService
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult
import org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository
import org.oleg.ai.challenge.domain.rag.vector.VectorStore

class VectorSearchPipeline(
    private val embeddingService: EmbeddingService,
    private val vectorStore: VectorStore,
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val embeddingModel: String,
    private val embeddingModelVersion: String,
    private val logger: Logger = Logger.withTag("VectorSearchPipeline")
) : SearchPipeline {

    override suspend fun search(
        query: String,
        embedding: QueryEmbedding?,
        topK: Int,
        filters: Map<String, String>
    ): List<RetrievalResult> {
        val queryEmbedding = embedding ?: embedQuery(query)
        val matches = vectorStore.query(
            embedding = queryEmbedding.vector,
            topK = topK,
            filters = filters
        )

        val documentCache = mutableMapOf<String, org.oleg.ai.challenge.domain.rag.model.DocumentWithChunks?>()
        return matches.mapNotNull { match ->
            val documentId = match.documentId ?: return@mapNotNull null
            val docWithChunks = documentCache.getOrPut(documentId) {
                knowledgeBaseRepository.getDocumentWithChunks(documentId)
            } ?: return@mapNotNull null

            val chunk = docWithChunks.chunks.firstOrNull { it.id == match.id }
                ?: docWithChunks.chunks.firstOrNull { it.chunkIndex == match.chunkIndex }
                ?: return@mapNotNull null

            RetrievalResult(
                document = docWithChunks.document,
                chunk = chunk,
                score = match.score,
                rerankedScore = null
            )
        }.also { results ->
            logger.d { "Vector pipeline returned ${results.size} results for query='$query'" }
        }
    }

    private suspend fun embedQuery(query: String): QueryEmbedding {
        val embeddings = embeddingService.embed(
            EmbeddingRequest(
                texts = listOf(query),
                model = embeddingModel,
                embeddingModelVersion = embeddingModelVersion,
                normalize = true
            )
        )
        val embedding = embeddings.firstOrNull() ?: error("Failed to embed query")
        return QueryEmbedding(
            vector = embedding.values,
            model = embedding.model,
            version = embedding.embeddingModelVersion
        )
    }
}
