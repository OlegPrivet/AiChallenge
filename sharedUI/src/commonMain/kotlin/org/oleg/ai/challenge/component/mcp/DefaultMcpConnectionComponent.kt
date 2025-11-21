package org.oleg.ai.challenge.component.mcp

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.oleg.ai.challenge.data.model.McpServerConfig
import org.oleg.ai.challenge.data.network.service.McpClientService
import org.oleg.ai.challenge.data.repository.McpServerRepository

/**
 * Default implementation of McpConnectionComponent.
 */
class DefaultMcpConnectionComponent(
    componentContext: ComponentContext,
    private val mcpClientService: McpClientService,
    private val mcpServerRepository: McpServerRepository,
) : McpConnectionComponent, ComponentContext by componentContext {

    private val logger = Logger.withTag("DefaultMcpConnectionComponent")
    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _state = MutableValue(McpConnectionComponent.State())
    override val state: Value<McpConnectionComponent.State> = _state

    // Convert StateFlow to Value
    override val connectionState: Value<McpClientService.ConnectionState> =
        mcpClientService.connectionState.asValue(this)

    override val availableTools: Value<List<McpClientService.ToolInfo>> =
        mcpClientService.availableTools.asValue(this)

    init {
        // Load saved servers on init
        scope.launch {
            mcpServerRepository.savedServers.collect { servers ->
                _state.value = _state.value.copy(savedServers = servers)
            }
        }
        scope.launch {
            mcpClientService.activeConnections.collect { connections ->
                _state.value = _state.value.copy(activeConnections = connections)
            }
        }
        scope.launch {
            mcpClientService.activeConnectionId.collect { activeId ->
                _state.value = _state.value.copy(activeConnectionId = activeId)
            }
        }
    }

    // Configuration Events

    override fun onServerNameChanged(name: String) {
        _state.value = _state.value.copy(
            currentConfig = _state.value.currentConfig.copy(name = name)
        )
    }

    override fun onTransportTypeChanged(type: McpClientService.McpTransportType) {
        _state.value = _state.value.copy(
            currentConfig = _state.value.currentConfig.copy(transportType = type)
        )
    }

    override fun onServerUrlChanged(url: String) {
        _state.value = _state.value.copy(
            currentConfig = _state.value.currentConfig.copy(serverUrl = url)
        )
    }

    override fun onAuthTypeChanged(authType: McpServerConfig.AuthType) {
        _state.value = _state.value.copy(
            currentConfig = _state.value.currentConfig.copy(
                authType = authType,
                authHeaders = emptyMap() // Reset headers when auth type changes
            ),
            tempAuthValue = "" // Reset temp value
        )
    }

    override fun onAuthValueChanged(value: String) {
        _state.value = _state.value.copy(tempAuthValue = value)
        updateAuthHeaders()
    }

    override fun onApiKeyHeaderChanged(headerName: String) {
        _state.value = _state.value.copy(tempApiKeyHeader = headerName)
        updateAuthHeaders()
    }

    override fun onCustomHeadersChanged(headers: Map<String, String>) {
        _state.value = _state.value.copy(
            currentConfig = _state.value.currentConfig.copy(authHeaders = headers)
        )
    }

    override fun onCommandArgsChanged(args: List<String>) {
        _state.value = _state.value.copy(
            currentConfig = _state.value.currentConfig.copy(commandArgs = args)
        )
    }

    override fun onEnvironmentVarsChanged(env: Map<String, String>) {
        _state.value = _state.value.copy(
            currentConfig = _state.value.currentConfig.copy(environmentVars = env)
        )
    }

    // Server Management Events

    override fun onSelectSavedServer(config: McpServerConfig) {
        _state.value = _state.value.copy(
            currentConfig = config,
            tempAuthValue = extractAuthValue(config),
            tempApiKeyHeader = extractApiKeyHeader(config)
        )
        val connectionId = connectionIdFor(config)
        if (_state.value.activeConnections.any { it.id == connectionId }) {
            onSelectActiveConnection(connectionId)
        }
    }

    override fun onSaveCurrentConfiguration() {
        scope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)

                val config = _state.value.currentConfig
                if (config.name.isBlank()) {
                    _state.value = _state.value.copy(
                        errorMessage = "Server name cannot be empty",
                        isLoading = false
                    )
                    return@launch
                }

                if (config.serverUrl.isBlank()) {
                    _state.value = _state.value.copy(
                        errorMessage = "Server URL cannot be empty",
                        isLoading = false
                    )
                    return@launch
                }

                val id = mcpServerRepository.saveServer(config)
                logger.i { "Saved server configuration: ${config.name} (id=$id)" }

                _state.value = _state.value.copy(
                    currentConfig = config.copy(id = id),
                    showAddServerDialog = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                logger.e(e) { "Failed to save server configuration" }
                _state.value = _state.value.copy(
                    errorMessage = "Failed to save: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    override fun onDeleteServer(id: Long) {
        scope.launch {
            try {
                mcpServerRepository.deleteServer(id)
                logger.i { "Deleted server configuration: id=$id" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to delete server configuration" }
                _state.value = _state.value.copy(
                    errorMessage = "Failed to delete: ${e.message}"
                )
            }
        }
    }

    override fun onToggleAddServerDialog(show: Boolean) {
        _state.value = _state.value.copy(
            showAddServerDialog = show,
            currentConfig = if (show) {
                // Reset to new config when opening dialog
                McpServerConfig(
                    name = "New Server",
                    transportType = McpClientService.McpTransportType.SSE,
                    serverUrl = ""
                )
            } else {
                _state.value.currentConfig
            },
            tempAuthValue = "",
            tempApiKeyHeader = "X-API-Key"
        )
    }

    // Connection Events

    override fun onTestConnection() {
        scope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)

                val config = _state.value.currentConfig
                val testConnectionId = "${connectionIdFor(config)}-test"
                val result = mcpClientService.connect(
                    config = config.toConnectionConfig(),
                    serverId = config.id.takeIf { it > 0 },
                    connectionId = testConnectionId,
                    displayName = config.name
                )

                if (result.isSuccess) {
                    logger.i { "Test connection successful" }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    // Disconnect after successful test
                    mcpClientService.disconnect(testConnectionId)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    logger.e { "Test connection failed: $error" }
                    _state.value = _state.value.copy(
                        errorMessage = "Connection failed: $error",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                logger.e(e) { "Test connection error" }
                _state.value = _state.value.copy(
                    errorMessage = "Connection error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    override fun onConnect() {
        scope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)

                val config = _state.value.currentConfig
                val connectionId = connectionIdFor(config)
                val result = mcpClientService.connect(
                    config = config.toConnectionConfig(),
                    serverId = config.id.takeIf { it > 0 },
                    connectionId = connectionId,
                    displayName = config.name
                )

                if (result.isSuccess) {
                    logger.i { "Connected to MCP server: ${config.name}" }
                    mcpClientService.setActiveConnection(connectionId)
                    _state.value = _state.value.copy(activeConnectionId = connectionId)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    logger.e { "Connection failed: $error" }
                    _state.value = _state.value.copy(errorMessage = "Connection failed: $error")
                }
            } catch (e: Exception) {
                logger.e(e) { "Connection error" }
                _state.value = _state.value.copy(errorMessage = "Connection error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    override fun onDisconnect() {
        scope.launch {
            try {
                val activeId = _state.value.activeConnectionId ?: return@launch
                mcpClientService.disconnect(activeId)
                _state.value = _state.value.copy(
                    selectedTool = null,
                    toolInvocationResult = null
                )
                logger.i { "Disconnected from MCP server $activeId" }
            } catch (e: Exception) {
                logger.e(e) { "Disconnect error" }
            }
        }
    }

    override fun onDisconnectConnection(connectionId: String) {
        scope.launch {
            try {
                mcpClientService.disconnect(connectionId)
                if (_state.value.activeConnectionId == connectionId) {
                    _state.value = _state.value.copy(
                        selectedTool = null,
                        toolInvocationResult = null
                    )
                }
            } catch (e: Exception) {
                logger.e(e) { "Disconnect error" }
            }
        }
    }

    override fun onConnectSavedServer(config: McpServerConfig) {
        scope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
                val connectionId = connectionIdFor(config)
                val result = mcpClientService.connect(
                    config = config.toConnectionConfig(),
                    serverId = config.id.takeIf { it > 0 },
                    connectionId = connectionId,
                    displayName = config.name
                )

                if (result.isSuccess) {
                    mcpClientService.setActiveConnection(connectionId)
                    _state.value = _state.value.copy(activeConnectionId = connectionId)
                    logger.i { "Connected to MCP server: ${config.name}" }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    logger.e { "Connection failed: $error" }
                    _state.value = _state.value.copy(errorMessage = "Connection failed: $error")
                }
            } catch (e: Exception) {
                logger.e(e) { "Connection error" }
                _state.value = _state.value.copy(errorMessage = "Connection error: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    override fun onSelectActiveConnection(connectionId: String) {
        mcpClientService.setActiveConnection(connectionId)
        _state.value = _state.value.copy(
            activeConnectionId = connectionId,
            selectedTool = null,
            toolInvocationResult = null
        )
    }

    // Tool Events

    override fun onSelectTool(tool: McpClientService.ToolInfo?) {
        _state.value = _state.value.copy(
            selectedTool = tool,
            toolArguments = emptyMap(),
            toolInvocationResult = null
        )
    }

    override fun onToolArgumentsChanged(args: Map<String, Any>) {
        _state.value = _state.value.copy(toolArguments = args)
    }

    override fun onInvokeTool() {
        val tool = _state.value.selectedTool ?: return
        val connectionId = tool.connectionId ?: _state.value.activeConnectionId ?: run {
            _state.value = _state.value.copy(
                errorMessage = "Select an active connection before invoking tools"
            )
            return
        }

        scope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)

                val result = mcpClientService.callTool(
                    name = tool.name,
                    arguments = _state.value.toolArguments,
                    connectionId = connectionId
                )

                _state.value = if (result.isSuccess) {
                    _state.value.copy(
                        toolInvocationResult = result.getOrNull(),
                        isLoading = false
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _state.value.copy(
                        errorMessage = "Tool invocation failed: $error",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                logger.e(e) { "Tool invocation error" }
                _state.value = _state.value.copy(
                    errorMessage = "Tool error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    override fun onRefreshTools() {
        scope.launch {
            try {
                mcpClientService.listTools(_state.value.activeConnectionId)
            } catch (e: Exception) {
                logger.e(e) { "Failed to refresh tools" }
            }
        }
    }

    // Private helper methods

    private fun connectionIdFor(config: McpServerConfig): String {
        return if (config.id > 0) {
            config.id.toString()
        } else {
            config.serverUrl.ifBlank { config.name }
        }
    }

    private fun updateAuthHeaders() {
        val currentState = _state.value
        val authType = currentState.currentConfig.authType
        val authValue = currentState.tempAuthValue

        if (authValue.isBlank()) {
            _state.value = currentState.copy(
                currentConfig = currentState.currentConfig.copy(authHeaders = emptyMap())
            )
            return
        }

        val headers = when (authType) {
            McpServerConfig.AuthType.BEARER -> {
                mapOf("Authorization" to "Bearer $authValue")
            }
            McpServerConfig.AuthType.API_KEY -> {
                mapOf(currentState.tempApiKeyHeader to authValue)
            }
            else -> emptyMap()
        }

        _state.value = currentState.copy(
            currentConfig = currentState.currentConfig.copy(authHeaders = headers)
        )
    }

    private fun extractAuthValue(config: McpServerConfig): String {
        return when (config.authType) {
            McpServerConfig.AuthType.BEARER -> {
                config.authHeaders["Authorization"]?.removePrefix("Bearer ")?.trim() ?: ""
            }
            McpServerConfig.AuthType.API_KEY -> {
                config.authHeaders.values.firstOrNull() ?: ""
            }
            else -> ""
        }
    }

    private fun extractApiKeyHeader(config: McpServerConfig): String {
        return if (config.authType == McpServerConfig.AuthType.API_KEY) {
            config.authHeaders.keys.firstOrNull() ?: "X-API-Key"
        } else {
            "X-API-Key"
        }
    }
}

// Extension to convert StateFlow to Value (using ComponentContext)
private fun <T : Any> StateFlow<T>.asValue(componentContext: ComponentContext): Value<T> {
    val mutableValue = MutableValue(value)
    val scope = componentContext.coroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    scope.launch {
        collect { mutableValue.value = it }
    }
    return mutableValue
}
