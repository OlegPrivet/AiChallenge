package org.oleg.ai.challenge.ui.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import org.oleg.ai.challenge.component.mcp.McpConnectionComponent
import org.oleg.ai.challenge.data.model.McpServerConfig
import org.oleg.ai.challenge.data.network.json
import org.oleg.ai.challenge.data.network.service.McpClientService

/**
 * Enhanced MCP Connection Screen with saved servers, configuration, and tool invocation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpConnectionScreen(
    component: McpConnectionComponent,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by component.state.subscribeAsState()
    val connectionState by component.connectionState.subscribeAsState()
    val availableTools by component.availableTools.subscribeAsState()

    val activeConnection = state.activeConnections.firstOrNull { it.id == state.activeConnectionId }
    val fabVisibility by remember {
        derivedStateOf {
            state.savedServers.isEmpty() || !state.showAddServerDialog
        }
    }
    val isConnected by remember(activeConnection, connectionState) {
        derivedStateOf {
            activeConnection != null && connectionState is McpClientService.ConnectionState.Connected
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP Server Connection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isConnected) {
                        IconButton(onClick = component::onRefreshTools) {
                            Icon(Icons.Default.Refresh, "Refresh tools")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (fabVisibility) {
                FloatingActionButton(
                    onClick = { component.onToggleAddServerDialog(true) }
                ) {
                    Icon(Icons.Default.Add, "Add Server")
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left Panel: Saved Servers (30%)
            SavedServersPanel(
                savedServers = state.savedServers,
                selectedConfig = state.currentConfig,
                activeConnections = state.activeConnections,
                activeConnectionId = state.activeConnectionId,
                onSelectServer = component::onSelectSavedServer,
                onConnectServer = component::onConnectSavedServer,
                onDisconnectServer = component::onDisconnectConnection,
                onSelectActiveConnection = component::onSelectActiveConnection,
                onDeleteServer = component::onDeleteServer,
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .padding(16.dp)
            )

            // Right Panel: Configuration & Tools (70%)
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Error message display
                if (state.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Connection status
                ConnectionStatusCard(
                    connectionState = connectionState,
                    activeConnections = state.activeConnections,
                    activeConnectionId = state.activeConnectionId,
                    onConnect = component::onConnect,
                    onDisconnect = component::onDisconnectConnection,
                    onSelectConnection = component::onSelectActiveConnection,
                    isLoading = state.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Configuration or Tools panel
                if (isConnected) {
                    // Show tools panel when connected
                    if (activeConnection != null) {
                        Text(
                            "Operating on ${activeConnection.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ToolsPanel(
                        availableTools = availableTools,
                        selectedTool = state.selectedTool,
                        toolInvocationResult = state.toolInvocationResult,
                        onSelectTool = component::onSelectTool,
                        onInvokeTool = component::onInvokeTool,
                        onToolArgumentsChanged = component::onToolArgumentsChanged,
                        isLoading = state.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Show configuration panel when disconnected
                    ServerConfigurationPanel(
                        config = state.currentConfig,
                        tempAuthValue = state.tempAuthValue,
                        tempApiKeyHeader = state.tempApiKeyHeader,
                        onServerNameChanged = component::onServerNameChanged,
                        onTransportTypeChanged = component::onTransportTypeChanged,
                        onServerUrlChanged = component::onServerUrlChanged,
                        onAuthTypeChanged = component::onAuthTypeChanged,
                        onAuthValueChanged = component::onAuthValueChanged,
                        onApiKeyHeaderChanged = component::onApiKeyHeaderChanged,
                        onCustomHeadersChanged = component::onCustomHeadersChanged,
                        onCommandArgsChanged = component::onCommandArgsChanged,
                        onEnvironmentVarsChanged = component::onEnvironmentVarsChanged,
                        onSaveConfiguration = component::onSaveCurrentConfiguration,
                        onTestConnection = component::onTestConnection,
                        isLoading = state.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Left panel showing saved server configurations.
 */
@Composable
private fun SavedServersPanel(
    savedServers: List<McpServerConfig>,
    selectedConfig: McpServerConfig,
    activeConnections: List<McpClientService.ActiveConnection>,
    activeConnectionId: String?,
    onSelectServer: (McpServerConfig) -> Unit,
    onConnectServer: (McpServerConfig) -> Unit,
    onDisconnectServer: (String) -> Unit,
    onSelectActiveConnection: (String) -> Unit,
    onDeleteServer: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            "Saved Servers (${savedServers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (savedServers.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "No saved servers.\nClick + to add a server.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedServers, key = { it.id }) { server ->
                    val runningConnection = activeConnections.firstOrNull { it.serverId == server.id }
                    ServerCard(
                        server = server,
                        isSelected = server.id == selectedConfig.id && selectedConfig.id > 0,
                        isFocused = runningConnection?.id == activeConnectionId,
                        runningConnection = runningConnection,
                        onClick = { onSelectServer(server) },
                        onConnect = { onConnectServer(server) },
                        onDisconnect = { runningConnection?.let { onDisconnectServer(it.id) } },
                        onFocus = { runningConnection?.let { onSelectActiveConnection(it.id) } },
                        onDelete = { onDeleteServer(server.id) }
                    )
                }
            }
        }
    }
}

