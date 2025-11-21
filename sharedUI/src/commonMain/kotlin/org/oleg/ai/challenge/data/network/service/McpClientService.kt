package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.modelcontextprotocol.kotlin.sdk.EmptyJsonObject
import io.modelcontextprotocol.kotlin.sdk.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * MCP Client Service for connecting to and communicating with MCP servers.
 *
 * This service provides full implementation of:
 * - SSE (Server-Sent Events) transport connection to remote MCP servers
 * - StdIO transport connection to local MCP server processes
 * - Tool invocation with parameters and results
 * - Resource access and reading
 * - Prompt listing and retrieval
 * - Connection state management
 * - Multiple concurrent MCP clients with active connection selection
 * - Flexible authentication (Bearer tokens, API keys, custom headers)
 */
class McpClientService(
    private val customLogger: Logger,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionsGuard = Mutex()
    private val connections = mutableMapOf<String, ManagedConnection>()

    private val _activeConnectionId = MutableStateFlow<String?>(null)
    val activeConnectionId: StateFlow<String?> = _activeConnectionId.asStateFlow()

    private val _activeConnections = MutableStateFlow<List<ActiveConnection>>(emptyList())
    val activeConnections: StateFlow<List<ActiveConnection>> = _activeConnections.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _availableTools = MutableStateFlow<List<ToolInfo>>(emptyList())
    val availableTools: StateFlow<List<ToolInfo>> = _availableTools.asStateFlow()
    val availableToolsAsList: List<ToolInfo>?
        get() = _availableTools.value.ifEmpty { null }

    private val _availableResources = MutableStateFlow<List<String>>(emptyList())
    val availableResources: StateFlow<List<String>> = _availableResources.asStateFlow()

    data class ActiveConnection(
        val id: String,
        val serverId: Long? = null,
        val name: String,
        val url: String,
        val transportType: McpTransportType,
        val state: ConnectionState,
        val toolCount: Int,
        val resourceCount: Int,
    )

    private data class ManagedConnection(
        val id: String,
        val serverId: Long?,
        val name: String,
        val config: McpConnectionConfig,
        val client: Client,
        var httpClient: HttpClient? = null,
        var process: McpProcess? = null,
        var pingJob: Job? = null,
        val tools: MutableStateFlow<List<ToolInfo>> = MutableStateFlow(emptyList()),
        val resources: MutableStateFlow<List<String>> = MutableStateFlow(emptyList()),
        val state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected),
    )

    private data class TransportContext(
        val transport: AbstractTransport,
        val httpClient: HttpClient? = null,
        val process: McpProcess? = null,
    )

    private fun defaultConnectionId(config: McpConnectionConfig, serverId: Long?): String {
        return serverId?.takeIf { it > 0 }?.toString() ?: config.serverUrl
    }

    /**
     * Transport type for MCP connection
     */
    enum class McpTransportType {
        /** Server-Sent Events over HTTP - for remote servers */
        SSE,

        /** Server-Sent Events over HTTP - for remote servers */
        HTTP,

        /** Standard Input/Output - for local process communication */
        STDIO,
    }

    /**
     * Configuration for MCP server connection
     *
     * @property transportType Type of transport to use (SSE or StdIO)
     * @property serverUrl URL for SSE transport or command path for StdIO transport
     * @property headers HTTP headers for SSE transport (e.g., Authorization, API keys)
     * @property queryParams URL query parameters for SSE transport
     * @property commandArgs Command line arguments for StdIO transport
     * @property environmentVars Environment variables for StdIO transport process
     * @property clientName Name of this client application
     * @property clientVersion Version of this client application
     */
    data class McpConnectionConfig(
        val transportType: McpTransportType,
        val serverUrl: String,
        val headers: Map<String, String> = emptyMap(),
        val queryParams: Map<String, String> = emptyMap(),
        val commandArgs: List<String> = emptyList(),
        val environmentVars: Map<String, String> = emptyMap(),
        val clientName: String = "AiChallenge-MCP-Client",
        val clientVersion: String = "1.0.0",
    ) {
        companion object {
            /**
             * Create SSE connection with Bearer token authentication
             */
            fun withBearerToken(
                serverUrl: String,
                token: String,
                additionalHeaders: Map<String, String> = emptyMap(),
                queryParams: Map<String, String> = emptyMap(),
            ): McpConnectionConfig {
                return McpConnectionConfig(
                    transportType = McpTransportType.SSE,
                    serverUrl = serverUrl,
                    headers = additionalHeaders + ("Authorization" to "Bearer $token"),
                    queryParams = queryParams
                )
            }

            /**
             * Create SSE connection with API key authentication
             *
             * @param headerName Name of the API key header (e.g., "X-API-Key", "API-Key")
             */
            fun withApiKey(
                serverUrl: String,
                apiKey: String,
                headerName: String = "X-API-Key",
                additionalHeaders: Map<String, String> = emptyMap(),
                queryParams: Map<String, String> = emptyMap(),
            ): McpConnectionConfig {
                return McpConnectionConfig(
                    transportType = McpTransportType.SSE,
                    serverUrl = serverUrl,
                    headers = additionalHeaders + (headerName to apiKey),
                    queryParams = queryParams
                )
            }

            /**
             * Create SSE connection with API key in query parameter
             */
            fun withApiKeyParam(
                serverUrl: String,
                apiKey: String,
                paramName: String = "api_key",
                headers: Map<String, String> = emptyMap(),
                additionalParams: Map<String, String> = emptyMap(),
            ): McpConnectionConfig {
                return McpConnectionConfig(
                    transportType = McpTransportType.SSE,
                    serverUrl = serverUrl,
                    headers = headers,
                    queryParams = additionalParams + (paramName to apiKey)
                )
            }

            /**
             * Create SSE connection with custom headers
             */
            fun withCustomHeaders(
                serverUrl: String,
                headers: Map<String, String>,
                queryParams: Map<String, String> = emptyMap(),
            ): McpConnectionConfig {
                return McpConnectionConfig(
                    transportType = McpTransportType.SSE,
                    serverUrl = serverUrl,
                    headers = headers,
                    queryParams = queryParams
                )
            }

            /**
             * Create StdIO connection for local MCP server process
             *
             * @param command Path to the executable or command
             * @param args Command line arguments
             * @param env Environment variables for the process
             */
            fun forStdio(
                command: String,
                args: List<String> = emptyList(),
                env: Map<String, String> = emptyMap(),
            ): McpConnectionConfig {
                return McpConnectionConfig(
                    transportType = McpTransportType.STDIO,
                    serverUrl = command,
                    commandArgs = args,
                    environmentVars = env
                )
            }

            /**
             * Create basic SSE connection without authentication
             */
            fun basic(serverUrl: String): McpConnectionConfig {
                return McpConnectionConfig(
                    transportType = McpTransportType.SSE,
                    serverUrl = serverUrl
                )
            }
        }
    }

    /**
     * Connect to an MCP server with advanced configuration
     *
     * @param config Connection configuration with transport type, auth, etc.
     */
    suspend fun connect(
        config: McpConnectionConfig,
        serverId: Long? = null,
        connectionId: String = defaultConnectionId(config, serverId),
        displayName: String? = null,
    ): Result<Unit> {
        val resolvedId = connectionId.ifBlank { defaultConnectionId(config, serverId) }
        val connectionName = displayName?.ifBlank { null } ?: config.serverUrl

        return try {
            customLogger.i { "Creating MCP client for: ${config.serverUrl} (${config.transportType}) as $resolvedId" }

            val client = Client(
                clientInfo = Implementation(
                    name = config.clientName,
                    version = config.clientVersion
                )
            )

            connectionsGuard.withLock {
                connections.remove(resolvedId)?.let { cleanupConnection(it) }
                connections[resolvedId] = ManagedConnection(
                    id = resolvedId,
                    serverId = serverId,
                    name = connectionName,
                    config = config,
                    client = client
                ).also { managed ->
                    managed.state.value = ConnectionState.Connecting
                }
            }
            publishConnectionChanges()

            val transportContext = createTransport(config)

            customLogger.d { "Connecting to ${config.transportType} transport..." }
            client.connect(transportContext.transport)

            connectionsGuard.withLock {
                connections[resolvedId]?.let { managed ->
                    managed.httpClient = transportContext.httpClient
                    managed.process = transportContext.process
                    managed.state.value = ConnectionState.Connected(config.serverUrl, config.transportType)
                    managed.pingJob = scope.launch {
                        while (isActive) {
                            delay(7000)
                            managed.client.ping()
                        }
                    }
                }
            }

            setActiveConnection(resolvedId)
            refreshCapabilities(resolvedId)

            customLogger.i { "Successfully connected to MCP server: ${config.serverUrl}" }
            Result.success(Unit)
        } catch (e: Exception) {
            connectionsGuard.withLock {
                connections[resolvedId]?.state?.value = ConnectionState.Error(e.message ?: "Unknown connection error")
            }
            publishConnectionChanges()
            customLogger.e(e) { "Failed to connect to MCP server" }
            Result.failure(e)
        }
    }

    /**
     * Connect to an MCP server via SSE (simple version for backward compatibility)
     *
     * @param serverUrl SSE endpoint URL
     * @param clientName Name of this client application
     * @param clientVersion Version of this client
     */
    suspend fun connect(
        serverUrl: String,
        clientName: String = "AiChallenge-MCP-Client",
        clientVersion: String = "1.0.0",
    ): Result<Unit> {
        return connect(
            McpConnectionConfig(
                transportType = McpTransportType.SSE,
                serverUrl = serverUrl,
                clientName = clientName,
                clientVersion = clientVersion
            )
        )
    }

    /**
     * Connect to an MCP server via SSE with Bearer token
     */
    suspend fun connectWithBearerToken(
        serverUrl: String,
        token: String,
        additionalHeaders: Map<String, String> = emptyMap(),
    ): Result<Unit> {
        return connect(
            McpConnectionConfig.withBearerToken(
                serverUrl = serverUrl,
                token = token,
                additionalHeaders = additionalHeaders
            )
        )
    }

    /**
     * Connect to an MCP server via SSE with API key
     */
    suspend fun connectWithApiKey(
        serverUrl: String,
        apiKey: String,
        headerName: String = "X-API-Key",
        additionalHeaders: Map<String, String> = emptyMap(),
    ): Result<Unit> {
        return connect(
            McpConnectionConfig.withApiKey(
                serverUrl = serverUrl,
                apiKey = apiKey,
                headerName = headerName,
                additionalHeaders = additionalHeaders
            )
        )
    }

    /**
     * Connect to a local MCP server via StdIO
     *
     * Note: StdIO transport requires platform-specific process spawning
     * and may not be available on all platforms (primarily JVM/Desktop).
     */
    suspend fun connectStdio(
        command: String,
        args: List<String> = emptyList(),
        env: Map<String, String> = emptyMap(),
    ): Result<Unit> {
        return connect(
            McpConnectionConfig.forStdio(
                command = command,
                args = args,
                env = env
            )
        )
    }

    private suspend fun createTransport(config: McpConnectionConfig): TransportContext {
        return when (config.transportType) {
            McpTransportType.SSE -> createSseTransport(config)
            McpTransportType.HTTP -> createStreamableHttpTransport(config)
            McpTransportType.STDIO -> createStdioTransport(config)
        }
    }

    /**
     * Create SSE transport with configured headers and query parameters
     */
    private suspend fun createSseTransport(config: McpConnectionConfig) = run {
        // Build URL with query parameters if provided
        val urlWithParams = if (config.queryParams.isNotEmpty()) {
            URLBuilder(config.serverUrl).apply {
                parameters.apply {
                    config.queryParams.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }.buildString()
        } else {
            config.serverUrl
        }

        customLogger.d {
            "Creating SSE transport for: $urlWithParams" +
                    if (config.headers.isNotEmpty()) " with ${config.headers.size} headers" else ""
        }

        // Create HTTP client with SSE support
        val httpClient = HttpClient {
            install(SSE)
            install(Logging) {
                logger = io.ktor.client.plugins.logging.Logger.DEFAULT
                level = LogLevel.ALL
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        customLogger.d(message)
                    }
                }
            }
        }

        // Create SSE transport with headers
        val transport = SseClientTransport(httpClient, urlString = urlWithParams) {
            // Add all configured headers
            method = HttpMethod.Get
            config.headers.forEach { (key, value) ->
                headers.append(key, value)
            }
            headers.append("Content-Type", "application/json")
            headers.append("User-Agent", "MCP-Client/1.0")
            headers.append("Accept", "text/event-stream, application/json")
        }

        return@run TransportContext(transport = transport, httpClient = httpClient)
    }

    /**
     * Create StreamableHttp transport with configured headers and query parameters
     */
    private suspend fun createStreamableHttpTransport(config: McpConnectionConfig) = run {
        // Build URL with query parameters if provided
        val urlWithParams = if (config.queryParams.isNotEmpty()) {
            URLBuilder(config.serverUrl).apply {
                parameters.apply {
                    config.queryParams.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }.buildString()
        } else {
            config.serverUrl
        }

        customLogger.d {
            "Creating StreamableHttp transport for: $urlWithParams" +
                    if (config.headers.isNotEmpty()) " with ${config.headers.size} headers" else ""
        }

        // Create HTTP client with SSE support
        val httpClient = HttpClient {
            install(SSE)
            install(Logging) {
                logger = io.ktor.client.plugins.logging.Logger.DEFAULT
                level = LogLevel.ALL
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        customLogger.d(message)
                    }
                }
            }
        }

        // Create SSE transport with headers
        val transport = StreamableHttpClientTransport(httpClient, url = urlWithParams) {
            // Add all configured headers
            method = HttpMethod.Get
            config.headers.forEach { (key, value) ->
                headers.append(key, value)
            }
            headers.append("Content-Type", "application/json")
            headers.append("User-Agent", "MCP-Client/1.0")
            headers.append("Accept", "text/event-stream, application/json")
        }

        return@run TransportContext(transport = transport, httpClient = httpClient)
    }

    /**
     * Create StdIO transport by spawning a local process
     *
     * Platform Support:
     * - **JVM/Desktop**: Fully supported using ProcessBuilder
     * - **Android**: Supported with security restrictions (see platform-specific notes)
     * - **iOS**: Not supported (throws UnsupportedOperationException)
     *
     * @throws UnsupportedOperationException on iOS platform
     * @throws Exception if process creation fails
     */
    private suspend fun createStdioTransport(config: McpConnectionConfig) = run {
        customLogger.d { "Creating StdIO transport for command: ${config.serverUrl} ${config.commandArgs.joinToString(" ")}" }

        try {
            // Spawn process with command, args, and environment variables
            val process = spawnMcpProcess(
                command = config.serverUrl,
                args = config.commandArgs,
                env = config.environmentVars
            )

            customLogger.i { "Process spawned successfully, creating StdIO transport" }

            TransportContext(
                transport = StdioClientTransport(
                    input = process.inputStream,
                    output = process.outputStream
                ),
                process = process
            )
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to create StdIO transport" }
            throw e
        }
    }

    /**
     * Disconnect from the current MCP server
     */
    suspend fun disconnect(connectionId: String? = _activeConnectionId.value) {
        val id = connectionId ?: return
        val connection = connectionsGuard.withLock { connections.remove(id) } ?: return

        try {
            connection.pingJob?.cancel()
            customLogger.i { "Disconnecting from MCP server $id..." }

            connection.process?.let { process ->
                try {
                    customLogger.d { "Terminating MCP process for $id..." }
                    process.destroy()
                    customLogger.i { "MCP process for $id terminated" }
                } catch (e: Exception) {
                    customLogger.w(e) { "Error while terminating process for $id" }
                }
            }

            connection.httpClient?.close()
            connection.state.value = ConnectionState.Disconnected

            if (_activeConnectionId.value == id) {
                _activeConnectionId.value = connections.keys.firstOrNull()
            }

            customLogger.i { "Disconnected from MCP server $id" }
        } catch (e: Exception) {
            customLogger.e(e) { "Error during disconnect for $id" }
        } finally {
            publishConnectionChanges()
        }
    }

    /**
    * Disconnect all active MCP servers.
    */
    suspend fun disconnectAll() {
        val ids = connectionsGuard.withLock { connections.keys.toList() }
        ids.forEach { disconnect(it) }
    }

    private fun cleanupConnection(connection: ManagedConnection) {
        try {
            connection.pingJob?.cancel()
        } catch (e: Exception) {
            customLogger.w(e) { "Failed to cancel ping job for ${connection.id}" }
        }

        try {
            connection.process?.destroy()
        } catch (e: Exception) {
            customLogger.w(e) { "Failed to destroy process for ${connection.id}" }
        }

        try {
            connection.httpClient?.close()
        } catch (e: Exception) {
            customLogger.w(e) { "Failed to close HTTP client for ${connection.id}" }
        }
    }

    fun setActiveConnection(connectionId: String?) {
        _activeConnectionId.value = connectionId
        publishConnectionChanges()
    }

    private fun publishConnectionChanges() {
        val snapshots = connections.values.map { connection ->
            ActiveConnection(
                id = connection.id,
                serverId = connection.serverId,
                name = connection.name,
                url = connection.config.serverUrl,
                transportType = connection.config.transportType,
                state = connection.state.value,
                toolCount = connection.tools.value.size,
                resourceCount = connection.resources.value.size
            )
        }.sortedBy { it.name.lowercase() }

        _activeConnections.value = snapshots

        val resolvedActiveId = _activeConnectionId.value?.takeIf { id -> connections.containsKey(id) }
            ?: snapshots.firstOrNull()?.id
        _activeConnectionId.value = resolvedActiveId

        val activeConnection = resolvedActiveId?.let { connections[it] }
        _connectionState.value = activeConnection?.state?.value ?: ConnectionState.Disconnected
        _availableTools.value = connections.values.flatMap { it.tools.value }
        _availableResources.value = activeConnection?.resources?.value ?: emptyList()
    }

    /**
     * Call a tool on the connected MCP server
     *
     * @param name Tool name
     * @param arguments Tool arguments as a map
     * @return Result containing the tool call result as text
     */
    private fun resolveConnection(connectionId: String?): ManagedConnection? {
        val targetId = connectionId ?: findConnectionIdForTool(null)
        return targetId?.let { id ->
            val connection = connections[id]
            if (connection == null) {
                customLogger.w { "Connection $id not found" }
            }
            connection
        } ?: run {
            customLogger.w { "No active MCP connection available" }
            null
        }
    }

    private fun findConnectionIdForTool(toolName: String?): String? {
        if (toolName == null) {
            return _activeConnectionId.value ?: connections.keys.firstOrNull()
        }
        return connections.values.firstOrNull { managed ->
            managed.tools.value.any { it.name == toolName }
        }?.id ?: _activeConnectionId.value ?: connections.keys.firstOrNull()
    }

    suspend fun callTool(
        name: String,
        arguments: Map<String, Any> = emptyMap(),
        connectionId: String? = _activeConnectionId.value,
    ): Result<String> {
        val connection = resolveConnection(connectionId ?: findConnectionIdForTool(name))
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Calling tool: $name with args: $arguments" }

            // Call tool with arguments
            val result = connection.client.callTool(
                name = name,
                arguments = arguments
            )

            // Extract text content from result
            val textContent = result?.content?.joinToString("\n") { content ->
                content.toString()
            } ?: "Tool executed successfully (no content)"

            customLogger.i { "Tool $name executed. Result: ${textContent.take(100)}..." }

            // Check if the result indicates an error
            val isError = result?.isError == true
            if (isError) {
                Result.failure(Exception("Tool error: $textContent"))
            } else {
                Result.success(textContent)
            }
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to call tool: $name" }
            Result.failure(e)
        }
    }

    /**
     * Read a resource from the connected MCP server
     *
     * @param uri Resource URI
     * @return Result containing the resource content as text
     */
    suspend fun readResource(
        uri: String,
        connectionId: String? = _activeConnectionId.value,
    ): Result<String> {
        val connection = resolveConnection(connectionId)
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Reading resource: $uri" }

            // Call readResource with ReadResourceRequest
            val result = connection.client.readResource(ReadResourceRequest(uri = uri))

            // Extract text content from result
            val textContent = result.contents.joinToString("\n") { content ->
                content.toString()
            }.ifEmpty { "Resource has no text content" }

            customLogger.i { "Resource $uri read successfully. Size: ${textContent.length} chars" }
            Result.success(textContent)
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to read resource: $uri" }
            Result.failure(e)
        }
    }

    /**
     * List all available tools from the connected server
     */
    suspend fun listTools(connectionId: String? = _activeConnectionId.value): Result<List<ToolInfo>> {
        val connection = resolveConnection(connectionId)
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Listing available tools..." }

            // Call listTools - returns ListToolsResult
            val result = connection.client.listTools()
            customLogger.d { result.tools.joinToString(separator = "\n") }

            // Extract tool information
            val tools = result.tools.map { tool ->
                ToolInfo(
                    name = tool.name,
                    title = tool.title,
                    description = tool.description,
                    parameters = Input(tool.inputSchema.properties, tool.inputSchema.required),
                    outputSchema = Output(
                        tool.outputSchema?.properties ?: EmptyJsonObject,
                        tool.outputSchema?.required
                    ),
                    annotations = ToolAnnotations(
                        title = tool.annotations?.title,
                        readOnlyHint = tool.annotations?.readOnlyHint,
                        destructiveHint = tool.annotations?.destructiveHint,
                        idempotentHint = tool.annotations?.idempotentHint,
                        openWorldHint = tool.annotations?.openWorldHint
                    ),
                    _meta = tool._meta,
                    connectionId = connection.id,
                    connectionName = connection.name
                )
            }

            connection.tools.value = tools
            publishConnectionChanges()
            customLogger.i { "Found ${tools.size} tools on ${connection.id}: ${tools.map { it.name }}" }
            Result.success(tools)
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to list tools" }
            Result.failure(e)
        }
    }

    /**
     * List all available resources from the connected server
     */
    suspend fun listResources(connectionId: String? = _activeConnectionId.value): Result<List<String>> {
        val connection = resolveConnection(connectionId)
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Listing available resources..." }

            // Call listResources - returns ListResourcesResult
            val result = connection.client.listResources()

            // Extract resource URIs
            val resourceUris = result.resources.map { it.uri }

            connection.resources.value = resourceUris
            if (connection.id == _activeConnectionId.value) {
                _availableResources.value = resourceUris
            }
            publishConnectionChanges()
            customLogger.i { "Found ${resourceUris.size} resources for ${connection.id}: $resourceUris" }
            Result.success(resourceUris)
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to list resources" }
            Result.failure(e)
        }
    }

    /**
     * Get a prompt from the connected server
     *
     * @param name Prompt name
     * @param arguments Optional prompt arguments
     */
    suspend fun getPrompt(
        name: String,
        arguments: Map<String, String> = emptyMap(),
        connectionId: String? = _activeConnectionId.value,
    ): Result<String> {
        val connection = resolveConnection(connectionId)
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Getting prompt: $name with args: $arguments" }

            // Call getPrompt with GetPromptRequest
            val result = connection.client.getPrompt(
                GetPromptRequest(name = name, arguments = arguments)
            )

            // Extract messages from result
            val messages = result.messages.joinToString("\n") { message ->
                message.toString()
            }

            customLogger.i { "Prompt $name retrieved successfully" }
            Result.success(messages)
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to get prompt: $name" }
            Result.failure(e)
        }
    }

    /**
     * List all available prompts from the connected server
     */
    suspend fun listPrompts(connectionId: String? = _activeConnectionId.value): Result<List<String>> {
        val connection = resolveConnection(connectionId)
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Listing available prompts..." }

            // Call listPrompts - returns ListPromptsResult
            val result = connection.client.listPrompts()

            // Extract prompt names
            val promptNames = result.prompts.map { it.name }

            customLogger.i { "Found ${promptNames.size} prompts: $promptNames" }
            Result.success(promptNames)
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to list prompts" }
            Result.failure(e)
        }
    }

    /**
     * Refresh available tools and resources from the server
     */
    private suspend fun refreshCapabilities(connectionId: String? = _activeConnectionId.value) {
        try {
            listTools(connectionId)
            listResources(connectionId)
        } catch (e: Exception) {
            customLogger.w(e) { "Failed to refresh capabilities" }
        }
    }

    /**
     * Connection state sealed class
     */
    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data class Connected(val url: String, val transportType: McpTransportType) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    @Serializable
    data class ToolInfo(
        val name: String,
        val title: String?,
        val description: String?,
        val parameters: Input,
        val outputSchema: Output?,
        val annotations: ToolAnnotations?,
        val _meta: JsonObject = EmptyJsonObject,
        val connectionId: String? = null,
        val connectionName: String? = null,
    )

    @Serializable
    data class Input(
        val properties: JsonObject = EmptyJsonObject,
        val required: List<String>? = null,
        val type: String = "object",
    )

    @Serializable
    data class Output(
        val properties: JsonObject = EmptyJsonObject,
        val required: List<String>? = null,
        val type: String = "object",
    )

    @Serializable
    data class ToolAnnotations(
        val title: String?,
        val readOnlyHint: Boolean? = false,
        val destructiveHint: Boolean? = true,
        val idempotentHint: Boolean? = false,
        val openWorldHint: Boolean? = true,
    )
}
