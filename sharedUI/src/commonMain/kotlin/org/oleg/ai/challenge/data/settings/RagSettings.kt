package org.oleg.ai.challenge.data.settings

import kotlinx.serialization.Serializable

@Serializable
data class RagSettings(
    val topK: Int = 5,
    val similarityThreshold: Float = 0.7f,
    val chunkingStrategy: String = "recursive",
    val hybridSearchWeight: Float = 0.5f,
    val enableHybridSearch: Boolean = false,
    val enableAgenticRag: Boolean = true,
    val enableExternalTools: Boolean = false,
    val enableReranker: Boolean = false,
    val rerankerThreshold: Float = 0.5f,
    val bm25Weight: Float = 0.3f,
    val semanticWeight: Float = 0.7f
) {
    companion object {
        const val MIN_TOP_K = 1
        const val MAX_TOP_K = 20
        const val MIN_SIMILARITY = 0.0f
        const val MAX_SIMILARITY = 1.0f
        const val MIN_HYBRID_WEIGHT = 0.0f
        const val MAX_HYBRID_WEIGHT = 1.0f
        const val MIN_RERANKER_THRESHOLD = 0.0f
        const val MAX_RERANKER_THRESHOLD = 1.0f
        const val MIN_RERANKER_WEIGHT = 0.0f
        const val MAX_RERANKER_WEIGHT = 1.0f
        const val DEFAULT_BM25_WEIGHT = 0.3f
        const val DEFAULT_SEMANTIC_WEIGHT = 0.7f

        val CHUNKING_STRATEGIES = listOf(
            "character" to "Character-based (simple)",
            "sentence" to "Sentence-based (semantic)",
            "recursive" to "Recursive (recommended)"
        )
    }
}
