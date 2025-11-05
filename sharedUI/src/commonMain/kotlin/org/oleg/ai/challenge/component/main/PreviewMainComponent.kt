package org.oleg.ai.challenge.component.main

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

class PreviewMainComponent : MainComponent {
    override val isPromptDialogVisible: Value<Boolean> = MutableValue(false)
    override val systemPrompt: Value<String> = MutableValue("")
    override val assistantPrompt: Value<String> = MutableValue("")

    override fun onNavigateToChat() = Unit
    override fun onShowPromptDialog() = Unit
    override fun onDismissPromptDialog() = Unit
    override fun onSavePromptsAndNavigate(systemPrompt: String, assistantPrompt: String) = Unit
}
