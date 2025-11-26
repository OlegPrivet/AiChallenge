package org.oleg.ai.challenge.component.rag

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.settings.RagSettings

data class RagSettingsState(
    val topK: Int = 5,
    val similarityThreshold: Float = 0.7f,
    val hybridSearchWeight: Float = 0.5f,
    val chunkingStrategy: String = "recursive",
    val enableHybridSearch: Boolean = false,
    val enableAgenticRag: Boolean = true,
    val enableExternalTools: Boolean = false,
    val enableReranker: Boolean = false,
    val rerankerThreshold: Float = 0.5f,
    val bm25Weight: Float = 0.3f,
    val semanticWeight: Float = 0.7f
) {
    companion object {
        fun from(settings: RagSettings): RagSettingsState {
            return RagSettingsState(
                topK = settings.topK,
                similarityThreshold = settings.similarityThreshold,
                hybridSearchWeight = settings.hybridSearchWeight,
                chunkingStrategy = settings.chunkingStrategy,
                enableHybridSearch = settings.enableHybridSearch,
                enableAgenticRag = settings.enableAgenticRag,
                enableExternalTools = settings.enableExternalTools,
                enableReranker = settings.enableReranker,
                rerankerThreshold = settings.rerankerThreshold,
                bm25Weight = settings.bm25Weight,
                semanticWeight = settings.semanticWeight
            )
        }
    }
}

interface RagSettingsComponent {
    val state: Value<RagSettingsState>

    fun updateTopK(value: Int)
    fun updateSimilarityThreshold(value: Float)
    fun updateHybridSearchWeight(value: Float)
    fun updateChunkingStrategy(value: String)
    fun updateEnableHybridSearch(enabled: Boolean)
    fun updateEnableAgenticRag(enabled: Boolean)
    fun updateEnableExternalTools(enabled: Boolean)
    fun updateEnableReranker(enabled: Boolean)
    fun updateRerankerThreshold(value: Float)
    fun updateBm25Weight(value: Float)
    fun updateSemanticWeight(value: Float)
    fun resetToDefaults()
    fun onBack()
}
