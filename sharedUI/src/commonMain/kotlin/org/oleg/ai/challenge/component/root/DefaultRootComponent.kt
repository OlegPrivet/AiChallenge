package org.oleg.ai.challenge.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popToFirst
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import org.oleg.ai.challenge.component.agentcreation.AgentCreationComponent
import org.oleg.ai.challenge.component.chat.ChatComponent
import org.oleg.ai.challenge.component.main.MainComponent
import org.oleg.ai.challenge.component.mcp.McpConnectionComponent

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val mainComponentFactory: (ComponentContext, onNavigateToMcp: () -> Unit) -> MainComponent,
    private val mcpConnectionComponentFactory: (ComponentContext) -> McpConnectionComponent
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val childStack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Main,
            handleBackButton = true,
            childFactory = ::createChild
        )

    @OptIn(DelicateDecomposeApi::class)
    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Main -> RootComponent.Child.MainChild(
                mainComponentFactory(context, ::navigateToMcp)
            )
            is Config.McpConnection -> RootComponent.Child.McpConnectionChild(
                mcpConnectionComponentFactory(context)
            )
        }

    override fun onBackClicked() {
        navigation.pop()
    }

    @OptIn(DelicateDecomposeApi::class)
    private fun navigateToMcp() {
        navigation.push(Config.McpConnection)
    }

    @Serializable
    private sealed class Config {
        @Serializable
        data object Main : Config()

        @Serializable
        data object McpConnection : Config()
    }
}
