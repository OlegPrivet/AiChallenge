package org.oleg.ai.challenge.data.rag.agent

import org.oleg.ai.challenge.domain.rag.agent.ExternalToolGateway

/**
 * Default stub gateway that performs no external calls.
 */
class NoopExternalToolGateway : ExternalToolGateway {
    override suspend fun fetch(query: String): List<String> = emptyList()
}
