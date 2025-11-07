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

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val mainComponentFactory: (ComponentContext, onNavigateToAgentCreation: () -> Unit) -> MainComponent,
    private val agentCreationComponentFactory: (ComponentContext, onNavigateBack: () -> Unit, onNavigateToChat: () -> Unit) -> AgentCreationComponent,
    private val chatComponentFactory: (ComponentContext, onNavigateBack: () -> Unit) -> ChatComponent
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
                mainComponentFactory(context) {
                    navigation.push(Config.AgentCreation)
                }
            )
            is Config.AgentCreation -> RootComponent.Child.AgentCreationChild(
                agentCreationComponentFactory(
                    context,
                    { navigation.pop() },
                    { navigation.push(Config.Chat) }
                )
            )
            is Config.Chat -> RootComponent.Child.ChatChild(
                chatComponentFactory(context) { navigation.popToFirst() }
            )
        }

    @Serializable
    private sealed class Config {
        @Serializable
        data object Main : Config()

        @Serializable
        data object AgentCreation : Config()

        @Serializable
        data object Chat : Config()
    }
}
