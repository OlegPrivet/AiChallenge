package org.oleg.ai.challenge.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an agent or subagent associated with a chat.
 *
 * - Main agents have isMain = true
 * - Subagents have parentAgentId pointing to their parent agent's agentId
 * - All agents for a chat are grouped by chatId foreign key
 */
@Entity(
    tableName = "agents",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chatId"]),
        Index(value = ["agentId"])
    ]
)
data class AgentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The agent's unique identifier (from Agent.id) */
    val agentId: String,

    /** Foreign key to the chat this agent belongs to */
    val chatId: Long,

    /** Human-readable agent name */
    val agentName: String,

    /** System prompt for the agent */
    val systemPrompt: String? = null,

    /** Assistant/instruction prompt for the agent */
    val assistantPrompt: String? = null,

    /** AI model used by this agent */
    val model: String,

    /** Temperature setting for the agent */
    val temperature: Float = 1.0f,

    /** Flag indicating if this is the main chat agent */
    val isMain: Boolean = false,

    /** Parent agent ID if this is a subagent (nullable) */
    val parentAgentId: String? = null
)
