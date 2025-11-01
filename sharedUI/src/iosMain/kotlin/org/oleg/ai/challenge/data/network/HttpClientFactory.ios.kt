package org.oleg.ai.challenge.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS-specific implementation using Darwin engine.
 */
internal actual fun createPlatformEngine(): HttpClientEngine {
    return Darwin.create {
        configureRequest {
            setTimeoutInterval(30.0)
            setAllowsCellularAccess(true)
        }

        configureSession {
            timeoutIntervalForRequest = 30.0
            timeoutIntervalForResource = 300.0
        }
    }
}
