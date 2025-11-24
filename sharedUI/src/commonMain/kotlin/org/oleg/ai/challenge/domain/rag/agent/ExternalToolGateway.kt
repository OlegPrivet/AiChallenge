package org.oleg.ai.challenge.domain.rag.agent

/**
 * Optional external tool integration (web search, APIs) to fill knowledge gaps.
 */
interface ExternalToolGateway {
    suspend fun fetch(query: String): List<String>
}
