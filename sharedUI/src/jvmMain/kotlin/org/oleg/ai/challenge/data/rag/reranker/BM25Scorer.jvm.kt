package org.oleg.ai.challenge.data.rag.reranker

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.domain.rag.lexical.LexicalSearchService
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult
import kotlin.math.max
import kotlin.math.min

/**
 * JVM-specific BM25 scorer implementation using Lucene.
 *
 * This scorer leverages the existing LuceneBm25SearchService to compute
 * BM25 scores for document chunks. It performs a fresh BM25 search and
 * matches the results with the provided retrieval results.
 *
 * @param lexicalSearchService The Lucene-based BM25 search service
 * @param logger Logger for diagnostics
 */
class LuceneBM25Scorer(
    private val lexicalSearchService: LexicalSearchService,
    private val logger: Logger = Logger.withTag("LuceneBM25Scorer")
) : BM25Scorer {

    override suspend fun computeScores(
        query: String,
        results: List<RetrievalResult>
    ): Map<String, Double> {
        if (results.isEmpty()) {
            logger.d { "No results to score" }
            return emptyMap()
        }

        if (query.isBlank()) {
            logger.w { "Empty query, returning uniform scores" }
            return results.associate { it.chunk.id to 0.5 }
        }

        try {
            // Perform BM25 search to get scores
            // We search for more results than needed to ensure we get scores for all chunks
            val topK = maxOf(results.size, 100)
            val lexicalMatches = lexicalSearchService.search(query, topK)

            // Create a map of chunk ID to BM25 score
            val bm25ScoresMap = lexicalMatches.associate { match ->
                match.chunkId to match.score
            }

            // Map the BM25 scores to the provided results
            val resultScores = results.associate { result ->
                result.chunk.id to (bm25ScoresMap[result.chunk.id] ?: 0.0)
            }

            // Normalize scores to [0, 1] range
            val normalizedScores = normalizeScores(resultScores)

            logger.d {
                val avgScore = normalizedScores.values.average()
                val matchedCount = normalizedScores.count { it.value > 0.0 }
                "BM25 scoring complete: $matchedCount/${results.size} chunks matched, avg score=$avgScore"
            }

            return normalizedScores

        } catch (e: Exception) {
            logger.e(e) { "BM25 scoring failed, returning fallback scores" }
            // Return uniform scores on failure
            return results.associate { it.chunk.id to 0.5 }
        }
    }

    override fun isAvailable(): Boolean = true

    /**
     * Normalizes scores to [0, 1] range using min-max normalization.
     */
    private fun normalizeScores(scores: Map<String, Double>): Map<String, Double> {
        if (scores.isEmpty()) return scores

        val values = scores.values
        val minScore = values.minOrNull() ?: 0.0
        val maxScore = values.maxOrNull() ?: 1.0

        // If all scores are the same, return them as-is
        if (minScore == maxScore) {
            logger.d { "All BM25 scores are identical ($minScore), skipping normalization" }
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
