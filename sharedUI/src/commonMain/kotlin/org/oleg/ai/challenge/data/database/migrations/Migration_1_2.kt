package org.oleg.ai.challenge.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migration from database version 1 to version 2.
 *
 * Adds reranker-related fields to the query_history table:
 * - rerankerEnabled: Whether cross-encoder reranking was enabled
 * - rerankerThreshold: The relevance threshold used for filtering
 * - resultsBeforeReranking: Number of results before reranking was applied
 * - averageScoreImprovement: Average score improvement from reranking
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // Add rerankerEnabled column (non-nullable with default false)
        connection.execSQL(
            "ALTER TABLE query_history ADD COLUMN rerankerEnabled INTEGER NOT NULL DEFAULT 0"
        )

        // Add rerankerThreshold column (nullable)
        connection.execSQL(
            "ALTER TABLE query_history ADD COLUMN rerankerThreshold REAL"
        )

        // Add resultsBeforeReranking column (nullable)
        connection.execSQL(
            "ALTER TABLE query_history ADD COLUMN resultsBeforeReranking INTEGER"
        )

        // Add averageScoreImprovement column (nullable)
        connection.execSQL(
            "ALTER TABLE query_history ADD COLUMN averageScoreImprovement REAL"
        )
    }
}
