package org.oleg.ai.challenge.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import org.oleg.ai.challenge.BuildConfig

/**
 * Factory for creating configured HttpClient instances.
 * Uses platform-specific engines (OkHttp for Android/JVM, Darwin for iOS).
 */
object HttpClientFactory {

    /**
     * Creates a configured HttpClient for API communication.
     *
     * @param apiKey The OpenRouter API key for authorization
     * @param enableLogging Whether to enable HTTP logging (default: true)
     * @param logLevel The logging level (default: LogLevel.INFO)
     * @return Configured HttpClient instance
     */
    private val customLogger: co.touchlab.kermit.Logger = co.touchlab.kermit.Logger.withTag("HttpClient")
    fun create(
        apiKey: String,
        enableLogging: Boolean = true,
        logLevel: LogLevel = LogLevel.INFO
    ): HttpClient {
        return HttpClient(getPlatformEngine()) {
            // JSON Content Negotiation
            install(ContentNegotiation) {
                json(json)
            }

            // Logging Plugin
            if (enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            customLogger.d(message)
                        }
                    }
                    level = logLevel
                    sanitizeHeader { header -> header == HttpHeaders.Authorization }
                }
            }

            // Timeout Configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000 // 60 seconds
                connectTimeoutMillis = 30_000 // 30 seconds
                socketTimeoutMillis = 60_000  // 60 seconds
            }

            // Default Request Configuration
            install(DefaultRequest) {
                url(BuildConfig.OPENROUTER_BASE_URL)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }

            // Expect Success (throws exception on non-2xx responses)
            expectSuccess = true
        }
    }

    /**
     * Expect/Actual function to get platform-specific HTTP engine.
     * Implementations provided in androidMain, jvmMain, and iosMain.
     */
    private fun getPlatformEngine(): HttpClientEngine = createPlatformEngine()
}

/**
 * Platform-specific function to create the appropriate HTTP engine.
 * - Android/JVM: OkHttp engine
 * - iOS: Darwin engine
 */
internal expect fun createPlatformEngine(): HttpClientEngine
