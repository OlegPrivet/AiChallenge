package org.oleg.ai.challenge.data.rag.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oleg.ai.challenge.domain.rag.agent.QueryDecomposer

/**
 * Naive query decomposer splitting by punctuation and conjunctions.
 */
class SimpleQueryDecomposer : QueryDecomposer {
    override suspend fun decompose(query: String): List<String> = withContext(Dispatchers.Default) {
        val normalized = query.trim()
        if (normalized.length < 80) return@withContext listOf(normalized)

        val segments = normalized.split(Regex("[.;]| и | and ", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        segments.ifEmpty { listOf(normalized) }
    }
}
