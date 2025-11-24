package org.oleg.ai.challenge.domain.rag.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.Serializable

/**
 * Embedding representation for text chunks/documents.
 */
@Serializable
data class Embedding @OptIn(ExperimentalTime::class) constructor(
    val values: List<Float>,
    val model: String,
    val embeddingModelVersion: String,
    val dimensions: Int = values.size,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val normalized: Boolean = false
)
