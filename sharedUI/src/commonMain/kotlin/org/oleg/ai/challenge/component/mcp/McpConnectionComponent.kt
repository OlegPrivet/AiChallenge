package org.oleg.ai.challenge.component.mcp

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.McpServerConfig
import org.oleg.ai.challenge.data.network.service.McpClientService

/**
 * Component for managing MCP server connections.
 *
 * Handles:
 * - Saving and loading server configurations
 * - Connecting/disconnecting from MCP servers
 * - Listing and invoking tools
 * - Managing authentication and transport types
 */
interface McpConnectionComponent {

    /**
     * Current component state.
     */
    val state: Value<State>

    /**
     * Connection state from MCP client.
     */
    val connectionState: Value<McpClientService.ConnectionState>

    /**
     * Available tools from connected server.
     */
    val availableTools: Value<List<McpClientService.ToolInfo>>

    // Configuration Events

    /**
     * Called when server name is changed.
     */
    fun onServerNameChanged(name: String)

    /**
     * Called when transport type is changed (SSE/STDIO).
     */
    fun onTransportTypeChanged(type: McpClientService.McpTransportType)

    /**
     * Called when server URL is changed.
     */
    fun onServerUrlChanged(url: String)

    /**
     * Called when authentication type is changed.
     */
    fun onAuthTypeChanged(authType: McpServerConfig.AuthType)

    /**
     * Called when auth token/key is changed.
     */
    fun onAuthValueChanged(value: String)

    /**
     * Called when API key header name is changed.
     */
    fun onApiKeyHeaderChanged(headerName: String)

    /**
     * Called when custom headers are changed.
     */
    fun onCustomHeadersChanged(headers: Map<String, String>)

    /**
     * Called when command args are changed (for STDIO).
     */
    fun onCommandArgsChanged(args: List<String>)

    /**
     * Called when environment variables are changed (for STDIO).
     */
    fun onEnvironmentVarsChanged(env: Map<String, String>)

    // Server Management Events

    /**
     * Select a saved server configuration.
     */
    fun onSelectSavedServer(config: McpServerConfig)

    /**
     * Save the current configuration.
     */
    fun onSaveCurrentConfiguration()

    /**
     * Delete a saved server configuration.
     */
    fun onDeleteServer(id: Long)

    /**
     * Show/hide the add server dialog.
     */
    fun onToggleAddServerDialog(show: Boolean)

    // Connection Events

    /**
     * Test the connection without saving.
     */
    fun onTestConnection()

    /**
     * Connect to the currently configured server.
     */
    fun onConnect()

    /**
     * Disconnect from the current server.
     */
    fun onDisconnect()

    // Tool Events

    /**
     * Select a tool for invocation.
     */
    fun onSelectTool(tool: McpClientService.ToolInfo?)

    /**
     * Called when tool arguments are changed.
     */
    fun onToolArgumentsChanged(args: Map<String, Any>)

    /**
     * Invoke the currently selected tool.
     */
    fun onInvokeTool()

    /**
     * Refresh the list of available tools.
     */
    fun onRefreshTools()

    /**
     * Component state data class.
     */
    data class State(
        // Saved servers from database
        val savedServers: List<McpServerConfig> = emptyList(),

        // Current configuration (may be new or from saved server)
        val currentConfig: McpServerConfig = McpServerConfig(
            name = "New Server",
            transportType = McpClientService.McpTransportType.SSE,
            serverUrl = ""
        ),

        // Selected tool for invocation
        val selectedTool: McpClientService.ToolInfo? = null,

        // Tool arguments for invocation
        val toolArguments: Map<String, Any> = emptyMap(),

        // Tool invocation result
        val toolInvocationResult: String? = null,

        // Loading state
        val isLoading: Boolean = false,

        // Error message
        val errorMessage: String? = null,

        // Show add server dialog
        val showAddServerDialog: Boolean = false,

        // Temporary auth values (not part of config yet)
        val tempAuthValue: String = "",
        val tempApiKeyHeader: String = "X-API-Key"
    )
}
