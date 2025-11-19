package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.modelcontextprotocol.kotlin.sdk.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
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
 * - Flexible authentication (Bearer tokens, API keys, custom headers)
 */
class McpClientService(
    private val customLogger: Logger,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pingJob: Job? = null
    private var mcpClient: Client? = null
    private var currentHttpClient: HttpClient? = null
    private var currentProcess: McpProcess? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _availableTools = MutableStateFlow<List<ToolInfo>>(emptyList())
    val availableTools: StateFlow<List<ToolInfo>> = _availableTools.asStateFlow()

    private val _availableResources = MutableStateFlow<List<String>>(emptyList())
    val availableResources: StateFlow<List<String>> = _availableResources.asStateFlow()

    /**
     * Transport type for MCP connection
     */
    enum class McpTransportType {
        /** Server-Sent Events over HTTP - for remote servers */
        SSE,

        /** Standard Input/Output - for local process communication */
        STDIO
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
    suspend fun connect(config: McpConnectionConfig): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.Connecting

            customLogger.i { "Creating MCP client for: ${config.serverUrl} (${config.transportType})" }

            // Create MCP client
            val client = Client(
                clientInfo = Implementation(
                    name = config.clientName,
                    version = config.clientVersion
                )
            )

            // Create transport based on type
            val transport = when (config.transportType) {
                McpTransportType.SSE -> createSseTransport(config)
                McpTransportType.STDIO -> createStdioTransport(config)
            }

            // Connect client to transport
            customLogger.d { "Connecting to ${config.transportType} transport..." }
            client.connect(transport)

            mcpClient = client
            _connectionState.value = ConnectionState.Connected(config.serverUrl, config.transportType)

            // Fetch available tools and resources
            refreshCapabilities()
            pingJob = scope.launch {
                while (isActive) {
                    delay(5000)
                    client.ping()
                }
            }

            customLogger.i { "Successfully connected to MCP server: ${config.serverUrl}" }
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown connection error")
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
        currentHttpClient = httpClient

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

        return@run transport
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

            // Store process reference for cleanup
            currentProcess = process

            customLogger.i { "Process spawned successfully, creating StdIO transport" }

            StdioClientTransport(
                input = process.inputStream,
                output = process.outputStream
            )
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to create StdIO transport" }
            throw e
        }
    }

    /**
     * Disconnect from the current MCP server
     */
    suspend fun disconnect() {
        try {
            pingJob?.cancel()
            customLogger.i { "Disconnecting from MCP server..." }

            // Destroy process if it exists (for STDIO transport)
            currentProcess?.let { process ->
                try {
                    customLogger.d { "Terminating MCP process..." }
                    process.destroy()
                    customLogger.i { "MCP process terminated" }
                } catch (e: Exception) {
                    customLogger.w(e) { "Error while terminating process" }
                }
            }
            currentProcess = null

            // Close HTTP client if it exists (for SSE transport)
            currentHttpClient?.close()
            currentHttpClient = null

            // Clear MCP client
            mcpClient = null
            _connectionState.value = ConnectionState.Disconnected
            _availableTools.value = emptyList()
            _availableResources.value = emptyList()

            customLogger.i { "Disconnected from MCP server" }
        } catch (e: Exception) {
            customLogger.e(e) { "Error during disconnect" }
        }
    }

    /**
     * Call a tool on the connected MCP server
     *
     * @param name Tool name
     * @param arguments Tool arguments as a map
     * @return Result containing the tool call result as text
     */
    suspend fun callTool(
        name: String,
        arguments: Map<String, Any> = emptyMap(),
    ): Result<String> {
        val client = mcpClient
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Calling tool: $name with args: $arguments" }

            // Call tool with arguments
            val result = client.callTool(
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
    suspend fun readResource(uri: String): Result<String> {
        val client = mcpClient
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Reading resource: $uri" }

            // Call readResource with ReadResourceRequest
            val result = client.readResource(ReadResourceRequest(uri = uri))

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
    suspend fun listTools(): Result<List<ToolInfo>> {
        val client = mcpClient
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Listing available tools..." }

            // Call listTools - returns ListToolsResult
            val result = client.listTools()
            customLogger.d { result.tools.joinToString(separator = "\n")}

            // Extract tool information
            val tools = result.tools.map { tool ->
                ToolInfo(
                    name = tool.name,
                    title = tool.title ?: "No title",
                    description = tool.description ?: "No description",
                    inputSchema = tool.inputSchema.properties.toString()
                )
            }

            _availableTools.value = tools
            customLogger.i { "Found ${tools.size} tools: ${tools.map { it.name }}" }
            Result.success(tools)
        } catch (e: Exception) {
            customLogger.e(e) { "Failed to list tools" }
            Result.failure(e)
        }
    }

    /**
     * List all available resources from the connected server
     */
    suspend fun listResources(): Result<List<String>> {
        val client = mcpClient
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Listing available resources..." }

            // Call listResources - returns ListResourcesResult
            val result = client.listResources()

            // Extract resource URIs
            val resourceUris = result.resources.map { it.uri }

            _availableResources.value = resourceUris
            customLogger.i { "Found ${resourceUris.size} resources: $resourceUris" }
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
    ): Result<String> {
        val client = mcpClient
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Getting prompt: $name with args: $arguments" }

            // Call getPrompt with GetPromptRequest
            val result = client.getPrompt(
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
    suspend fun listPrompts(): Result<List<String>> {
        val client = mcpClient
            ?: return Result.failure(IllegalStateException("Not connected to MCP server"))

        return try {
            customLogger.d { "Listing available prompts..." }

            // Call listPrompts - returns ListPromptsResult
            val result = client.listPrompts()

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
    private suspend fun refreshCapabilities() {
        try {
            listTools()
            listResources()
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

    /**
     * Tool information data class
     */
    data class ToolInfo(
        val name: String,
        val title: String,
        val description: String,
        val inputSchema: String,
    )
}
