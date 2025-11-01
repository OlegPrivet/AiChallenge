package org.oleg.ai.challenge.data.network

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import java.util.concurrent.TimeUnit

/**
 * JVM (Desktop) specific implementation using OkHttp engine.
 * OkHttp provides efficient connection pooling and HTTP/2 support.
 */
internal actual fun createPlatformEngine(): HttpClientEngine {
    return OkHttp.create {
        config {
            // Connection pool configuration for efficient reuse
            connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 5,
                    keepAliveDuration = 5,
                    timeUnit = TimeUnit.MINUTES
                )
            )

            // Timeout configurations (complementary to Ktor's timeout plugin)
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            writeTimeout(60, TimeUnit.SECONDS)

            // Follow redirects
            followRedirects(true)
            followSslRedirects(true)

            // Retry on connection failure
            retryOnConnectionFailure(true)
        }
    }
}