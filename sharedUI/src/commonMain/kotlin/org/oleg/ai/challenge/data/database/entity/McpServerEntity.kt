package org.oleg.ai.challenge.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Room entity representing an MCP server configuration.
 *
 * Stores server connection details, authentication settings, and active status.
 * Auth headers and environment variables are stored as JSON strings.
 */
@Entity(tableName = "mcp_servers")
data class McpServerEntity @OptIn(ExperimentalTime::class) constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * User-friendly name for this server configuration.
     */
    val name: String,

    /**
     * Transport type: "SSE" or "STDIO"
     */
    val transportType: String,

    /**
     * Server URL for SSE transport, or command path for STDIO transport.
     */
    val serverUrl: String,

    /**
     * Authentication type: "NONE", "BEARER", "API_KEY", "CUSTOM_HEADERS"
     */
    val authType: String,

    /**
     * Authentication headers as JSON string.
     * Example: {"Authorization": "Bearer token123", "X-API-Key": "key456"}
     */
    val authHeadersJson: String = "{}",

    /**
     * Command line arguments for STDIO transport as JSON array string.
     * Example: ["--verbose", "--port=8080"]
     */
    val commandArgsJson: String = "[]",

    /**
     * Environment variables for STDIO transport as JSON string.
     * Example: {"NODE_ENV": "production", "API_KEY": "key123"}
     */
    val environmentVarsJson: String = "{}",

    /**
     * Whether this server is currently set as active.
     * Only one server should be active at a time.
     */
    val isActive: Boolean = false,

    /**
     * Timestamp when this configuration was created.
     */
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),

    /**
     * Timestamp when this configuration was last updated.
     */
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
