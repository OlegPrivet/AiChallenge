package org.oleg.ai.challenge.ui.rag

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import org.oleg.ai.challenge.utils.format
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.value.Value
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.oleg.ai.challenge.component.rag.DocumentManagementComponent
import org.oleg.ai.challenge.component.rag.DocumentUiModel
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.model.Embedding
import org.oleg.ai.challenge.domain.rag.model.KnowledgeSourceType
import org.oleg.ai.challenge.theme.AppTheme
import kotlin.math.sqrt

@Composable
fun DocumentManagementScreen(
    component: DocumentManagementComponent,
    modifier: Modifier = Modifier
)
{
    val documents by component.documents.subscribeAsState()
    val isIngesting by component.isIngesting.subscribeAsState()
    val status by component.statusMessage.subscribeAsState()
    val uploadProgress by component.uploadProgress.subscribeAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(KnowledgeSourceType.INTERNAL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = component::onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Document Management",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Text(
            text = "Ingest documents to power RAG. Add a title, pick source type, paste content, and ingest.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

        SourceTypeSelector(
            selected = sourceType,
            onSelected = { sourceType = it }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    component.onIngest(
                        title = title,
                        description = description,
                        content = content,
                        sourceType = sourceType
                    )
                },
                enabled = !isIngesting
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Ingest")
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isIngesting) "Ingesting..." else "Ingest Document")
            }

            Button(
                onClick = component::onUploadFiles,
                enabled = !isIngesting
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Files")
            }

            TextButton(
                onClick = component::onIngestSample,
                enabled = !isIngesting
            ) {
                Text("Sample")
            }
        }

        if (status.isNotBlank()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when (val progress = uploadProgress) {
            is org.oleg.ai.challenge.component.rag.UploadProgress.Uploading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Uploading ${progress.currentFile}/${progress.totalFiles}: ${progress.fileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LinearProgressIndicator(
                        progress = { progress.currentFile.toFloat() / progress.totalFiles.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is org.oleg.ai.challenge.component.rag.UploadProgress.None -> {
                // No upload in progress
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Knowledge Base",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(documents, key = { it.id }) { doc ->
                DocumentCard(
                    doc = doc,
                    onDelete = { component.onDelete(doc.id) }
                )
            }
        }
    }
}

@Composable
private fun SourceTypeSelector(
    selected: KnowledgeSourceType,
    onSelected: (KnowledgeSourceType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KnowledgeSourceType.entries.forEach { type ->
            androidx.compose.material3.FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(type.name) }
            )
        }
    }
}

@Composable
private fun DocumentCard(
    doc: DocumentUiModel,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Source: ${doc.sourceType} • Chunking ${doc.chunkingVersion} • Embedding ${doc.embeddingVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (doc.chunks.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete document")
                }
            }

            // Expanded Content
            if (expanded && doc.chunks.isNotEmpty()) {
                ChunksSection(chunks = doc.chunks)
            }
        }
    }
}

@Composable
private fun ChunksSection(chunks: List<DocumentChunk>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Divider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "${chunks.size} chunks indexed",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        chunks.forEachIndexed { index, chunk ->
            ChunkItem(chunk = chunk, index = index)
        }
    }
}

@Composable
private fun ChunkItem(chunk: DocumentChunk, index: Int) {
    var showFullVector by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chunk header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Chunk #${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                chunk.tokenCount?.let { tokens ->
                    Text(
                        text = "$tokens tokens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content preview
            Text(
                text = chunk.content.take(150) + if (chunk.content.length > 150) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Embedding section
            chunk.embedding?.let { embedding ->
                EmbeddingSection(
                    embedding = embedding,
                    showFullVector = showFullVector,
                    onToggleFullVector = { showFullVector = !showFullVector }
                )
            } ?: run {
                Text(
                    text = "No embedding generated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmbeddingSection(
    embedding: Embedding,
    showFullVector: Boolean,
    onToggleFullVector: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Embedding:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        // Stats
        EmbeddingStats(embedding)

        // Mini heatmap
        MiniHeatmap(embedding.values.take(50))

        // Full vector (collapsible)
        if (showFullVector) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = embedding.values.joinToString(", ") { it.format(4) },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onToggleFullVector) {
                Text(if (showFullVector) "Hide vector" else "Show all ${embedding.dimensions} values")
            }

            TextButton(
                onClick = {
                    // Copy to clipboard
                    // Note: Platform-specific implementation needed
                }
            ) {
                Text("Copy vector")
            }
        }
    }
}

@Composable
private fun EmbeddingStats(embedding: Embedding) {
    val stats = remember(embedding) { calculateEmbeddingStats(embedding.values) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${embedding.dimensions}D • ${embedding.model}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (embedding.normalized) "Normalized" else "Not normalized",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "min: ${stats.min.format(4)} • max: ${stats.max.format(4)} • mean: ${stats.mean.format(4)} • norm: ${stats.norm.format(4)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniHeatmap(values: List<Float>) {
    if (values.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        values.forEach { value ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colorForValue(value))
            )
        }
    }
}

private fun colorForValue(value: Float): Color {
    // Normalize value to [0, 1] range
    // Assuming embeddings are typically in [-1, 1] or [0, 1]
    val normalized = ((value + 1f) / 2f).coerceIn(0f, 1f)
    return Color(
        red = normalized,
        green = 0.5f,
        blue = 1f - normalized,
        alpha = 1f
    )
}

private data class EmbeddingStats(
    val min: Float,
    val max: Float,
    val mean: Float,
    val norm: Float
)

private fun calculateEmbeddingStats(values: List<Float>): EmbeddingStats {
    if (values.isEmpty()) {
        return EmbeddingStats(0f, 0f, 0f, 0f)
    }
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 0f
    val mean = values.average().toFloat()
    val norm = sqrt(values.sumOf { (it * it).toDouble() }).toFloat()
    return EmbeddingStats(min, max, mean, norm)
}

@Preview
@Composable
private fun DocumentManagementScreenPreview() {
    val fakeComponent = object : DocumentManagementComponent {
        override val documents: Value<List<DocumentUiModel>> =
            com.arkivanov.decompose.value.MutableValue(
                listOf(
                    DocumentUiModel(
                        id = "1",
                        title = "Guide",
                        sourceType = KnowledgeSourceType.INTERNAL,
                        updatedAt = 0L,
                        chunkingVersion = "recursive-1",
                        embeddingVersion = "nomic-embed-text"
                    )
                )
            )
        override val isIngesting: Value<Boolean> = com.arkivanov.decompose.value.MutableValue(false)
        override val statusMessage: Value<String> = com.arkivanov.decompose.value.MutableValue("Ready")
        override val uploadProgress: Value<org.oleg.ai.challenge.component.rag.UploadProgress> =
            com.arkivanov.decompose.value.MutableValue(org.oleg.ai.challenge.component.rag.UploadProgress.None)
        override fun onIngest(title: String, description: String, content: String, sourceType: KnowledgeSourceType) {}
        override fun onIngestSample() {}
        override fun onUploadFiles() {}
        override fun onDelete(documentId: String) {}
        override fun onBack() {}
    }
    AppTheme {
        DocumentManagementScreen(component = fakeComponent)
    }
}
