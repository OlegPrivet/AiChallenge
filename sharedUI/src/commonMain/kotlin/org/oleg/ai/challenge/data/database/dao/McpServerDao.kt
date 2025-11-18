package org.oleg.ai.challenge.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.oleg.ai.challenge.data.database.entity.McpServerEntity

/**
 * Data Access Object for MCP server configuration operations.
 */
@Dao
interface McpServerDao {

    /**
     * Get all saved MCP server configurations ordered by most recently updated first.
     */
    @Query("SELECT * FROM mcp_servers ORDER BY updatedAt DESC")
    fun getAllServers(): Flow<List<McpServerEntity>>

    /**
     * Get a specific server configuration by ID.
     */
    @Query("SELECT * FROM mcp_servers WHERE id = :id")
    suspend fun getServerById(id: Long): McpServerEntity?

    /**
     * Get a specific server configuration by ID as Flow (for reactive updates).
     */
    @Query("SELECT * FROM mcp_servers WHERE id = :id")
    fun getServerByIdFlow(id: Long): Flow<McpServerEntity?>

    /**
     * Get the currently active server configuration.
     * Should return only one server since only one can be active at a time.
     */
    @Query("SELECT * FROM mcp_servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServer(): McpServerEntity?

    /**
     * Get the currently active server as Flow for reactive updates.
     */
    @Query("SELECT * FROM mcp_servers WHERE isActive = 1 LIMIT 1")
    fun getActiveServerFlow(): Flow<McpServerEntity?>

    /**
     * Insert a new server configuration and return its auto-generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: McpServerEntity): Long

    /**
     * Update an existing server configuration.
     */
    @Update
    suspend fun updateServer(server: McpServerEntity)

    /**
     * Delete a server configuration by ID.
     */
    @Query("DELETE FROM mcp_servers WHERE id = :id")
    suspend fun deleteServer(id: Long)

    /**
     * Delete a server configuration entity.
     */
    @Delete
    suspend fun deleteServer(server: McpServerEntity)

    /**
     * Deactivate all servers.
     * Used before setting a new active server to ensure only one is active.
     */
    @Query("UPDATE mcp_servers SET isActive = 0")
    suspend fun deactivateAllServers()

    /**
     * Set a specific server as active.
     * Note: Should be called after deactivateAllServers() in a transaction.
     */
    @Query("UPDATE mcp_servers SET isActive = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setServerActive(id: Long, updatedAt: Long)

    /**
     * Update server name and timestamp.
     */
    @Query("UPDATE mcp_servers SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateServerName(id: Long, name: String, updatedAt: Long)
}
