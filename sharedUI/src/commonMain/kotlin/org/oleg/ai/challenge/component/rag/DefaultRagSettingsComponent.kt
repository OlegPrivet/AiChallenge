package org.oleg.ai.challenge.component.rag

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.oleg.ai.challenge.data.settings.RagSettingsService

class DefaultRagSettingsComponent(
    componentContext: ComponentContext,
    private val ragSettingsService: RagSettingsService,
    private val onBack: () -> Unit
) : RagSettingsComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _state = MutableValue(RagSettingsState.from(ragSettingsService.loadSettings()))
    override val state: Value<RagSettingsState> = _state

    init {
        ragSettingsService.ragSettings
            .onEach { settings ->
                _state.value = RagSettingsState.from(settings)
            }
            .launchIn(scope)
    }

    override fun updateTopK(value: Int) {
        ragSettingsService.updateTopK(value)
    }

    override fun updateSimilarityThreshold(value: Float) {
        ragSettingsService.updateSimilarityThreshold(value)
    }

    override fun updateHybridSearchWeight(value: Float) {
        ragSettingsService.updateHybridSearchWeight(value)
    }

    override fun updateChunkingStrategy(value: String) {
        ragSettingsService.updateChunkingStrategy(value)
    }

    override fun updateEnableHybridSearch(enabled: Boolean) {
        ragSettingsService.updateEnableHybridSearch(enabled)
    }

    override fun updateEnableAgenticRag(enabled: Boolean) {
        ragSettingsService.updateEnableAgenticRag(enabled)
    }

    override fun updateEnableExternalTools(enabled: Boolean) {
        ragSettingsService.updateEnableExternalTools(enabled)
    }

    override fun resetToDefaults() {
        ragSettingsService.resetToDefaults()
    }

    override fun onBack() = onBack.invoke()
}
