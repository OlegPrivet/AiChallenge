package org.oleg.ai.challenge.data.rag.reranker

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.domain.rag.model.Embedding
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Computes semantic similarity scores using cosine similarity between embeddings.
 *
 * This scorer uses pre-computed embeddings from the vector search pipeline,
 * so it requires no additional API calls or model inference.
 *
 * Features:
 * - Cosine similarity computation
 * - Score normalization to [0, 1] range
 * - Handles missing embeddings gracefully
 */
class SemanticSimilarityScorer(
    private val logger: Logger = Logger.withTag("SemanticSimilarityScorer")
) {
    /**
     * Computes semantic similarity scores for retrieval results.
     *
     * @param queryEmbedding The embedding vector for the search query
     * @param results The retrieval results containing document chunk embeddings
     * @return Map of chunk ID to normalized similarity score [0, 1]
     */
    fun computeScores(
        queryEmbedding: Embedding,
        results: List<RetrievalResult>
    ): Map<String, Double> {
        if (results.isEmpty()) {
            logger.d { "No results to score" }
            return emptyMap()
        }

        val scores = mutableMapOf<String, Double>()

        results.forEach { result ->
            val chunkEmbedding = result.chunk.embedding
            if (chunkEmbedding == null) {
                logger.w { "Chunk ${result.chunk.id} has no embedding, using fallback score 0.0" }
                scores[result.chunk.id] = 0.0
            } else {
                val similarity = cosineSimilarity(queryEmbedding.values, chunkEmbedding.values)
                scores[result.chunk.id] = similarity
            }
        }

        // Normalize scores to [0, 1] if needed
        return normalizeScores(scores)
    }

    /**
     * Computes cosine similarity between two embedding vectors.
     *
     * Cosine similarity = (A · B) / (||A|| * ||B||)
     * Range: [-1, 1] where 1 = identical, 0 = orthogonal, -1 = opposite
     *
     * @return Similarity score in range [-1, 1], normalized to [0, 1]
     */
    private fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Double {
        require(vec1.size == vec2.size) {
            "Embedding dimensions must match: ${vec1.size} vs ${vec2.size}"
        }

        if (vec1.isEmpty()) return 0.0

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vec1.indices) {
            val v1 = vec1[i].toDouble()
            val v2 = vec2[i].toDouble()
            dotProduct += v1 * v2
            norm1 += v1 * v1
            norm2 += v2 * v2
        }

        norm1 = sqrt(norm1)
        norm2 = sqrt(norm2)

        if (norm1 == 0.0 || norm2 == 0.0) {
            logger.w { "Zero-norm embedding detected, returning similarity 0.0" }
            return 0.0
        }

        // Cosine similarity is in [-1, 1], convert to [0, 1]
        val similarity = dotProduct / (norm1 * norm2)
        return (similarity + 1.0) / 2.0
    }

    /**
     * Normalizes scores to [0, 1] range using min-max normalization.
     *
     * This ensures consistent score interpretation across different result sets.
     */
    private fun normalizeScores(scores: Map<String, Double>): Map<String, Double> {
        if (scores.isEmpty()) return scores

        val values = scores.values
        val minScore = values.minOrNull() ?: 0.0
        val maxScore = values.maxOrNull() ?: 1.0

        // If all scores are the same, return them as-is
        if (minScore == maxScore) {
            logger.d { "All scores are identical ($minScore), skipping normalization" }
            return scores
        }

        // Apply min-max normalization
        return scores.mapValues { (_, score) ->
            val normalized = (score - minScore) / (maxScore - minScore)
            // Clamp to [0, 1] to handle floating point errors
            max(0.0, min(1.0, normalized))
        }
    }
}
