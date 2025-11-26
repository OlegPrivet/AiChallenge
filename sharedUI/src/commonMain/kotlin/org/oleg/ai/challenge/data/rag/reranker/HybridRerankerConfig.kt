package org.oleg.ai.challenge.data.rag.reranker

/**
 * Configuration for the hybrid reranker that combines multiple scoring signals.
 *
 * @param bm25Weight Weight applied to BM25 lexical scores (default: 0.3)
 * @param semanticWeight Weight applied to semantic similarity scores (default: 0.7)
 * @param minimumScore Minimum combined score threshold for filtering results (default: 0.5)
 */
data class HybridRerankerConfig(
    val bm25Weight: Float = DEFAULT_BM25_WEIGHT,
    val semanticWeight: Float = DEFAULT_SEMANTIC_WEIGHT,
    val minimumScore: Float = DEFAULT_MINIMUM_SCORE
) {
    init {
        require(bm25Weight >= 0f && bm25Weight <= 1f) {
            "BM25 weight must be between 0 and 1, got $bm25Weight"
        }
        require(semanticWeight >= 0f && semanticWeight <= 1f) {
            "Semantic weight must be between 0 and 1, got $semanticWeight"
        }
        require(minimumScore >= 0f && minimumScore <= 1f) {
            "Minimum score must be between 0 and 1, got $minimumScore"
        }
    }

    /**
     * Returns the total weight (should typically sum to 1.0 for normalized scores).
     */
    val totalWeight: Float
        get() = bm25Weight + semanticWeight

    /**
     * Returns normalized weights that sum to 1.0.
     */
    fun normalizedWeights(): Pair<Float, Float> {
        val total = totalWeight
        return if (total > 0f) {
            Pair(bm25Weight / total, semanticWeight / total)
        } else {
            // Fallback to equal weights if both are 0
            Pair(0.5f, 0.5f)
        }
    }

    companion object {
        const val DEFAULT_BM25_WEIGHT = 0.3f
        const val DEFAULT_SEMANTIC_WEIGHT = 0.7f
        const val DEFAULT_MINIMUM_SCORE = 0.5f

        /**
         * Returns a config with equal BM25 and semantic weights.
         */
        fun balanced() = HybridRerankerConfig(
            bm25Weight = 0.5f,
            semanticWeight = 0.5f
        )

        /**
         * Returns a config that heavily favors semantic similarity.
         */
        fun semanticFocused() = HybridRerankerConfig(
            bm25Weight = 0.2f,
            semanticWeight = 0.8f
        )

        /**
         * Returns a config that heavily favors BM25 lexical matching.
         */
        fun lexicalFocused() = HybridRerankerConfig(
            bm25Weight = 0.8f,
            semanticWeight = 0.2f
        )
    }
}
