package org.oleg.ai.challenge.domain.rag.model

/**
 * Describes where a document originated from to help with validation and ranking.
 */
enum class KnowledgeSourceType {
    USER,
    INTERNAL,
    REMOTE,
    CACHED
}
