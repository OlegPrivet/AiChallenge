package org.oleg.ai.challenge.data.rag.pipeline

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.domain.rag.QueryEmbedding
import org.oleg.ai.challenge.domain.rag.SearchPipeline
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingRequest
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingService
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult

/**
 * Hybrid search pipeline combining vector (semantic) and lexical (BM25 keyword) results
 * using Reciprocal Rank Fusion (RRF).
 *
 * ## Reciprocal Rank Fusion (RRF)
 * RRF is a rank-based fusion algorithm that combines multiple ranked result lists.
 * Each result receives a score based on its rank position:
 * ```
 * score = weight / (k + rank + 1)
 * ```
 * where `k` is a constant (default 60) that controls how quickly scores decrease with rank.
 *
 * ## Weight Configuration
 * The [vectorWeight] and [lexicalWeight] parameters control the balance between:
 * - **Vector search (semantic)**: Finds results with similar meaning/context
 * - **Lexical search (keyword)**: Finds results with exact keyword matches
 *
 * ### Recommended Weight Ranges:
 * - **Equal weights (1.0, 1.0)**: Balanced approach, good default
 * - **Favor semantic (2.0, 1.0)**: Better for conceptual queries, synonyms
 * - **Favor keywords (1.0, 2.0)**: Better for exact terms, technical jargon
 * - **Disable lexical (1.0, 0.0)**: Pure vector search
 * - **Disable vector (0.0, 1.0)**: Pure keyword search
 *
 * @param vectorPipeline The semantic vector search pipeline
 * @param lexicalPipeline The BM25 keyword search pipeline
 * @param embeddingService Service for generating query embeddings
 * @param embeddingModel The embedding model name to use
 * @param embeddingModelVersion The version of the embedding model
 * @param rrfK The RRF constant (default 60). Higher values flatten score differences
 * @param vectorWeight Weight for vector/semantic results (default 1.0)
 * @param lexicalWeight Weight for lexical/keyword results (default 1.0)
 * @param logger Logger instance
 */
class HybridSearchPipeline(
    private val vectorPipeline: SearchPipeline,
    private val lexicalPipeline: SearchPipeline,
    private val embeddingService: EmbeddingService,
    private val embeddingModel: String,
    private val embeddingModelVersion: String,
    private val rrfK: Int = 60,
    private val vectorWeight: Double = 1.0,
    private val lexicalWeight: Double = 1.0,
    private val logger: Logger = Logger.withTag("HybridSearchPipeline")
) : SearchPipeline {

    override suspend fun search(
        query: String,
        embedding: QueryEmbedding?,
        topK: Int,
        filters: Map<String, String>
    ): List<RetrievalResult> {
        val queryEmbedding = embedding ?: embedQuery(query)

        val vectorResults = vectorPipeline.search(
            query = query,
            embedding = queryEmbedding,
            topK = topK,
            filters = filters
        )
        val lexicalResults = lexicalPipeline.search(
            query = query,
            embedding = queryEmbedding,
            topK = topK,
            filters = filters
        )

        val fused = fuseWithRrf(vectorResults, lexicalResults, topK)
        logger.d {
            "Hybrid pipeline returned ${fused.size} results " +
            "(vector=${vectorResults.size} w=$vectorWeight, " +
            "lexical=${lexicalResults.size} w=$lexicalWeight, " +
            "rrfK=$rrfK)"
        }
        return fused
    }

    private fun fuseWithRrf(
        vectorResults: List<RetrievalResult>,
        lexicalResults: List<RetrievalResult>,
        topK: Int
    ): List<RetrievalResult> {
        val scoreMap = linkedMapOf<String, Pair<RetrievalResult, Double>>()

        fun applyRrf(results: List<RetrievalResult>, weight: Double = 1.0) {
            results.forEachIndexed { index, result ->
                val key = result.chunk.id
                val score = weight * (1.0 / (rrfK + index + 1))
                val existing = scoreMap[key]
                if (existing == null) {
                    scoreMap[key] = result to score
                } else {
                    scoreMap[key] = existing.first to (existing.second + score)
                }
            }
        }

        applyRrf(vectorResults, vectorWeight)
        applyRrf(lexicalResults, lexicalWeight)

        return scoreMap.values
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first.copy(rerankedScore = it.second) }
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
