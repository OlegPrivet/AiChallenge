package org.oleg.ai.challenge.data.rag.reranker

import org.oleg.ai.challenge.domain.rag.model.RetrievalResult

/**
 * Interface for computing BM25 lexical scores for document chunks.
 *
 * BM25 (Best Matching 25) is a ranking function used for information retrieval
 * that considers term frequency and document length normalization.
 *
 * Platform-specific implementations:
 * - JVM/Android: Uses Lucene BM25Similarity
 * - iOS: Uses simplified TF-IDF or delegates to semantic scoring
 */
interface BM25Scorer {
    /**
     * Computes BM25 scores for the given query and retrieval results.
     *
     * @param query The search query string
     * @param results The retrieval results to score
     * @return Map of chunk ID to normalized BM25 score [0, 1]
     */
    suspend fun computeScores(
        query: String,
        results: List<RetrievalResult>
    ): Map<String, Double>

    /**
     * Indicates whether this BM25 scorer is available on the current platform.
     * Returns false if BM25 scoring is not supported and fallback scoring will be used.
     */
    fun isAvailable(): Boolean = true
}

/**
 * Fallback BM25 scorer that returns uniform scores.
 *
 * Used when platform-specific BM25 implementation is unavailable.
 * Can be replaced with a simple TF-IDF implementation if needed.
 */
class FallbackBM25Scorer : BM25Scorer {
    override suspend fun computeScores(
        query: String,
        results: List<RetrievalResult>
    ): Map<String, Double> {
        // Simple fallback: return uniform scores
        // In a more sophisticated implementation, this could do basic TF-IDF
        return results.associate { it.chunk.id to 0.5 }
    }

    override fun isAvailable(): Boolean = false
}
