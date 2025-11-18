package org.oleg.ai.challenge.component.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.component.main.MainComponent
import org.oleg.ai.challenge.component.mcp.McpConnectionComponent

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    sealed class Child {
        data class MainChild(val component: MainComponent) : Child()
        data class McpConnectionChild(val component: McpConnectionComponent) : Child()
    }
}