/**
 * Card displaying a saved server configuration.
 */
@Composable
private fun ServerCard(
    server: McpServerConfig,
    isSelected: Boolean,
    isFocused: Boolean,
    runningConnection: McpClientService.ActiveConnection?,
    onClick: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onFocus: () -> Unit,
    onDelete: () -> Unit,
) {
    val connectionState = runningConnection?.state ?: McpClientService.ConnectionState.Disconnected
    val stateLabel = when (connectionState) {
        is McpClientService.ConnectionState.Disconnected -> "Disconnected"
        is McpClientService.ConnectionState.Connecting -> "Connecting"
        is McpClientService.ConnectionState.Connected -> "Connected"
        is McpClientService.ConnectionState.Error -> "Error"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFocused -> MaterialTheme.colorScheme.primaryContainer
                isSelected -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(connectionState)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            server.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            stateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            if (runningConnection != null) onDisconnect() else onConnect()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (runningConnection != null) Icons.Default.Close else Icons.Default.PlayArrow,
                            if (runningConnection != null) "Disconnect" else "Connect",
                            tint = if (runningConnection != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onFocus,
                        enabled = runningConnection != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            "Make active",
                            tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransportTypeBadge(server.transportType)
                AuthTypeBadge(server.authType)
                if (runningConnection != null) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${runningConnection.toolCount} tools",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                server.serverUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (connectionState is McpClientService.ConnectionState.Error) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    connectionState.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isFocused) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ACTIVE TARGET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else if (server.isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Badge showing transport type.
 */
@Composable
private fun TransportTypeBadge(transportType: McpClientService.McpTransportType) {
    AssistChip(
        onClick = {},
        label = { Text(transportType.name, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.height(24.dp)
    )
}

/**
 * Badge showing auth type.
 */
@Composable
private fun AuthTypeBadge(authType: McpServerConfig.AuthType) {
    val label = when (authType) {
        McpServerConfig.AuthType.NONE -> "No Auth"
        McpServerConfig.AuthType.BEARER -> "Bearer"
        McpServerConfig.AuthType.API_KEY -> "API Key"
        McpServerConfig.AuthType.CUSTOM_HEADERS -> "Custom"
    }

    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.height(24.dp)
    )
}

/**
 * Connection status card with connect/disconnect button.
 */
@Composable
private fun ConnectionStatusCard(
    connectionState: McpClientService.ConnectionState,
    activeConnections: List<McpClientService.ActiveConnection>,
    activeConnectionId: String?,
    onConnect: () -> Unit,
    onDisconnect: (String) -> Unit,
    onSelectConnection: (String) -> Unit,
    isLoading: Boolean,
) {
    val activeConnection = activeConnections.firstOrNull { it.id == activeConnectionId }
    val stateLabel = when (connectionState) {
        is McpClientService.ConnectionState.Disconnected -> "Disconnected"
        is McpClientService.ConnectionState.Connecting -> "Connecting..."
        is McpClientService.ConnectionState.Connected -> "Connected"
        is McpClientService.ConnectionState.Error -> "Error"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Active Connections (${activeConnections.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        activeConnection?.name ?: "No active connection selected",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (connectionState is McpClientService.ConnectionState.Error) {
                        Text(
                            connectionState.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    OutlinedButton(
                        onClick = { activeConnection?.let { onDisconnect(it.id) } },
                        enabled = activeConnection != null && !isLoading
                    ) {
                        Text("Disconnect selected")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onConnect,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Connect current config")
                        }
                    }
                }
            }

            if (activeConnections.isEmpty()) {
                Text(
                    "No MCP clients are running. Connect or start a saved server to launch one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeConnections, key = { it.id }) { connection ->
                        ActiveConnectionChip(
                            connection = connection,
                            isSelected = connection.id == activeConnectionId,
                            onSelect = { onSelectConnection(connection.id) },
                            onDisconnect = { onDisconnect(connection.id) }
                        )
                    }
                }

                if (activeConnection != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatusIndicator(connectionState)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                activeConnection.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${activeConnection.toolCount} tools • ${activeConnection.resourceCount} resources",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveConnectionChip(
    connection: McpClientService.ActiveConnection,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = onSelect,
            label = {
                Text(
                    connection.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingIcon = { StatusIndicator(connection.state) },
            modifier = Modifier.height(32.dp),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        )
        IconButton(
            onClick = onDisconnect,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Close,
                "Disconnect ${connection.name}",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Status indicator circle.
 */
@Composable
private fun StatusIndicator(state: McpClientService.ConnectionState) {
    val color = when (state) {
        is McpClientService.ConnectionState.Disconnected -> Color.Gray
        is McpClientService.ConnectionState.Connecting -> Color(0xFFFFA500) // Orange
        is McpClientService.ConnectionState.Connected -> Color(0xFF4CAF50) // Green
        is McpClientService.ConnectionState.Error -> Color(0xFFF44336) // Red
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

/**
 * Server configuration panel with all settings.
 */
@Composable
private fun ServerConfigurationPanel(
    config: McpServerConfig,
    tempAuthValue: String,
    tempApiKeyHeader: String,
    onServerNameChanged: (String) -> Unit,
    onTransportTypeChanged: (McpClientService.McpTransportType) -> Unit,
    onServerUrlChanged: (String) -> Unit,
    onAuthTypeChanged: (McpServerConfig.AuthType) -> Unit,
    onAuthValueChanged: (String) -> Unit,
    onApiKeyHeaderChanged: (String) -> Unit,
    onCustomHeadersChanged: (Map<String, String>) -> Unit,
    onCommandArgsChanged: (List<String>) -> Unit,
    onEnvironmentVarsChanged: (Map<String, String>) -> Unit,
    onSaveConfiguration: () -> Unit,
    onTestConnection: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Server Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Server Name
        item {
            OutlinedTextField(
                value = config.name,
                onValueChange = onServerNameChanged,
                label = { Text("Server Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Transport Type Tabs
        item {
            Text("Transport Type", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            val selectedTabIndex = when (config.transportType) {
                McpClientService.McpTransportType.SSE -> 0
                McpClientService.McpTransportType.HTTP -> 1
                McpClientService.McpTransportType.STDIO -> 2
            }
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier,
            ) {
                Tab(
                    selected = config.transportType == McpClientService.McpTransportType.SSE,
                    onClick = { onTransportTypeChanged(McpClientService.McpTransportType.SSE) },
                    text = { Text("SSE") }
                )
                Tab(
                    selected = config.transportType == McpClientService.McpTransportType.HTTP,
                    onClick = { onTransportTypeChanged(McpClientService.McpTransportType.HTTP) },
                    text = { Text("HTTP") }
                )
                Tab(
                    selected = config.transportType == McpClientService.McpTransportType.STDIO,
                    onClick = { onTransportTypeChanged(McpClientService.McpTransportType.STDIO) },
                    text = { Text("STDIO") }
                )
            }
        }
        when (config.transportType) {
            McpClientService.McpTransportType.SSE -> {
                item {
                    OutlinedTextField(
                        value = config.serverUrl,
                        onValueChange = onServerUrlChanged,
                        label = { Text("Server URL") },
                        placeholder = { Text("https://example.com/sse") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    AuthTypeSelector(
                        selectedAuthType = config.authType,
                        onAuthTypeChanged = onAuthTypeChanged,
                        tempAuthValue = tempAuthValue,
                        tempApiKeyHeader = tempApiKeyHeader,
                        authHeaders = config.authHeaders,
                        onAuthValueChanged = onAuthValueChanged,
                        onApiKeyHeaderChanged = onApiKeyHeaderChanged,
                        onCustomHeadersChanged = onCustomHeadersChanged
                    )
                }
            }

            McpClientService.McpTransportType.HTTP -> {
                item {
                    OutlinedTextField(
                        value = config.serverUrl,
                        onValueChange = onServerUrlChanged,
                        label = { Text("Server URL") },
                        placeholder = { Text("https://example.com/mcp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    AuthTypeSelector(
                        selectedAuthType = config.authType,
                        onAuthTypeChanged = onAuthTypeChanged,
                        tempAuthValue = tempAuthValue,
                        tempApiKeyHeader = tempApiKeyHeader,
                        authHeaders = config.authHeaders,
                        onAuthValueChanged = onAuthValueChanged,
                        onApiKeyHeaderChanged = onApiKeyHeaderChanged,
                        onCustomHeadersChanged = onCustomHeadersChanged
                    )
                }
            }

            McpClientService.McpTransportType.STDIO -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "STDIO Server Configuration Examples",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Java JAR: Command = java, Args = -jar /path/to/server.jar",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "• Node.js: Command = node, Args = /path/to/server.js",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "• Python: Command = python3, Args = /path/to/server.py",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = config.serverUrl,
                        onValueChange = onServerUrlChanged,
                        label = { Text("Command") },
                        placeholder = { Text("java, node, python3, etc.") },
                        supportingText = { Text("Executable command (e.g., 'java' for JAR files, 'node' for Node.js)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    CommandArgsEditor(
                        args = config.commandArgs,
                        onArgsChanged = onCommandArgsChanged
                    )
                }

                item {
                    EnvironmentVarsEditor(
                        vars = config.environmentVars,
                        onVarsChanged = onEnvironmentVarsChanged
                    )
                }
            }
        }

        // Action buttons
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && config.serverUrl.isNotBlank()
                ) {
                    Text("Test")
                }
                Button(
                    onClick = onSaveConfiguration,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && config.name.isNotBlank() && config.serverUrl.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/**
 * Auth type selector with appropriate input fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthTypeSelector(
    selectedAuthType: McpServerConfig.AuthType,
    onAuthTypeChanged: (McpServerConfig.AuthType) -> Unit,
    tempAuthValue: String,
    tempApiKeyHeader: String,
    authHeaders: Map<String, String>,
    onAuthValueChanged: (String) -> Unit,
    onApiKeyHeaderChanged: (String) -> Unit,
    onCustomHeadersChanged: (Map<String, String>) -> Unit,
) {
    Column {
        var expanded by remember { mutableStateOf(false) }

        Text("Authentication", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = when (selectedAuthType) {
                    McpServerConfig.AuthType.NONE -> "None"
                    McpServerConfig.AuthType.BEARER -> "Bearer Token"
                    McpServerConfig.AuthType.API_KEY -> "API Key"
                    McpServerConfig.AuthType.CUSTOM_HEADERS -> "Custom Headers"
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                McpServerConfig.AuthType.entries.forEach { authType ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (authType) {
                                    McpServerConfig.AuthType.NONE -> "None"
                                    McpServerConfig.AuthType.BEARER -> "Bearer Token"
                                    McpServerConfig.AuthType.API_KEY -> "API Key"
                                    McpServerConfig.AuthType.CUSTOM_HEADERS -> "Custom Headers"
                                }
                            )
                        },
                        onClick = {
                            onAuthTypeChanged(authType)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Auth-specific fields
        when (selectedAuthType) {
            McpServerConfig.AuthType.BEARER -> {
                OutlinedTextField(
                    value = tempAuthValue,
                    onValueChange = onAuthValueChanged,
                    label = { Text("Bearer Token") },
                    placeholder = { Text("your-token-here") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            McpServerConfig.AuthType.API_KEY -> {
                OutlinedTextField(
                    value = tempApiKeyHeader,
                    onValueChange = onApiKeyHeaderChanged,
                    label = { Text("Header Name") },
                    placeholder = { Text("X-API-Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tempAuthValue,
                    onValueChange = onAuthValueChanged,
                    label = { Text("API Key") },
                    placeholder = { Text("your-api-key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            McpServerConfig.AuthType.CUSTOM_HEADERS -> {
                CustomHeadersEditor(
                    headers = authHeaders,
                    onHeadersChanged = onCustomHeadersChanged
                )
            }

            McpServerConfig.AuthType.NONE -> {
                // No additional fields
            }
        }
    }
}

/**
 * Custom headers key-value editor.
 */
@Composable
private fun CustomHeadersEditor(
    headers: Map<String, String>,
    onHeadersChanged: (Map<String, String>) -> Unit,
) {
    var headersList by remember(headers) {
        mutableStateOf(headers.toList())
    }

    Column {
        Text("Custom Headers", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))

        headersList.forEachIndexed { index, (key, value) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { newKey ->
                        headersList = headersList.toMutableList().apply {
                            set(index, newKey to value)
                        }
                        onHeadersChanged(headersList.toMap())
                    },
                    label = { Text("Key") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        headersList = headersList.toMutableList().apply {
                            set(index, key to newValue)
                        }
                        onHeadersChanged(headersList.toMap())
                    },
                    label = { Text("Value") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        headersList = headersList.toMutableList().apply {
                            removeAt(index)
                        }
                        onHeadersChanged(headersList.toMap())
                    }
                ) {
                    Icon(Icons.Default.Delete, "Remove")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        TextButton(
            onClick = {
                headersList = headersList + ("" to "")
            }
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Header")
        }
    }
}

/**
 * Command arguments editor.
 */
@Composable
private fun CommandArgsEditor(
    args: List<String>,
    onArgsChanged: (List<String>) -> Unit,
) {
    var argsText by remember(args) {
        mutableStateOf(args.joinToString(" "))
    }

    OutlinedTextField(
        value = argsText,
        onValueChange = { newText ->
            argsText = newText
            onArgsChanged(newText.split(" ").filter { it.isNotBlank() })
        },
        label = { Text("Command Arguments") },
        placeholder = { Text("-jar /path/to/server.jar") },
        supportingText = { Text("Space-separated arguments. For JAR files: -jar /path/to/file.jar") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
}

/**
 * Environment variables editor.
 */
@Composable
private fun EnvironmentVarsEditor(
    vars: Map<String, String>,
    onVarsChanged: (Map<String, String>) -> Unit,
) {
    var varsList by remember(vars) {
        mutableStateOf(vars.toList())
    }

    Column {
        Text("Environment Variables", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))

        varsList.forEachIndexed { index, (key, value) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { newKey ->
                        varsList = varsList.toMutableList().apply {
                            set(index, newKey to value)
                        }
                        onVarsChanged(varsList.toMap())
                    },
                    label = { Text("Key") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        varsList = varsList.toMutableList().apply {
                            set(index, key to newValue)
                        }
                        onVarsChanged(varsList.toMap())
                    },
                    label = { Text("Value") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        varsList = varsList.toMutableList().apply {
                            removeAt(index)
                        }
                        onVarsChanged(varsList.toMap())
                    }
                ) {
                    Icon(Icons.Default.Delete, "Remove")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        TextButton(
            onClick = {
                varsList = varsList + ("" to "")
            }
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Variable")
        }
    }
}

/**
 * Tools panel showing available tools and invocation dialog.
 */
@Composable
private fun ToolsPanel(
    availableTools: List<McpClientService.ToolInfo>,
    selectedTool: McpClientService.ToolInfo?,
    toolInvocationResult: String?,
    onSelectTool: (McpClientService.ToolInfo?) -> Unit,
    onInvokeTool: () -> Unit,
    onToolArgumentsChanged: (Map<String, Any>) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            "Available Tools (${availableTools.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (availableTools.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "No tools available from this server.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableTools) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { onSelectTool(tool) }
                    )
                }
            }
        }
    }

    // Tool invocation dialog
    if (selectedTool != null) {
        ToolInvocationDialog(
            tool = selectedTool,
            result = toolInvocationResult,
            onDismiss = { onSelectTool(null) },
            onInvoke = onInvokeTool,
            onArgumentsChanged = onToolArgumentsChanged,
            isLoading = isLoading
        )
    }
}

/**
 * Card displaying a tool.
 */
@Composable
private fun ToolCard(
    tool: McpClientService.ToolInfo,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                tool.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Converts a JsonElement to a Kotlin Any type for MCP SDK compatibility.
 * Handles primitives, objects, and arrays recursively.
 */
fun jsonElementToAny(element: JsonElement): Any {
    return when (element) {
        is JsonPrimitive -> {
            when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.boolean
                element.intOrNull != null -> element.int
                element.longOrNull != null -> element.long
                element.doubleOrNull != null -> element.double
                else -> element.content
            }
        }

        is JsonObject -> {
            element.mapValues { (_, value) -> jsonElementToAny(value) }
        }

        is JsonArray -> {
            element.map { jsonElementToAny(it) }
        }
    }
}

/**
 * Dialog for invoking a tool with parameters.
 */
@Composable
private fun ToolInvocationDialog(
    tool: McpClientService.ToolInfo,
    result: String?,
    onDismiss: () -> Unit,
    onInvoke: () -> Unit,
    onArgumentsChanged: (Map<String, Any>) -> Unit,
    isLoading: Boolean,
) {
    var argsJson by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invoke Tool: ${tool.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = argsJson,
                    onValueChange = { newValue ->
                        argsJson = newValue
                        // Parse JSON to map using kotlinx.serialization
                        try {
                            val jsonObject = json.parseToJsonElement(newValue) as? JsonObject
                            if (jsonObject != null) {
                                val parsedMap = jsonObject.mapValues { (_, value) ->
                                    jsonElementToAny(value)
                                }
                                onArgumentsChanged(parsedMap)
                            } else {
                                // Not a JSON object, reset to empty map
                                onArgumentsChanged(emptyMap())
                            }
                        } catch (_: Exception) {
                            // Invalid JSON - don't update the map
                        }
                    },
                    label = { Text("Arguments (JSON)") },
                    placeholder = { Text(tool.parameters.properties.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (result != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Result:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            result,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onInvoke,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Invoke")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
