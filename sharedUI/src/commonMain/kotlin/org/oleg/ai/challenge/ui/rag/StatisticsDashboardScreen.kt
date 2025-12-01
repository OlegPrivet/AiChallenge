package org.oleg.ai.challenge.ui.rag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.oleg.ai.challenge.component.rag.KnowledgeBaseStats
import org.oleg.ai.challenge.component.rag.QueryHistoryStats
import org.oleg.ai.challenge.component.rag.StatisticsDashboardComponent
import org.oleg.ai.challenge.data.model.QueryHistory
import org.oleg.ai.challenge.theme.AppTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.round

@Composable
fun StatisticsDashboardScreen(
    component: StatisticsDashboardComponent,
    modifier: Modifier = Modifier
) {
    val stats by component.stats.subscribeAsState()
    val queryHistoryStats by component.queryHistoryStats.subscribeAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = component::onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Knowledge Base Statistics",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = component::refresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Text(
            text = "Overview of your RAG knowledge base",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Documents Card
        StatCard(
            title = "Documents",
            value = stats.totalDocuments.toString(),
            description = "Total documents in knowledge base"
        )

        // Chunks Card
        StatCard(
            title = "Chunks",
            value = stats.totalChunks.toString(),
            description = "Total chunks generated from documents"
        )

        // Embedding Model Card
        StatCard(
            title = "Embedding Model",
            value = stats.embeddingModel,
            description = "Model used for vector embeddings"
        )

        // Chunking Strategy Card
        StatCard(
            title = "Chunking Strategy",
            value = stats.defaultChunkingStrategy,
            description = "Default strategy for splitting documents"
        )

        // Last Updated Card
        stats.lastUpdated?.let { timestamp ->
            val date = formatTimestamp(timestamp)
            StatCard(
                title = "Last Updated",
                value = date,
                description = "Most recent document update"
            )
        }

        // Query History Section
        Text(
            text = "Query History",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Total Queries Card
        StatCard(
            title = "Total RAG Queries",
            value = queryHistoryStats.totalQueries.toString(),
            description = "Total number of RAG queries executed"
        )

        // Average Relevance Score Card
        if (queryHistoryStats.totalQueries > 0) {
            StatCard(
                title = "Average Relevance",
                value = "${(round(queryHistoryStats.averageRelevanceScore * 100) / 100)}",
                description = "Average relevance score of retrieved chunks"
            )
        }

        // Recent Queries Card
        if (queryHistoryStats.recentQueries.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Recent Queries",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    queryHistoryStats.recentQueries.forEach { query ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = query.queryText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Results: ${query.resultsCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Score: ${(round(query.averageRelevanceScore * 100) / 100)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatTimestamp(query.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Additional Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "About RAG",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Retrieval-Augmented Generation (RAG) enhances AI responses by retrieving relevant information from your knowledge base. Documents are split into chunks, embedded using AI models, and stored for efficient semantic search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatTimestamp(timestamp: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return if (minutes < 1) {
        "Just now"
    } else if (minutes < 60) {
        "$minutes minutes ago"
    } else if (hours < 24) {
        "$hours hours ago"
    } else if (days < 7) {
        "$days days ago"
    } else {
        "${days / 7} weeks ago"
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Preview
@Composable
private fun StatisticsDashboardScreenPreview() {
    val now = Clock.System.now().toEpochMilliseconds()
    val fakeComponent = object : StatisticsDashboardComponent {
        override val stats = com.arkivanov.decompose.value.MutableValue(
            KnowledgeBaseStats(
                totalDocuments = 12,
                totalChunks = 156,
                embeddingModel = "nomic-embed-text",
                defaultChunkingStrategy = "recursive",
                lastUpdated = now - 3600000 // 1 hour ago
            )
        )
        override val queryHistoryStats = com.arkivanov.decompose.value.MutableValue(
            QueryHistoryStats(
                totalQueries = 25,
                averageRelevanceScore = 0.78,
                recentQueries = listOf(
                    QueryHistory(
                        id = 1,
                        queryText = "How do I implement RAG in Kotlin?",
                        timestamp = now - 300000, // 5 minutes ago
                        resultsCount = 5,
                        averageRelevanceScore = 0.85,
                        citationsCount = 3,
                        topK = 5,
                        similarityThreshold = 0.7
                    ),
                    QueryHistory(
                        id = 2,
                        queryText = "What is vector embedding?",
                        timestamp = now - 1800000, // 30 minutes ago
                        resultsCount = 4,
                        averageRelevanceScore = 0.72,
                        citationsCount = 2,
                        topK = 5,
                        similarityThreshold = 0.7
                    )
                )
            )
        )
        override fun refresh() {}
        override fun onBack() {}
    }
    AppTheme {
        StatisticsDashboardScreen(component = fakeComponent)
    }
}
