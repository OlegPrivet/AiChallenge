package org.oleg.ai.challenge.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.mikepenz.markdown.m3.Markdown
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.oleg.ai.challenge.component.planner.PlannerComponent
import org.oleg.ai.challenge.component.planner.PreviewPlannerComponent
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.PlannerFrequency
import org.oleg.ai.challenge.data.network.model.Usage
import org.oleg.ai.challenge.data.network.service.McpClientService
import org.oleg.ai.challenge.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    component: PlannerComponent,
    modifier: Modifier = Modifier,
) {
    val messages by component.messages.subscribeAsState()
    val inputText by component.inputText.subscribeAsState()
    val isLoading by component.isLoading.subscribeAsState()
    val showInputField by component.showInputField.subscribeAsState()
    val availableTools by component.availableTools.subscribeAsState()
    val selectedToolName by component.selectedToolName.subscribeAsState()
    val selectedFrequency by component.selectedFrequency.subscribeAsState()
    val isPeriodicRunning by component.isPeriodicRunning.subscribeAsState()

    // Find the selected tool from the list
    val selectedTool = availableTools.find { it.name == selectedToolName }

    val lazyListState: LazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planner Mode") },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tool selector
            ToolSelector(
                availableTools = availableTools,
                selectedTool = selectedTool,
                onToolSelected = component::onSelectTool,
                modifier = Modifier.fillMaxWidth()
            )

            // Frequency selector
            FrequencySelector(
                selectedFrequency = selectedFrequency,
                onFrequencyChanged = component::onFrequencyChanged,
                enabled = !isPeriodicRunning,
                modifier = Modifier.fillMaxWidth()
            )

            // Periodic execution controls
            PeriodicControls(
                isRunning = isPeriodicRunning,
                onStart = component::onStartPeriodic,
                onStop = component::onStopPeriodic,
                enabled = !showInputField,
                modifier = Modifier.fillMaxWidth()
            )

            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = lazyListState,
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                items(
                    messages.filter { it.isVisibleInUI }.reversed(),
                    key = { it.id }
                ) { message ->
                    PlannerMessageItem(message)
                }
            }

            // Input field (only shown before first message)
            if (showInputField) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = component::onTextChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter your message...") },
                        maxLines = 3,
                        enabled = !isLoading
                    )

                    Box {
                        IconButton(
                            onClick = component::onSendMessage,
                            enabled = inputText.isNotBlank() && !isLoading
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send"
                            )
                        }
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            // Loading indicator (when periodic is running)
            if (isLoading && !showInputField) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Processing request...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                lazyListState.animateScrollToItem(0)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolSelector(
    availableTools: List<McpClientService.ToolInfo>,
    selectedTool: McpClientService.ToolInfo?,
    onToolSelected: (McpClientService.ToolInfo?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "MCP Tool",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedTool?.name ?: "None (Direct AI)",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Option for no tool
                DropdownMenuItem(
                    text = { Text("None (Direct AI)") },
                    onClick = {
                        onToolSelected(null)
                        expanded = false
                    }
                )

                // Available tools
                availableTools.forEach { tool ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = tool.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = tool.description.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onToolSelected(tool)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(
    selectedFrequency: PlannerFrequency,
    onFrequencyChanged: (PlannerFrequency) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Request Frequency",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = selectedFrequency.displayName,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false }
            ) {
                PlannerFrequency.entries.forEach { frequency ->
                    DropdownMenuItem(
                        text = { Text(frequency.displayName) },
                        onClick = {
                            onFrequencyChanged(frequency)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodicControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Periodic Execution:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isRunning) {
            IconButton(
                onClick = onStop,
                enabled = true
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = "Running",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            IconButton(
                onClick = onStart,
                enabled = enabled
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Start",
                    tint = if (enabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            Text(
                text = if (enabled) "Stopped" else "Send message first",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlannerMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 100.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.wrapContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Model badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    message.modelUsed?.let { model ->
                        Text(
                            text = "Model: $model",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Magenta
                        )
                    }
                }

                // Token usage display
                if (message.usage != null) {
                    PlannerTokenUsageDisplay(
                        usage = message.usage,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Message content
                SelectionContainer {
                    Markdown(
                        content = message.text,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannerTokenUsageDisplay(
    usage: Usage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tokens:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = "${usage.promptTokens} in",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = "/",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
        )
        Text(
            text = "${usage.completionTokens} out",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = "/",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
        )
        Text(
            text = "${usage.totalTokens} total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Preview
@Composable
private fun PlannerScreenPreview() {
    AppTheme {
        PlannerScreen(PreviewPlannerComponent())
    }
}
