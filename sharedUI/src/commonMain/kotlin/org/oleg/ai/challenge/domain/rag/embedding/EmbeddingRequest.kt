package org.oleg.ai.challenge.domain.rag.embedding

import org.oleg.ai.challenge.domain.rag.model.Embedding

data class EmbeddingRequest(
    val texts: List<String>,
    val model: String,
    val embeddingModelVersion: String,
    val normalize: Boolean = true,
    val useFp16Quantization: Boolean = false,
    val batchSize: Int = 8
)

data class EmbeddingResult(
    val embeddings: List<Embedding>,
    val model: String
)
