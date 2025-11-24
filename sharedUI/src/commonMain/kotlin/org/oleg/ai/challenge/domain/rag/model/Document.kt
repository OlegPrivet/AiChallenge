package org.oleg.ai.challenge.domain.rag.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * High-level document representation stored in the knowledge base.
 */
@Serializable
data class Document @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val title: String,
    val description: String? = null,
    val sourceType: KnowledgeSourceType,
    val uri: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val chunkingStrategyVersion: String,
    val embeddingModelVersion: String
)
