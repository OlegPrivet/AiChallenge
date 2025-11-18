package org.oleg.ai.challenge.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.oleg.ai.challenge.data.database.converters.Converters
import org.oleg.ai.challenge.data.database.dao.AgentDao
import org.oleg.ai.challenge.data.database.dao.ChatDao
import org.oleg.ai.challenge.data.database.dao.McpServerDao
import org.oleg.ai.challenge.data.database.dao.MessageDao
import org.oleg.ai.challenge.data.database.entity.AgentEntity
import org.oleg.ai.challenge.data.database.entity.ChatEntity
import org.oleg.ai.challenge.data.database.entity.McpServerEntity
import org.oleg.ai.challenge.data.database.entity.MessageEntity

/**
 * Room database for the application.
 *
 * Contains four main entities:
 * - ChatEntity: Represents conversations/chats
 * - AgentEntity: Represents AI agents and subagents
 * - MessageEntity: Represents chat messages
 * - McpServerEntity: Represents MCP server configurations
 */
@Database(
    entities = [
        ChatEntity::class,
        AgentEntity::class,
        MessageEntity::class,
        McpServerEntity::class
    ],
    autoMigrations = [AutoMigration(1, 2)],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    abstract fun agentDao(): AgentDao

    abstract fun messageDao(): MessageDao

    abstract fun mcpServerDao(): McpServerDao

    companion object {
        const val DATABASE_NAME = "ai_challenge.db"
    }
}
