package org.oleg.ai.challenge.data.model

import org.oleg.ai.challenge.data.network.service.McpClientService

/**
 * Domain model representing an MCP server configuration.
 *
 * This is the business logic model used throughout the app, converted from/to
 * McpServerEntity for persistence and McpConnectionConfig for actual connection.
 */
data class McpServerConfig(
    /**
     * Unique identifier (0 for new, unsaved configs).
     */
    val id: Long = 0,

    /**
     * User-friendly name for this server configuration.
     */
    val name: String,

    /**
     * Transport type for connection (SSE or STDIO).
     */
    val transportType: McpClientService.McpTransportType,

    /**
     * Server URL for SSE transport, or command path for STDIO transport.
     */
    val serverUrl: String,

    /**
     * Authentication type.
     */
    val authType: AuthType = AuthType.NONE,

    /**
     * Authentication headers map (for SSE transport).
     * Used based on authType:
     * - BEARER: {"Authorization": "Bearer <token>"}
     * - API_KEY: {"<headerName>": "<apiKey>"}
     * - CUSTOM_HEADERS: {custom key-value pairs}
     */
    val authHeaders: Map<String, String> = emptyMap(),

    /**
     * Command line arguments (for STDIO transport).
     */
    val commandArgs: List<String> = emptyList(),

    /**
     * Environment variables for the process (for STDIO transport).
     */
    val environmentVars: Map<String, String> = emptyMap(),

    /**
     * Whether this server is currently set as active.
     */
    val isActive: Boolean = false,

    /**
     * Timestamp when this configuration was created.
     */
    val createdAt: Long = 0,

    /**
     * Timestamp when this configuration was last updated.
     */
    val updatedAt: Long = 0
) {

    /**
     * Convert this domain model to McpConnectionConfig for actual connection.
     */
    fun toConnectionConfig(): McpClientService.McpConnectionConfig {
        return McpClientService.McpConnectionConfig(
            transportType = transportType,
            serverUrl = serverUrl,
            headers = authHeaders,
            commandArgs = commandArgs,
            environmentVars = environmentVars
        )
    }

    /**
     * Authentication type enum.
     */
    enum class AuthType {
        /**
         * No authentication required.
         */
        NONE,

        /**
         * Bearer token authentication (Authorization: Bearer <token>).
         */
        BEARER,

        /**
         * API key in custom header (e.g., X-API-Key: <key>).
         */
        API_KEY,

        /**
         * Custom headers provided by user.
         */
        CUSTOM_HEADERS
    }

    companion object {
        /**
         * Create a new SSE server config with Bearer token.
         */
        fun withBearerToken(
            name: String,
            serverUrl: String,
            token: String
        ): McpServerConfig {
            return McpServerConfig(
                name = name,
                transportType = McpClientService.McpTransportType.SSE,
                serverUrl = serverUrl,
                authType = AuthType.BEARER,
                authHeaders = mapOf("Authorization" to "Bearer $token")
            )
        }

        /**
         * Create a new SSE server config with API key.
         */
        fun withApiKey(
            name: String,
            serverUrl: String,
            apiKey: String,
            headerName: String = "X-API-Key"
        ): McpServerConfig {
            return McpServerConfig(
                name = name,
                transportType = McpClientService.McpTransportType.SSE,
                serverUrl = serverUrl,
                authType = AuthType.API_KEY,
                authHeaders = mapOf(headerName to apiKey)
            )
        }

        /**
         * Create a new SSE server config with custom headers.
         */
        fun withCustomHeaders(
            name: String,
            serverUrl: String,
            headers: Map<String, String>
        ): McpServerConfig {
            return McpServerConfig(
                name = name,
                transportType = McpClientService.McpTransportType.SSE,
                serverUrl = serverUrl,
                authType = AuthType.CUSTOM_HEADERS,
                authHeaders = headers
            )
        }

        /**
         * Create a new STDIO server config.
         */
        fun forStdio(
            name: String,
            command: String,
            args: List<String> = emptyList(),
            env: Map<String, String> = emptyMap()
        ): McpServerConfig {
            return McpServerConfig(
                name = name,
                transportType = McpClientService.McpTransportType.STDIO,
                serverUrl = command,
                authType = AuthType.NONE,
                commandArgs = args,
                environmentVars = env
            )
        }

        /**
         * Create a new basic SSE server config without authentication.
         */
        fun basic(
            name: String,
            serverUrl: String
        ): McpServerConfig {
            return McpServerConfig(
                name = name,
                transportType = McpClientService.McpTransportType.SSE,
                serverUrl = serverUrl,
                authType = AuthType.NONE
            )
        }
    }
}
