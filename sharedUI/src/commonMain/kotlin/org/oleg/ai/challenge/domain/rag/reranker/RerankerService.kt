package org.oleg.ai.challenge.domain.rag.reranker

import org.oleg.ai.challenge.domain.rag.model.RetrievalResult

/**
 * Service for reranking search results using cross-encoder models.
 *
 * Reranking improves search quality by computing more accurate relevance scores
 * between a query and candidate documents using a cross-encoder architecture.
 */
interface RerankerService {
    /**
     * Reranks a list of retrieval results based on their relevance to the query.
     *
     * @param query The user's search query
     * @param results The initial search results to rerank
     * @return List of results with populated [RetrievalResult.rerankedScore] field,
     *         maintaining the original score for comparison
     */
    suspend fun rerank(query: String, results: List<RetrievalResult>): List<RetrievalResult>
}
