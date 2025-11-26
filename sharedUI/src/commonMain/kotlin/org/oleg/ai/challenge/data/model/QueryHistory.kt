package org.oleg.ai.challenge.data.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a RAG query history entry.
 */
@Serializable
data class QueryHistory(
    val id: Long = 0,
    val queryText: String,
    val timestamp: Long,
    val resultsCount: Int,
    val averageRelevanceScore: Double,
    val citationsCount: Int,
    val documentIds: List<String> = emptyList(),
    val topK: Int,
    val similarityThreshold: Double,
    val hybridSearchEnabled: Boolean = false,
    val hybridSearchWeight: Double? = null,
    val rerankerEnabled: Boolean = false,
    val rerankerThreshold: Float? = null,
    val resultsBeforeReranking: Int? = null,
    val averageScoreImprovement: Double? = null
)
