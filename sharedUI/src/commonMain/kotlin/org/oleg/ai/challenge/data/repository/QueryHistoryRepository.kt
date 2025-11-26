package org.oleg.ai.challenge.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.oleg.ai.challenge.data.database.dao.QueryHistoryDao
import org.oleg.ai.challenge.data.database.entity.QueryHistoryEntity
import org.oleg.ai.challenge.data.model.QueryHistory

/**
 * Repository for managing RAG query history.
 */
interface QueryHistoryRepository {
    /**
     * Flow of all query history entries.
     */
    val queryHistory: Flow<List<QueryHistory>>

    /**
     * Save a new query history entry.
     */
    suspend fun saveQueryHistory(query: QueryHistory): Long

    /**
     * Get recent query history entries with a limit.
     */
    suspend fun getRecentQueryHistory(limit: Int): List<QueryHistory>

    /**
     * Delete a query history entry by ID.
     */
    suspend fun deleteQueryHistory(id: Long)

    /**
     * Delete all query history entries.
     */
    suspend fun deleteAllQueryHistory()

    /**
     * Get statistics about query history.
     */
    suspend fun getQueryStatistics(): QueryStatistics
}

/**
 * Statistics about RAG queries.
 */
data class QueryStatistics(
    val totalQueries: Int,
    val averageRelevanceScore: Double
)

/**
 * Default implementation of QueryHistoryRepository.
 */
class DefaultQueryHistoryRepository(
    private val queryHistoryDao: QueryHistoryDao
) : QueryHistoryRepository {

    private val logger = Logger.withTag("DefaultQueryHistoryRepository")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    override val queryHistory: Flow<List<QueryHistory>> =
        queryHistoryDao.observeQueryHistory()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }

    override suspend fun saveQueryHistory(query: QueryHistory): Long {
        return try {
            val entity = query.toEntity()
            val id = queryHistoryDao.insertQueryHistory(entity)
            logger.d { "Saved query history: '${query.queryText}' (id=$id)" }
            id
        } catch (e: Exception) {
            logger.e(e) { "Failed to save query history: ${query.queryText}" }
            throw e
        }
    }

    override suspend fun getRecentQueryHistory(limit: Int): List<QueryHistory> {
        return try {
            queryHistoryDao.getRecentQueryHistory(limit).map { it.toDomainModel() }
        } catch (e: Exception) {
            logger.e(e) { "Failed to get recent query history" }
            emptyList()
        }
    }

    override suspend fun deleteQueryHistory(id: Long) {
        try {
            queryHistoryDao.deleteQueryHistory(id)
            logger.d { "Deleted query history: id=$id" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete query history: id=$id" }
            throw e
        }
    }

    override suspend fun deleteAllQueryHistory() {
        try {
            queryHistoryDao.deleteAllQueryHistory()
            logger.i { "Deleted all query history" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete all query history" }
            throw e
        }
    }

    override suspend fun getQueryStatistics(): QueryStatistics {
        return try {
            val totalQueries = queryHistoryDao.getQueryCount()
            val averageScore = queryHistoryDao.getAverageRelevanceScore() ?: 0.0
            QueryStatistics(
                totalQueries = totalQueries,
                averageRelevanceScore = averageScore
            )
        } catch (e: Exception) {
            logger.e(e) { "Failed to get query statistics" }
            QueryStatistics(totalQueries = 0, averageRelevanceScore = 0.0)
        }
    }

    /**
     * Convert domain model to database entity.
     */
    private fun QueryHistory.toEntity(): QueryHistoryEntity {
        return QueryHistoryEntity(
            id = id,
            queryText = queryText,
            timestamp = timestamp,
            resultsCount = resultsCount,
            averageRelevanceScore = averageRelevanceScore,
            citationsCount = citationsCount,
            documentIdsJson = if (documentIds.isNotEmpty()) {
                json.encodeToString(documentIds)
            } else {
                null
            },
            topK = topK,
            similarityThreshold = similarityThreshold,
            hybridSearchEnabled = hybridSearchEnabled,
            hybridSearchWeight = hybridSearchWeight,
            rerankerEnabled = rerankerEnabled,
            rerankerThreshold = rerankerThreshold,
            resultsBeforeReranking = resultsBeforeReranking,
            averageScoreImprovement = averageScoreImprovement
        )
    }

    /**
     * Convert database entity to domain model.
     */
    private fun QueryHistoryEntity.toDomainModel(): QueryHistory {
        return QueryHistory(
            id = id,
            queryText = queryText,
            timestamp = timestamp,
            resultsCount = resultsCount,
            averageRelevanceScore = averageRelevanceScore,
            citationsCount = citationsCount,
            documentIds = try {
                documentIdsJson?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse document IDs for query $id, using empty list" }
                emptyList()
            },
            topK = topK,
            similarityThreshold = similarityThreshold,
            hybridSearchEnabled = hybridSearchEnabled,
            hybridSearchWeight = hybridSearchWeight,
            rerankerEnabled = rerankerEnabled,
            rerankerThreshold = rerankerThreshold,
            resultsBeforeReranking = resultsBeforeReranking,
            averageScoreImprovement = averageScoreImprovement
        )
    }
}
