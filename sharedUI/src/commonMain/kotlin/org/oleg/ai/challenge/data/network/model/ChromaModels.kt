package org.oleg.ai.challenge.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChromaCollection(
    val id: String,
    val name: String
)

@Serializable
data class ChromaCreateCollectionRequest(
    val name: String,
    val metadata: Map<String, String>? = null
)

@Serializable
data class ChromaCreateCollectionResponse(
    val collection: ChromaCollection
)

@Serializable
data class ChromaAddRequest(
    @SerialName("ids")
    val ids: List<String>,
    @SerialName("embeddings")
    val embeddings: List<List<Float>>,
    @SerialName("documents")
    val documents: List<String>,
    @SerialName("metadatas")
    val metadatas: List<Map<String, String>>
)

@Serializable
data class ChromaQueryRequest(
    @SerialName("query_embeddings")
    val queryEmbeddings: List<List<Float>>,
    @SerialName("n_results")
    val topK: Int = 10,
    val where: Map<String, String>? = null,
    val include: List<String> = listOf("metadatas", "distances", "documents", "ids")
)

@Serializable
data class ChromaQueryResponse(
    val ids: List<List<String>>,
    val distances: List<List<Double>>? = null,
    val metadatas: List<List<Map<String, String>>>? = null,
    val documents: List<List<String>>? = null
)

@Serializable
data class ChromaDeleteRequest(
    val ids: List<String>? = null,
    val where: Map<String, String>? = null
)
