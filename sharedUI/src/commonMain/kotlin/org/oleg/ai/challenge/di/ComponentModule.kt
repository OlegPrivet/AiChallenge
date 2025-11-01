package org.oleg.ai.challenge.di

import com.arkivanov.decompose.ComponentContext
import org.koin.dsl.module
import org.oleg.ai.challenge.component.chat.DefaultChatComponent
import org.oleg.ai.challenge.component.main.DefaultMainComponent
import org.oleg.ai.challenge.component.root.DefaultRootComponent
import org.oleg.ai.challenge.component.root.RootComponent

val componentModule = module {
    factory<RootComponent> { (componentContext: ComponentContext) ->
        DefaultRootComponent(
            componentContext = componentContext,
            mainComponentFactory = { context, onNavigateToChat ->
                DefaultMainComponent(context, onNavigateToChat)
            },
            chatComponentFactory = { context, onNavigateBack ->
                DefaultChatComponent(
                    componentContext = context,
                    chatApiService = get(),
                    onNavigateBack = onNavigateBack
                )
            }
        )
    }
}
