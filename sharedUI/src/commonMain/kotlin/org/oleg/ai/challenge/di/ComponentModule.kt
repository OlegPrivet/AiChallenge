package org.oleg.ai.challenge.di

import com.arkivanov.decompose.ComponentContext
import org.koin.dsl.module
import org.oleg.ai.challenge.component.agentcreation.DefaultAgentCreationComponent
import org.oleg.ai.challenge.component.chat.DefaultChatComponent
import org.oleg.ai.challenge.component.main.DefaultMainComponent
import org.oleg.ai.challenge.component.mcp.DefaultMcpConnectionComponent
import org.oleg.ai.challenge.component.planner.DefaultPlannerComponent
import org.oleg.ai.challenge.component.root.DefaultRootComponent
import org.oleg.ai.challenge.component.root.RootComponent
import org.oleg.ai.challenge.data.AgentManager

val componentModule = module {
    // Singleton AgentManager
    single { AgentManager() }

    factory<RootComponent> { (componentContext: ComponentContext) ->
        val agentManager = get<AgentManager>()
        val chatApiService = get<org.oleg.ai.challenge.data.network.service.ChatApiService>()
        val chatRepository = get<org.oleg.ai.challenge.data.repository.ChatRepository>()
        val mcpClientService = get<org.oleg.ai.challenge.data.network.service.McpClientService>()
        val mcpServerRepository = get<org.oleg.ai.challenge.data.repository.McpServerRepository>()
        val chatOrchestratorService = get<org.oleg.ai.challenge.data.network.service.ChatOrchestratorService>()

        DefaultRootComponent(
            componentContext = componentContext,
            mainComponentFactory = { context, onNavigateToMcp ->
                DefaultMainComponent(
                    componentContext = context,
                    chatRepository = chatRepository,
                    onNavigateToMcp = onNavigateToMcp,
                    agentCreationComponentFactory = { agentCreationContext, onAgentsCreated, onNavigateBack ->
                        DefaultAgentCreationComponent(
                            componentContext = agentCreationContext,
                            agentManager = agentManager,
                            onNavigateBack = onNavigateBack,
                            onAgentsCreated = onAgentsCreated
                        )
                    },
                    chatComponentFactory = { chatContext, chatId->
                        DefaultChatComponent(
                            componentContext = chatContext,
                            chatApiService = chatApiService,
                            chatRepository = chatRepository,
                            agentManager = agentManager,
                            chatOrchestratorService = chatOrchestratorService,
                            mcpClientService = mcpClientService,
                            chatId = chatId,
                        )
                    },
                    plannerComponentFactory = { plannerContext, onNavigateBack ->
                        DefaultPlannerComponent(
                            componentContext = plannerContext,
                            mcpClientService = mcpClientService,
                            chatOrchestratorService = chatOrchestratorService,
                            onNavigateBack = onNavigateBack
                        )
                    }
                )
            },
            mcpConnectionComponentFactory = { context ->
                DefaultMcpConnectionComponent(
                    componentContext = context,
                    mcpClientService = mcpClientService,
                    mcpServerRepository = mcpServerRepository,
                )
            }
        )
    }
}
