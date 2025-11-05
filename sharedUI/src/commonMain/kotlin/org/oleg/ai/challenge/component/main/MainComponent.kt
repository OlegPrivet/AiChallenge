package org.oleg.ai.challenge.component.main

import com.arkivanov.decompose.value.Value

interface MainComponent {
    val isPromptDialogVisible: Value<Boolean>
    val systemPrompt: Value<String>
    val assistantPrompt: Value<String>

    fun onNavigateToChat()
    fun onShowPromptDialog()
    fun onDismissPromptDialog()
    fun onSavePromptsAndNavigate(systemPrompt: String, assistantPrompt: String)
}
