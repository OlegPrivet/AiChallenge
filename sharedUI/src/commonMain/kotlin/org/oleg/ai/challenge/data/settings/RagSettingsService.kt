package org.oleg.ai.challenge.data.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RagSettingsService(private val settings: Settings) {

    private val _ragSettings = MutableStateFlow(loadSettings())
    val ragSettings: StateFlow<RagSettings> = _ragSettings.asStateFlow()

    fun loadSettings(): RagSettings {
        return RagSettings(
            topK = settings[KEY_TOP_K, RagSettings().topK],
            similarityThreshold = settings[KEY_SIMILARITY_THRESHOLD, RagSettings().similarityThreshold],
            chunkingStrategy = settings[KEY_CHUNKING_STRATEGY, RagSettings().chunkingStrategy],
            hybridSearchWeight = settings[KEY_HYBRID_SEARCH_WEIGHT, RagSettings().hybridSearchWeight],
            enableHybridSearch = settings[KEY_ENABLE_HYBRID_SEARCH, RagSettings().enableHybridSearch],
            enableAgenticRag = settings[KEY_ENABLE_AGENTIC_RAG, RagSettings().enableAgenticRag],
            enableExternalTools = settings[KEY_ENABLE_EXTERNAL_TOOLS, RagSettings().enableExternalTools],
            enableReranker = settings[KEY_ENABLE_RERANKER, RagSettings().enableReranker],
            rerankerThreshold = settings[KEY_RERANKER_THRESHOLD, RagSettings().rerankerThreshold],
            bm25Weight = settings[KEY_BM25_WEIGHT, RagSettings().bm25Weight],
            semanticWeight = settings[KEY_SEMANTIC_WEIGHT, RagSettings().semanticWeight]
        )
    }

    fun saveSettings(ragSettings: RagSettings) {
        settings[KEY_TOP_K] = ragSettings.topK
        settings[KEY_SIMILARITY_THRESHOLD] = ragSettings.similarityThreshold
        settings[KEY_CHUNKING_STRATEGY] = ragSettings.chunkingStrategy
        settings[KEY_HYBRID_SEARCH_WEIGHT] = ragSettings.hybridSearchWeight
        settings[KEY_ENABLE_HYBRID_SEARCH] = ragSettings.enableHybridSearch
        settings[KEY_ENABLE_AGENTIC_RAG] = ragSettings.enableAgenticRag
        settings[KEY_ENABLE_EXTERNAL_TOOLS] = ragSettings.enableExternalTools
        settings[KEY_ENABLE_RERANKER] = ragSettings.enableReranker
        settings[KEY_RERANKER_THRESHOLD] = ragSettings.rerankerThreshold
        settings[KEY_BM25_WEIGHT] = ragSettings.bm25Weight
        settings[KEY_SEMANTIC_WEIGHT] = ragSettings.semanticWeight

        _ragSettings.value = ragSettings
    }

    fun updateTopK(value: Int) {
        val current = _ragSettings.value
        saveSettings(current.copy(topK = value.coerceIn(RagSettings.MIN_TOP_K, RagSettings.MAX_TOP_K)))
    }

    fun updateSimilarityThreshold(value: Float) {
        val current = _ragSettings.value
        saveSettings(current.copy(similarityThreshold = value.coerceIn(RagSettings.MIN_SIMILARITY, RagSettings.MAX_SIMILARITY)))
    }

    fun updateChunkingStrategy(strategy: String) {
        val current = _ragSettings.value
        saveSettings(current.copy(chunkingStrategy = strategy))
    }

    fun updateHybridSearchWeight(value: Float) {
        val current = _ragSettings.value
        saveSettings(current.copy(hybridSearchWeight = value.coerceIn(RagSettings.MIN_HYBRID_WEIGHT, RagSettings.MAX_HYBRID_WEIGHT)))
    }

    fun updateEnableHybridSearch(enabled: Boolean) {
        val current = _ragSettings.value
        saveSettings(current.copy(enableHybridSearch = enabled))
    }

    fun updateEnableAgenticRag(enabled: Boolean) {
        val current = _ragSettings.value
        saveSettings(current.copy(enableAgenticRag = enabled))
    }

    fun updateEnableExternalTools(enabled: Boolean) {
        val current = _ragSettings.value
        saveSettings(current.copy(enableExternalTools = enabled))
    }

    fun updateEnableReranker(enabled: Boolean) {
        val current = _ragSettings.value
        saveSettings(current.copy(enableReranker = enabled))
    }

    fun updateRerankerThreshold(value: Float) {
        val current = _ragSettings.value
        saveSettings(current.copy(rerankerThreshold = value.coerceIn(RagSettings.MIN_RERANKER_THRESHOLD, RagSettings.MAX_RERANKER_THRESHOLD)))
    }

    fun updateBm25Weight(value: Float) {
        val current = _ragSettings.value
        saveSettings(current.copy(bm25Weight = value.coerceIn(RagSettings.MIN_RERANKER_WEIGHT, RagSettings.MAX_RERANKER_WEIGHT)))
    }

    fun updateSemanticWeight(value: Float) {
        val current = _ragSettings.value
        saveSettings(current.copy(semanticWeight = value.coerceIn(RagSettings.MIN_RERANKER_WEIGHT, RagSettings.MAX_RERANKER_WEIGHT)))
    }

    fun resetToDefaults() {
        saveSettings(RagSettings())
    }

    companion object {
        private const val KEY_TOP_K = "rag_top_k"
        private const val KEY_SIMILARITY_THRESHOLD = "rag_similarity_threshold"
        private const val KEY_CHUNKING_STRATEGY = "rag_chunking_strategy"
        private const val KEY_HYBRID_SEARCH_WEIGHT = "rag_hybrid_search_weight"
        private const val KEY_ENABLE_HYBRID_SEARCH = "rag_enable_hybrid_search"
        private const val KEY_ENABLE_AGENTIC_RAG = "rag_enable_agentic_rag"
        private const val KEY_ENABLE_EXTERNAL_TOOLS = "rag_enable_external_tools"
        private const val KEY_ENABLE_RERANKER = "rag_enable_reranker"
        private const val KEY_RERANKER_THRESHOLD = "rag_reranker_threshold"
        private const val KEY_BM25_WEIGHT = "rag_bm25_weight"
        private const val KEY_SEMANTIC_WEIGHT = "rag_semantic_weight"
    }
}
