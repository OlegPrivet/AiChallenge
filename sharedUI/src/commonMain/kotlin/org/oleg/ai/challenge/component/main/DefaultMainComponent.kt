package org.oleg.ai.challenge.component.main

import com.arkivanov.decompose.ComponentContext

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val onNavigateToAgentCreationScreen: () -> Unit
) : MainComponent, ComponentContext by componentContext {

    override fun onNavigateToAgentCreation() {
        onNavigateToAgentCreationScreen()
    }
}
