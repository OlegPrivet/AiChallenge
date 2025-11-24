package org.oleg.ai.challenge.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.oleg.ai.challenge.data.database.converters.Converters
import org.oleg.ai.challenge.data.database.dao.AgentDao
import org.oleg.ai.challenge.data.database.dao.ChatDao
import org.oleg.ai.challenge.data.database.dao.DocumentDao
import org.oleg.ai.challenge.data.database.dao.McpServerDao
import org.oleg.ai.challenge.data.database.dao.MessageDao
import org.oleg.ai.challenge.data.database.dao.QueryHistoryDao
import org.oleg.ai.challenge.data.database.entity.AgentEntity
import org.oleg.ai.challenge.data.database.entity.ChatEntity
import org.oleg.ai.challenge.data.database.entity.DocumentChunkEntity
import org.oleg.ai.challenge.data.database.entity.DocumentEntity
import org.oleg.ai.challenge.data.database.entity.McpServerEntity
import org.oleg.ai.challenge.data.database.entity.MessageEntity
import org.oleg.ai.challenge.data.database.entity.QueryHistoryEntity

/**
 * Room database for the application.
 *
 * Contains the following entities:
 * - ChatEntity: Represents conversations/chats
 * - AgentEntity: Represents AI agents and subagents
 * - MessageEntity: Represents chat messages
 * - McpServerEntity: Represents MCP server configurations
 * - DocumentEntity / DocumentChunkEntity: Persisted knowledge base artifacts for RAG
 * - QueryHistoryEntity: RAG query history for statistics and analysis
 */
@Database(
    entities = [
        ChatEntity::class,
        AgentEntity::class,
        MessageEntity::class,
        McpServerEntity::class,
        DocumentEntity::class,
        DocumentChunkEntity::class,
        QueryHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    abstract fun agentDao(): AgentDao

    abstract fun messageDao(): MessageDao

    abstract fun mcpServerDao(): McpServerDao

    abstract fun documentDao(): DocumentDao

    abstract fun queryHistoryDao(): QueryHistoryDao

    companion object {
        const val DATABASE_NAME = "ai_challenge.db"
    }
}
