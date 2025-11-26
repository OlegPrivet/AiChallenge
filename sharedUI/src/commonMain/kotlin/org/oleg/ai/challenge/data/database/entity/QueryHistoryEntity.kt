package org.oleg.ai.challenge.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a RAG query history entry.
 *
 * Tracks RAG queries for statistics and analysis purposes.
 */
@Entity(
    tableName = "query_history",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class QueryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The query text submitted by the user */
    val queryText: String,

    /** Timestamp when the query was executed */
    val timestamp: Long,

    /** Number of chunks retrieved */
    val resultsCount: Int,

    /** Average similarity/relevance score of retrieved chunks */
    val averageRelevanceScore: Double,

    /** Number of citations generated from this query */
    val citationsCount: Int,

    /** JSON string of document IDs used in results (serialized List<String>) */
    val documentIdsJson: String? = null,

    /** Top-K value used for this query */
    val topK: Int,

    /** Similarity threshold used for this query */
    val similarityThreshold: Double,

    /** Whether hybrid search was enabled */
    val hybridSearchEnabled: Boolean = false,

    /** Hybrid search weight if applicable */
    val hybridSearchWeight: Double? = null,

    /** Whether cross-encoder reranking was enabled */
    val rerankerEnabled: Boolean = false,

    /** Reranker relevance threshold if applicable */
    val rerankerThreshold: Float? = null,

    /** Number of results before reranking was applied */
    val resultsBeforeReranking: Int? = null,

    /** Average score improvement from reranking */
    val averageScoreImprovement: Double? = null
)
