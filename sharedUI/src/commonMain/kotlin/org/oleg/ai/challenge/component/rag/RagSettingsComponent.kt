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
    val enableExternalTools: Boolean = false
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
                enableExternalTools = settings.enableExternalTools
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
    fun resetToDefaults()
    fun onBack()
}
