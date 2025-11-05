package org.oleg.ai.challenge.component.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val onNavigateToChatWithPrompts: (systemPrompt: String, assistantPrompt: String) -> Unit
) : MainComponent, ComponentContext by componentContext {

    private val _isPromptDialogVisible = MutableValue(false)
    override val isPromptDialogVisible: Value<Boolean> = _isPromptDialogVisible

    private val _systemPrompt = MutableValue("")
    override val systemPrompt: Value<String> = _systemPrompt

    private val _assistantPrompt = MutableValue("")
    override val assistantPrompt: Value<String> = _assistantPrompt

    override fun onNavigateToChat() {
        onShowPromptDialog()
    }

    override fun onShowPromptDialog() {
        _isPromptDialogVisible.value = true
    }

    override fun onDismissPromptDialog() {
        _isPromptDialogVisible.value = false
    }

    override fun onSavePromptsAndNavigate(systemPrompt: String, assistantPrompt: String) {
        _systemPrompt.value = systemPrompt
        _assistantPrompt.value = assistantPrompt
        _isPromptDialogVisible.value = false
        onNavigateToChatWithPrompts(systemPrompt, assistantPrompt)
    }
}
