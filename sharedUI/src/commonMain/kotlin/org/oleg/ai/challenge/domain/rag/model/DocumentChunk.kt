package org.oleg.ai.challenge.domain.rag.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.Serializable

/**
 * Individual chunk of a document used for retrieval and embedding.
 */
@Serializable
data class DocumentChunk @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val documentId: String,
    val content: String,
    val chunkIndex: Int,
    val tokenCount: Int? = null,
    val embedding: Embedding? = null,
    val chunkingStrategyVersion: String,
    val embeddingModelVersion: String,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
)
