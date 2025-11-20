package org.oleg.ai.challenge.data.network.di

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import org.koin.core.module.Module
import org.koin.dsl.module
import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.network.HttpClientFactory
import org.oleg.ai.challenge.data.network.service.ChatApiService
import org.oleg.ai.challenge.data.network.service.ChatApiServiceImpl
import org.oleg.ai.challenge.data.network.service.ChatOrchestratorService
import org.oleg.ai.challenge.data.network.service.McpClientService
import org.oleg.ai.challenge.data.network.service.ToolValidationService

/**
 * Koin module providing network-related dependencies.
 */
val networkModule: Module = module {
    // Provide HttpClient as a singleton
    single<HttpClient> {
        HttpClientFactory.create(
            apiKey = BuildConfig.OPENROUTER_API_KEY,
            enableLogging = true,
            logLevel = LogLevel.ALL
        )
    }

    // Provide ChatApiService as a singleton
    single<ChatApiService> {
        ChatApiServiceImpl(
            httpClient = get(),
            mcpClientService = get(),
            logger = Logger.withTag("ChatApiService")
        )
    }

    // Provide McpClientService as a singleton
    single<McpClientService> {
        McpClientService(
            customLogger = Logger.withTag("McpClientService")
        )
    }

    // Provide ToolValidationService as a singleton
    single<ToolValidationService> {
        ToolValidationService(
            logger = Logger.withTag("ToolValidationService")
        )
    }

    // Provide ChatOrchestratorService as a singleton
    single<ChatOrchestratorService> {
        ChatOrchestratorService(
            chatApiService = get(),
            mcpClientService = get(),
            toolValidationService = get(),
            logger = Logger.withTag("ChatOrchestratorService")
        )
    }
}
