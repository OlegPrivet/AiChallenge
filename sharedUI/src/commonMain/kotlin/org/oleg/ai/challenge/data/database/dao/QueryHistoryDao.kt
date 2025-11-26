package org.oleg.ai.challenge.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.oleg.ai.challenge.data.database.entity.QueryHistoryEntity

/**
 * DAO for managing RAG query history.
 */
@Dao
interface QueryHistoryDao {

    /**
     * Observe all query history entries, ordered by most recent first.
     */
    @Query("SELECT * FROM query_history ORDER BY timestamp DESC")
    fun observeQueryHistory(): Flow<List<QueryHistoryEntity>>

    /**
     * Get all query history entries, ordered by most recent first.
     */
    @Query("SELECT * FROM query_history ORDER BY timestamp DESC")
    suspend fun getAllQueryHistory(): List<QueryHistoryEntity>

    /**
     * Get recent query history entries with a limit.
     */
    @Query("SELECT * FROM query_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentQueryHistory(limit: Int): List<QueryHistoryEntity>

    /**
     * Get a single query history entry by ID.
     */
    @Query("SELECT * FROM query_history WHERE id = :id")
    suspend fun getQueryHistoryById(id: Long): QueryHistoryEntity?

    /**
     * Insert a new query history entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueryHistory(queryHistory: QueryHistoryEntity): Long

    /**
     * Delete a query history entry by ID.
     */
    @Query("DELETE FROM query_history WHERE id = :id")
    suspend fun deleteQueryHistory(id: Long)

    /**
     * Delete all query history entries.
     */
    @Query("DELETE FROM query_history")
    suspend fun deleteAllQueryHistory()

    /**
     * Delete query history entries older than the specified timestamp.
     */
    @Query("DELETE FROM query_history WHERE timestamp < :timestamp")
    suspend fun deleteQueryHistoryOlderThan(timestamp: Long)

    /**
     * Get the total count of queries.
     */
    @Query("SELECT COUNT(*) FROM query_history")
    suspend fun getQueryCount(): Int

    /**
     * Get the average relevance score across all queries.
     */
    @Query("SELECT AVG(averageRelevanceScore) FROM query_history")
    suspend fun getAverageRelevanceScore(): Double?
}
