package org.oleg.ai.challenge.data.rag.reranker

import co.touchlab.kermit.Logger
import kotlin.math.max
import kotlin.math.min

/**
 * Utility for fusing multiple ranking scores into a single combined score.
 *
 * Currently implements weighted linear combination, but designed to be
 * extensible for more sophisticated fusion strategies like:
 * - Reciprocal Rank Fusion (RRF)
 * - CombSUM/CombMNZ
 * - Learned fusion models
 */
class ScoreFusion(
    private val logger: Logger = Logger.withTag("ScoreFusion")
) {
    /**
     * Combines BM25 and semantic scores using weighted linear combination.
     *
     * Formula: finalScore = (bm25Weight * bm25Score) + (semanticWeight * semanticScore)
     *
     * Weights are normalized to sum to 1.0 if they don't already.
     *
     * @param bm25Scores Map of chunk ID to BM25 score [0, 1]
     * @param semanticScores Map of chunk ID to semantic similarity score [0, 1]
     * @param config Configuration specifying weights
     * @return Map of chunk ID to fused score [0, 1]
     */
    fun fuseScores(
        bm25Scores: Map<String, Double>,
        semanticScores: Map<String, Double>,
        config: HybridRerankerConfig
    ): Map<String, Double> {
        // Get all unique chunk IDs from both score maps
        val allChunkIds = (bm25Scores.keys + semanticScores.keys).distinct()

        if (allChunkIds.isEmpty()) {
            logger.d { "No scores to fuse" }
            return emptyMap()
        }

        // Normalize weights to sum to 1.0
        val (normalizedBm25Weight, normalizedSemanticWeight) = config.normalizedWeights()

        logger.d {
            "Fusing scores with normalized weights: BM25=$normalizedBm25Weight, Semantic=$normalizedSemanticWeight"
        }

        // Compute fused scores
        val fusedScores = allChunkIds.associateWith { chunkId ->
            val bm25 = bm25Scores[chunkId] ?: 0.0
            val semantic = semanticScores[chunkId] ?: 0.0

            val fused = (normalizedBm25Weight * bm25) + (normalizedSemanticWeight * semantic)

            // Clamp to [0, 1] range to handle any floating point errors
            max(0.0, min(1.0, fused))
        }

        logger.d {
            val avgBm25 = bm25Scores.values.average()
            val avgSemantic = semanticScores.values.average()
            val avgFused = fusedScores.values.average()
            "Score fusion complete: avg BM25=$avgBm25, avg Semantic=$avgSemantic, avg Fused=$avgFused"
        }

        return fusedScores
    }

    /**
     * Applies Reciprocal Rank Fusion (RRF) to combine rankings.
     *
     * RRF formula: score(d) = Σ 1/(k + rank(d))
     * where k is a constant (typically 60) and rank(d) is the position in each ranking.
     *
     * This is a more sophisticated fusion strategy that can be added in the future.
     *
     * @param rankings List of rankings (each ranking is a list of chunk IDs in order)
     * @param k RRF constant parameter (default: 60)
     * @return Map of chunk ID to RRF score
     */
    fun applyReciprocalRankFusion(
        rankings: List<List<String>>,
        k: Int = 60
    ): Map<String, Double> {
        if (rankings.isEmpty()) {
            return emptyMap()
        }

        val rrfScores = mutableMapOf<String, Double>()

        // For each ranking, compute RRF contribution
        rankings.forEach { ranking ->
            ranking.forEachIndexed { index, chunkId ->
                val rank = index + 1 // 1-based ranking
                val rrfContribution = 1.0 / (k + rank)
                rrfScores[chunkId] = (rrfScores[chunkId] ?: 0.0) + rrfContribution
            }
        }

        // Normalize RRF scores to [0, 1] range
        if (rrfScores.isEmpty()) return emptyMap()

        val maxScore = rrfScores.values.maxOrNull() ?: 1.0
        return if (maxScore > 0.0) {
            rrfScores.mapValues { (_, score) -> score / maxScore }
        } else {
            rrfScores
        }
    }
}
