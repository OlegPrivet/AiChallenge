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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.oleg.ai.challenge.component.rag.RagSettingsComponent
import org.oleg.ai.challenge.component.rag.RagSettingsState
import org.oleg.ai.challenge.data.settings.RagSettings
import org.oleg.ai.challenge.theme.AppTheme

@Composable
fun RagSettingsScreen(
    component: RagSettingsComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.subscribeAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                text = "RAG Settings",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Text(
            text = "Configure RAG retrieval and search parameters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Basic Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Basic Settings",
                    style = MaterialTheme.typography.titleMedium
                )

                SettingSlider(
                    title = "Top-K",
                    description = "Number of chunks to retrieve (${state.topK})",
                    value = state.topK.toFloat(),
                    valueRange = RagSettings.MIN_TOP_K.toFloat()..RagSettings.MAX_TOP_K.toFloat(),
                    onValueChange = { component.updateTopK(it.toInt()) }
                )

                SettingSlider(
                    title = "Similarity Threshold",
                    description = "Minimum similarity score to include results (${"%.2f".format(state.similarityThreshold)})",
                    value = state.similarityThreshold,
                    valueRange = RagSettings.MIN_SIMILARITY..RagSettings.MAX_SIMILARITY,
                    onValueChange = component::updateSimilarityThreshold
                )
            }
        }

        // Chunking Strategy Card
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
                    text = "Chunking Strategy",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "How documents are split into chunks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RagSettings.CHUNKING_STRATEGIES.forEach { (id, label) ->
                        FilterChip(
                            selected = state.chunkingStrategy == id,
                            onClick = { component.updateChunkingStrategy(id) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
            }
        }

        // Advanced Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Advanced Settings",
                    style = MaterialTheme.typography.titleMedium
                )

                SettingSwitch(
                    title = "Enable Agentic RAG",
                    description = "Use AI agents for source validation and conflict resolution",
                    checked = state.enableAgenticRag,
                    onCheckedChange = component::updateEnableAgenticRag
                )

                SettingSwitch(
                    title = "Enable Hybrid Search",
                    description = "Combine vector search with BM25 keyword search",
                    checked = state.enableHybridSearch,
                    onCheckedChange = component::updateEnableHybridSearch
                )

                if (state.enableHybridSearch) {
                    SettingSlider(
                        title = "Hybrid Search Weight",
                        description = "Vector search vs BM25 balance (${"%.2f".format(state.hybridSearchWeight)})",
                        value = state.hybridSearchWeight,
                        valueRange = RagSettings.MIN_HYBRID_WEIGHT..RagSettings.MAX_HYBRID_WEIGHT,
                        onValueChange = component::updateHybridSearchWeight
                    )
                }

                SettingSwitch(
                    title = "Enable External Tools",
                    description = "Allow web search when knowledge gaps are detected",
                    checked = state.enableExternalTools,
                    onCheckedChange = component::updateEnableExternalTools
                )
            }
        }

        // Reranking Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Hybrid Reranking",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Improve search accuracy by combining BM25 and semantic similarity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SettingSwitch(
                    title = "Enable Reranker",
                    description = "Use hybrid scoring to rerank search results",
                    checked = state.enableReranker,
                    onCheckedChange = component::updateEnableReranker
                )

                if (state.enableReranker) {
                    SettingSlider(
                        title = "BM25 Weight",
                        description = "Weight for lexical/keyword matching (${"%.2f".format(state.bm25Weight)})",
                        value = state.bm25Weight,
                        valueRange = RagSettings.MIN_RERANKER_WEIGHT..RagSettings.MAX_RERANKER_WEIGHT,
                        onValueChange = component::updateBm25Weight
                    )

                    SettingSlider(
                        title = "Semantic Weight",
                        description = "Weight for semantic similarity (${"%.2f".format(state.semanticWeight)})",
                        value = state.semanticWeight,
                        valueRange = RagSettings.MIN_RERANKER_WEIGHT..RagSettings.MAX_RERANKER_WEIGHT,
                        onValueChange = component::updateSemanticWeight
                    )

                    val totalWeight = state.bm25Weight + state.semanticWeight
                    if (totalWeight != 1.0f) {
                        Text(
                            text = "Note: Weights will be normalized (total: ${"%.2f".format(totalWeight)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    SettingSlider(
                        title = "Relevance Threshold",
                        description = "Minimum reranked score to keep results (${"%.2f".format(state.rerankerThreshold)})",
                        value = state.rerankerThreshold,
                        valueRange = RagSettings.MIN_RERANKER_THRESHOLD..RagSettings.MAX_RERANKER_THRESHOLD,
                        onValueChange = component::updateRerankerThreshold
                    )
                }
            }
        }

        // Reset Button
        Button(
            onClick = component::resetToDefaults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset to Defaults")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingSlider(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview
@Composable
private fun RagSettingsScreenPreview() {
    val fakeComponent = object : RagSettingsComponent {
        override val state = com.arkivanov.decompose.value.MutableValue(RagSettingsState())
        override fun updateTopK(value: Int) {}
        override fun updateSimilarityThreshold(value: Float) {}
        override fun updateHybridSearchWeight(value: Float) {}
        override fun updateChunkingStrategy(value: String) {}
        override fun updateEnableHybridSearch(enabled: Boolean) {}
        override fun updateEnableAgenticRag(enabled: Boolean) {}
        override fun updateEnableExternalTools(enabled: Boolean) {}
        override fun updateEnableReranker(enabled: Boolean) {}
        override fun updateRerankerThreshold(value: Float) {}
        override fun updateBm25Weight(value: Float) {}
        override fun updateSemanticWeight(value: Float) {}
        override fun resetToDefaults() {}
        override fun onBack() {}
    }
    AppTheme {
        RagSettingsScreen(component = fakeComponent)
    }
}
