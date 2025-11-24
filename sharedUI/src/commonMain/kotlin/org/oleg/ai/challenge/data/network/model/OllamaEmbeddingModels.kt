package org.oleg.ai.challenge.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class OllamaEmbeddingRequest(
    val model: String,
    val input: List<String>,
    val options: Map<String, String>? = null
)

@Serializable
data class OllamaEmbeddingResponse(
    val model: String? = null,
    val embeddings: List<List<Float>>? = null
)
