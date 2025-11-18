package org.oleg.ai.challenge.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.oleg.ai.challenge.data.database.dao.McpServerDao
import org.oleg.ai.challenge.data.database.entity.McpServerEntity
import org.oleg.ai.challenge.data.model.McpServerConfig
import org.oleg.ai.challenge.data.network.service.McpClientService
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository for managing MCP server configurations.
 *
 * Handles persistence of server configurations and conversion between
 * database entities and domain models.
 */
interface McpServerRepository {
    /**
     * Flow of all saved server configurations.
     */
    val savedServers: Flow<List<McpServerConfig>>

    /**
     * Flow of the currently active server configuration.
     */
    val activeServer: Flow<McpServerConfig?>

    /**
     * Save a new server configuration or update existing one.
     *
     * @param config Server configuration to save
     * @return ID of the saved configuration
     */
    suspend fun saveServer(config: McpServerConfig): Long

    /**
     * Delete a server configuration by ID.
     *
     * @param id Server configuration ID
     */
    suspend fun deleteServer(id: Long)

    /**
     * Get a server configuration by ID.
     *
     * @param id Server configuration ID
     * @return Server configuration or null if not found
     */
    suspend fun getServerById(id: Long): McpServerConfig?

    /**
     * Set a server as the active one.
     * Deactivates all other servers.
     *
     * @param id Server configuration ID to activate
     */
    suspend fun setActiveServer(id: Long)

    /**
     * Deactivate all servers.
     */
    suspend fun deactivateAllServers()
}

/**
 * Default implementation of McpServerRepository.
 */
class DefaultMcpServerRepository(
    private val mcpServerDao: McpServerDao,
) : McpServerRepository {

    private val logger = Logger.withTag("DefaultMcpServerRepository")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    override val savedServers: Flow<List<McpServerConfig>> =
        mcpServerDao.getAllServers()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }

    override val activeServer: Flow<McpServerConfig?> =
        mcpServerDao.getActiveServerFlow()
            .map { it?.toDomainModel() }

    @OptIn(ExperimentalTime::class)
    override suspend fun saveServer(config: McpServerConfig): Long {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val entity = config.toEntity().copy(
                updatedAt = now,
                createdAt = if (config.id == 0L) now else config.createdAt
            )

            val id = mcpServerDao.insertServer(entity)
            logger.i { "Saved MCP server config: ${config.name} (id=$id)" }
            id
        } catch (e: Exception) {
            logger.e(e) { "Failed to save server config: ${config.name}" }
            throw e
        }
    }

    override suspend fun deleteServer(id: Long) {
        try {
            mcpServerDao.deleteServer(id)
            logger.i { "Deleted MCP server config: id=$id" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete server config: id=$id" }
            throw e
        }
    }

    override suspend fun getServerById(id: Long): McpServerConfig? {
        return try {
            mcpServerDao.getServerById(id)?.toDomainModel()
        } catch (e: Exception) {
            logger.e(e) { "Failed to get server config: id=$id" }
            null
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun setActiveServer(id: Long) {
        try {
            // Deactivate all servers first
            mcpServerDao.deactivateAllServers()

            // Activate the specified server
            val now = Clock.System.now().toEpochMilliseconds()
            mcpServerDao.setServerActive(id, now)

            logger.i { "Set active MCP server: id=$id" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to set active server: id=$id" }
            throw e
        }
    }

    override suspend fun deactivateAllServers() {
        try {
            mcpServerDao.deactivateAllServers()
            logger.i { "Deactivated all MCP servers" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to deactivate servers" }
            throw e
        }
    }

    /**
     * Convert domain model to database entity.
     */
    private fun McpServerConfig.toEntity(): McpServerEntity {
        return McpServerEntity(
            id = id,
            name = name,
            transportType = transportType.name,
            serverUrl = serverUrl,
            authType = authType.name,
            authHeadersJson = json.encodeToString(authHeaders),
            commandArgsJson = json.encodeToString(commandArgs),
            environmentVarsJson = json.encodeToString(environmentVars),
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Convert database entity to domain model.
     */
    private fun McpServerEntity.toDomainModel(): McpServerConfig {
        return McpServerConfig(
            id = id,
            name = name,
            transportType = McpClientService.McpTransportType.valueOf(transportType),
            serverUrl = serverUrl,
            authType = McpServerConfig.AuthType.valueOf(authType),
            authHeaders = try {
                json.decodeFromString<Map<String, String>>(authHeadersJson)
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse auth headers for server $id, using empty map" }
                emptyMap()
            },
            commandArgs = try {
                json.decodeFromString<List<String>>(commandArgsJson)
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse command args for server $id, using empty list" }
                emptyList()
            },
            environmentVars = try {
                json.decodeFromString<Map<String, String>>(environmentVarsJson)
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse environment vars for server $id, using empty map" }
                emptyMap()
            },
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
