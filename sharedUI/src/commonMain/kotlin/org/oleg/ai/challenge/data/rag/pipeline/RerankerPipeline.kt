package org.oleg.ai.challenge.data.rag.pipeline

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.domain.rag.QueryEmbedding
import org.oleg.ai.challenge.domain.rag.SearchPipeline
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult
import org.oleg.ai.challenge.domain.rag.reranker.RerankerService
import org.oleg.ai.challenge.utils.format

/**
 * Decorator that adds cross-encoder reranking to any search pipeline.
 *
 * This decorator implements the Decorator pattern to add reranking functionality
 * on top of existing search pipelines (vector, lexical, or hybrid). It:
 *
 * 1. Delegates initial search to the wrapped pipeline
 * 2. Applies cross-encoder reranking to improve relevance scores
 * 3. Filters results below the relevance threshold
 * 4. Returns reranked and filtered results
 *
 * The decorator can be disabled via the [enabled] flag, allowing for A/B testing
 * and comparison of results with and without reranking.
 *
 * @param innerPipeline The search pipeline to wrap (e.g., VectorSearchPipeline)
 * @param rerankerService Service for computing cross-encoder relevance scores
 * @param relevanceThreshold Minimum score to keep results (0.0 to 1.0)
 * @param enabled Whether reranking is active (false = passthrough to inner pipeline)
 * @param logger Logger for diagnostics and performance tracking
 */
class RerankerPipeline(
    private val innerPipeline: SearchPipeline,
    private val rerankerService: RerankerService,
    private val relevanceThreshold: Double,
    private val enabled: Boolean = true,
    private val logger: Logger = Logger.withTag("RerankerPipeline")
) : SearchPipeline {

    override suspend fun search(
        query: String,
        embedding: QueryEmbedding?,
        topK: Int,
        filters: Map<String, String>
    ): List<RetrievalResult> {
        // Step 1: Get initial results from wrapped pipeline
        val initialResults = innerPipeline.search(query, embedding, topK, filters)

        // If reranking is disabled, return results as-is
        if (!enabled) {
            logger.d { "Reranking disabled, returning ${initialResults.size} initial results" }
            return initialResults
        }

        // If no results to rerank, return early
        if (initialResults.isEmpty()) {
            logger.d { "No results to rerank" }
            return initialResults
        }

        logger.d { "Reranking ${initialResults.size} initial results (threshold=$relevanceThreshold)" }

        // Step 2: Apply cross-encoder reranking
        val reranked = rerankerService.rerank(query, initialResults)

        // Step 3: Filter by relevance threshold
        // Use reranked score if available, otherwise fall back to original score
        val filtered = reranked.filter { result ->
            val score = result.rerankedScore ?: result.score
            score >= relevanceThreshold
        }

        // Step 4: Sort by reranked scores (descending)
        val sorted = filtered.sortedByDescending { it.rerankedScore ?: it.score }

        // Step 5: Take top-K after reranking and filtering
        val final = sorted.take(topK)

        logger.d {
            "Reranking complete: ${initialResults.size} → ${filtered.size} after filtering → ${final.size} final " +
                    "(filtered out ${initialResults.size - filtered.size} below threshold)"
        }

        // Log score statistics for analysis
        if (final.isNotEmpty()) {
            val avgOriginal = final.map { it.score }.average()
            val avgReranked = final.mapNotNull { it.rerankedScore }.average()
            val improvement = avgReranked - avgOriginal

            logger.d {
                "Score statistics: original avg=${avgOriginal.format(3)}, " +
                        "reranked avg=${avgReranked.format(3)}, " +
                        "improvement=${improvement.format(3)}"
            }
        }

        return final
    }
}
