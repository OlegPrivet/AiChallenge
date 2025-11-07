package org.oleg.ai.challenge.di

import com.arkivanov.decompose.ComponentContext
import org.koin.dsl.module
import org.oleg.ai.challenge.component.agentcreation.DefaultAgentCreationComponent
import org.oleg.ai.challenge.component.chat.DefaultChatComponent
import org.oleg.ai.challenge.component.main.DefaultMainComponent
import org.oleg.ai.challenge.component.root.DefaultRootComponent
import org.oleg.ai.challenge.component.root.RootComponent
import org.oleg.ai.challenge.data.AgentManager

val componentModule = module {
    // Singleton AgentManager
    single { AgentManager() }

    factory<RootComponent> { (componentContext: ComponentContext) ->
        val agentManager = get<AgentManager>()
        val chatApiService = get<org.oleg.ai.challenge.data.network.service.ChatApiService>()

        DefaultRootComponent(
            componentContext = componentContext,
            mainComponentFactory = { context, onNavigateToAgentCreation ->
                DefaultMainComponent(context, onNavigateToAgentCreation)
            },
            agentCreationComponentFactory = { context, onNavigateBack, onNavigateToChat ->
                DefaultAgentCreationComponent(
                    componentContext = context,
                    agentManager = agentManager,
                    onNavigateBack = onNavigateBack,
                    onNavigateToChat = onNavigateToChat
                )
            },
            chatComponentFactory = { context, onNavigateBack ->
                DefaultChatComponent(
                    componentContext = context,
                    chatApiService = chatApiService,
                    agentManager = agentManager,
                    onNavigateBack = onNavigateBack
                )
            }
        )
    }
}
