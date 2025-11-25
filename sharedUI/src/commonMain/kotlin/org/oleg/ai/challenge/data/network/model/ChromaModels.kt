package org.oleg.ai.challenge.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChromaCreateCollectionRequest(
    val name: String,
    val metadata: Map<String, String>? = null,
    @SerialName("get_or_create")
    val getOrCreate: Boolean = false
)

@Serializable
data class ChromaCreateCollectionResponse(
    val id: String,
    val name: String,
    val metadata: Map<String, String>? = null
)

@Serializable
data class ChromaListCollectionsResponse(
    val collections: List<String>
)

@Serializable
data class ChromaGetCollectionResponse(
    val id: String,
    val name: String,
    val metadata: Map<String, String>? = null
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

@Serializable
data class ChromaUpsertRequest(
    @SerialName("ids")
    val ids: List<String>,
    @SerialName("embeddings")
    val embeddings: List<List<Float>>,
    @SerialName("documents")
    val documents: List<String>? = null,
    @SerialName("metadatas")
    val metadatas: List<Map<String, String>>? = null
)

@Serializable
data class ChromaUpdateRequest(
    @SerialName("ids")
    val ids: List<String>,
    @SerialName("embeddings")
    val embeddings: List<List<Float>>? = null,
    @SerialName("documents")
    val documents: List<String>? = null,
    @SerialName("metadatas")
    val metadatas: List<Map<String, String>>? = null
)

@Serializable
data class ChromaGetRequest(
    @SerialName("ids")
    val ids: List<String>? = null,
    @SerialName("where")
    val where: Map<String, String>? = null,
    @SerialName("limit")
    val limit: Int? = null,
    @SerialName("offset")
    val offset: Int? = null,
    @SerialName("include")
    val include: List<String> = listOf("metadatas", "documents")
)

@Serializable
data class ChromaGetResult(
    val ids: List<String>,
    val embeddings: List<List<Float>>? = null,
    val documents: List<String>? = null,
    val metadatas: List<Map<String, String>>? = null
)

@Serializable
data class ChromaCountResponse(
    val count: Int
)
