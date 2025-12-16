package org.oleg.ai.challenge.di

import com.arkivanov.decompose.ComponentContext
import org.koin.dsl.module
import org.oleg.ai.challenge.component.agentcreation.DefaultAgentCreationComponent
import org.oleg.ai.challenge.component.chat.DefaultChatComponent
import org.oleg.ai.challenge.component.main.DefaultMainComponent
import org.oleg.ai.challenge.component.mcp.DefaultMcpConnectionComponent
import org.oleg.ai.challenge.component.planner.DefaultPlannerComponent
import org.oleg.ai.challenge.component.rag.DefaultDocumentManagementComponent
import org.oleg.ai.challenge.component.rag.DefaultRagSettingsComponent
import org.oleg.ai.challenge.component.rag.DefaultStatisticsDashboardComponent
import org.oleg.ai.challenge.component.root.DefaultRootComponent
import org.oleg.ai.challenge.component.root.RootComponent
import org.oleg.ai.challenge.component.userprofile.DefaultUserProfileComponent
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
        val knowledgeBaseRepository = get<org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository>()
        val ingestionRepository = get<org.oleg.ai.challenge.data.rag.repository.DocumentIngestionRepository>()

        DefaultRootComponent(
            componentContext = componentContext,
            mainComponentFactory = { context, onNavigateToMcp ->
                DefaultMainComponent(
                    componentContext = context,
                    chatRepository = chatRepository,
                    onNavigateToMcp = onNavigateToMcp,
                    documentManagementComponentFactory = { docContext, onBack ->
                        DefaultDocumentManagementComponent(
                            componentContext = docContext,
                            knowledgeBaseRepository = knowledgeBaseRepository,
                            ingestionRepository = ingestionRepository,
                            defaultChunkingStrategy = get<org.oleg.ai.challenge.domain.rag.chunking.RecursiveChunkingStrategy>(),
                            filePickerService = get<org.oleg.ai.challenge.data.file.FilePickerService>(),
                            onBack = onBack
                        )
                    },
                    ragSettingsComponentFactory = { settingsContext, onBack ->
                        DefaultRagSettingsComponent(
                            componentContext = settingsContext,
                            ragSettingsService = get<org.oleg.ai.challenge.data.settings.RagSettingsService>(),
                            onBack = onBack
                        )
                    },
                    statisticsDashboardComponentFactory = { statsContext, onBack ->
                        DefaultStatisticsDashboardComponent(
                            componentContext = statsContext,
                            knowledgeBaseRepository = knowledgeBaseRepository,
                            queryHistoryRepository = get<org.oleg.ai.challenge.data.repository.QueryHistoryRepository>(),
                            onBack = onBack
                        )
                    },
                    userProfileComponentFactory = { userProfileContext, onBack ->
                        DefaultUserProfileComponent(
                            componentContext = userProfileContext,
                            userProfileService = get<org.oleg.ai.challenge.data.settings.UserProfileService>(),
                            onBack = onBack
                        )
                    },
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
                            commandOrchestrator = get(),
                            userProfileService = get<org.oleg.ai.challenge.data.settings.UserProfileService>(),
                            speechRecognizer = get<org.oleg.ai.challenge.data.audio.SpeechRecognizer>(),
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
