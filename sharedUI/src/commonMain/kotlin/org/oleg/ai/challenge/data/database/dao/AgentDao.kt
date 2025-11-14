package org.oleg.ai.challenge.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.oleg.ai.challenge.data.database.entity.AgentEntity

/**
 * Data Access Object for agent operations.
 */
@Dao
interface AgentDao {

    /**
     * Get all agents for a specific chat.
     */
    @Query("SELECT * FROM agents WHERE chatId = :chatId ORDER BY isMain DESC, id ASC")
    fun getAgentsForChat(chatId: Long): Flow<List<AgentEntity>>

    /**
     * Get all agents for a specific chat (suspend version).
     */
    @Query("SELECT * FROM agents WHERE chatId = :chatId ORDER BY isMain DESC, id ASC")
    suspend fun getAgentsForChatSuspend(chatId: Long): List<AgentEntity>

    /**
     * Get the main agent for a chat.
     */
    @Query("SELECT * FROM agents WHERE chatId = :chatId AND isMain = 1 LIMIT 1")
    suspend fun getMainAgentForChat(chatId: Long): AgentEntity?

    /**
     * Get a specific agent by agentId and chatId.
     */
    @Query("SELECT * FROM agents WHERE agentId = :agentId AND chatId = :chatId LIMIT 1")
    suspend fun getAgent(agentId: String, chatId: Long): AgentEntity?

    /**
     * Insert a single agent.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity): Long

    /**
     * Insert multiple agents.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgents(agents: List<AgentEntity>): List<Long>

    /**
     * Update an existing agent.
     */
    @Update
    suspend fun updateAgent(agent: AgentEntity)

    /**
     * Delete all agents for a specific chat.
     */
    @Query("DELETE FROM agents WHERE chatId = :chatId")
    suspend fun deleteAgentsForChat(chatId: Long)

    /**
     * Delete a specific agent.
     */
    @Query("DELETE FROM agents WHERE agentId = :agentId AND chatId = :chatId")
    suspend fun deleteAgent(agentId: String, chatId: Long)
}
