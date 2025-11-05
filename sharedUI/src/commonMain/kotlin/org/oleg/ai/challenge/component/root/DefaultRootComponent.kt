package org.oleg.ai.challenge.component.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import org.oleg.ai.challenge.component.chat.ChatComponent
import org.oleg.ai.challenge.component.main.MainComponent

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val mainComponentFactory: (ComponentContext, onNavigateToChatWithPrompts: (String, String) -> Unit) -> MainComponent,
    private val chatComponentFactory: (ComponentContext, onNavigateBack: () -> Unit, systemPrompt: String, assistantPrompt: String) -> ChatComponent
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
                mainComponentFactory(context) { systemPrompt, assistantPrompt ->
                    navigation.push(Config.Chat(systemPrompt, assistantPrompt))
                }
            )
            is Config.Chat -> RootComponent.Child.ChatChild(
                chatComponentFactory(context, { navigation.pop() }, config.systemPrompt, config.assistantPrompt)
            )
        }

    @Serializable
    private sealed class Config {
        @Serializable
        data object Main : Config()

        @Serializable
        data class Chat(
            val systemPrompt: String = "",
            val assistantPrompt: String = ""
        ) : Config()
    }
}
