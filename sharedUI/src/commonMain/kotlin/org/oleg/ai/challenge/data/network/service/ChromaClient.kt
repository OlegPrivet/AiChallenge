package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.oleg.ai.challenge.data.network.model.ChromaAddRequest
import org.oleg.ai.challenge.data.network.model.ChromaCountResponse
import org.oleg.ai.challenge.data.network.model.ChromaCreateCollectionRequest
import org.oleg.ai.challenge.data.network.model.ChromaCreateCollectionResponse
import org.oleg.ai.challenge.data.network.model.ChromaDeleteRequest
import org.oleg.ai.challenge.data.network.model.ChromaGetCollectionResponse
import org.oleg.ai.challenge.data.network.model.ChromaGetRequest
import org.oleg.ai.challenge.data.network.model.ChromaGetResult
import org.oleg.ai.challenge.data.network.model.ChromaListCollectionsResponse
import org.oleg.ai.challenge.data.network.model.ChromaQueryRequest
import org.oleg.ai.challenge.data.network.model.ChromaQueryResponse
import org.oleg.ai.challenge.data.network.model.ChromaUpdateRequest
import org.oleg.ai.challenge.data.network.model.ChromaUpsertRequest

/**
 * Data class for representing a Chroma record (embeddings with metadata).
 * Used by ChromaVectorStore for preparing data.
 */
data class ChromaRecord(
    val id: String,
    val embedding: List<Float>,
    val document: String,
    val metadata: Map<String, String>
)

/**
 * ChromaDB HTTP API client implementation.
 * Implements the ChromaDB REST API for vector database operations.
 *
 * @param httpClient Configured HTTP client for API requests
 * @param baseUrl Base URL of the ChromaDB server (e.g., "http://localhost:8000")
 * @param logger Logger instance for debugging
 */
class ChromaClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val logger: Logger = Logger.withTag("ChromaClient")
) {

    private val apiBase = baseUrl.trimEnd('/')

    // ========================================
    // COLLECTION MANAGEMENT METHODS
    // ========================================

    /**
     * Health check endpoint.
     * @return Timestamp or heartbeat value
     */
    suspend fun heartbeat(): Int {
        logger.d { "Heartbeat check" }
        return try {
            val response: HttpResponse = httpClient.get("$apiBase/heartbeat")
            response.status.value
        } catch (e: Exception) {
            logger.e(e) { "Heartbeat failed" }
            throw e
        }
    }

    /**
     * Lists all collections with optional pagination.
     * @param limit Maximum number of collections to return
     * @param offset Number of collections to skip
     * @return List of collection names
     */
    suspend fun listCollections(
        limit: Int? = null,
        offset: Int? = null
    ): List<String> {
        logger.d { "Listing collections: limit=$limit, offset=$offset" }
        val response: HttpResponse = httpClient.get("$apiBase/collections") {
            limit?.let { parameter("limit", it) }
            offset?.let { parameter("offset", it) }
        }
        val result = response.body<ChromaListCollectionsResponse>()
        return result.collections
    }

    /**
     * Counts total number of collections.
     * @return Total collection count
     */
    suspend fun countCollections(): Int {
        logger.d { "Counting collections" }
        val response: HttpResponse = httpClient.get("$apiBase/count_collections")
        val result = response.body<ChromaCountResponse>()
        return result.count
    }

    /**
     * Gets a collection by name.
     * @param name Collection name
     * @return Collection info or null if not found
     */
    suspend fun getCollection(name: String): ChromaGetCollectionResponse? {
        logger.d { "Getting collection: $name" }
        return try {
            val response: HttpResponse = httpClient.get("$apiBase/collections/$name")
            response.body<ChromaGetCollectionResponse>()
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            if (e.response.status.value == 404) {
                logger.d { "Collection $name not found" }
                null
            } else {
                logger.e(e) { "Error getting collection $name" }
                throw e
            }
        }
    }

    /**
     * Creates a new collection.
     * @param name Collection name
     * @param metadata Optional metadata
     * @param getOrCreate If true, returns existing collection if it exists
     * @return Collection info
     */
    suspend fun createCollection(
        name: String,
        metadata: Map<String, String>? = null,
        getOrCreate: Boolean = false
    ): ChromaCreateCollectionResponse {
        logger.d { "Creating collection: $name, getOrCreate=$getOrCreate" }
        return try {
            val response: HttpResponse = httpClient.post("$apiBase/collections") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChromaCreateCollectionRequest(
                        name = name,
                        metadata = metadata,
                        getOrCreate = getOrCreate
                    )
                )
            }
            val result = response.body<ChromaCreateCollectionResponse>()
            logger.d { "Collection created/retrieved: $name, id=${result.id}" }
            result
        } catch (e: Exception) {
            logger.e(e) { "Error creating collection $name" }
            throw e
        }
    }

    /**
     * Gets or creates a collection (idempotent).
     * Convenience method that wraps createCollection with getOrCreate=true.
     * @param name Collection name
     * @param metadata Optional metadata (only used when creating new collection)
     * @return Collection info (existing or newly created)
     */
    suspend fun getOrCreateCollection(
        name: String,
        metadata: Map<String, String>? = null
    ): ChromaCreateCollectionResponse {
        logger.d { "Get or create collection: $name" }
        return createCollection(name, metadata, getOrCreate = true)
    }

    /**
     * Deletes a collection by name.
     * @param name Collection name
     */
    suspend fun deleteCollection(name: String) {
        logger.d { "Deleting collection: $name" }
        try {
            httpClient.delete("$apiBase/collections/$name")
            logger.d { "Collection deleted: $name" }
        } catch (e: Exception) {
            logger.e(e) { "Error deleting collection $name" }
            throw e
        }
    }

    // ========================================
    // ITEM OPERATIONS (within collections)
    // ========================================

    /**
     * Adds new items to a collection.
     * Fails if any of the IDs already exist.
     * @param collectionId Collection ID
     * @param ids List of unique IDs
     * @param embeddings List of embedding vectors
     * @param documents Optional list of document texts
     * @param metadatas Optional list of metadata maps
     * @return Success status
     */
    suspend fun add(
        collectionId: String,
        ids: List<String>,
        embeddings: List<List<Float>>,
        documents: List<String>? = null,
        metadatas: List<Map<String, String>>? = null
    ): Boolean {
        logger.d { "Adding ${ids.size} items to collection $collectionId" }
        return try {
            httpClient.post("$apiBase/collections/$collectionId/add") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChromaAddRequest(
                        ids = ids,
                        embeddings = embeddings,
                        documents = documents ?: List(ids.size) { "" },
                        metadatas = metadatas ?: List(ids.size) { emptyMap() }
                    )
                )
            }
            logger.d { "Successfully added ${ids.size} items" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Error adding items to collection $collectionId" }
            throw e
        }
    }

    /**
     * Updates existing items in a collection.
     * Fails if any of the IDs don't exist.
     * @param collectionId Collection ID
     * @param ids List of existing IDs
     * @param embeddings Optional list of new embeddings
     * @param documents Optional list of new document texts
     * @param metadatas Optional list of new metadata maps
     * @return Success status
     */
    suspend fun update(
        collectionId: String,
        ids: List<String>,
        embeddings: List<List<Float>>? = null,
        documents: List<String>? = null,
        metadatas: List<Map<String, String>>? = null
    ): Boolean {
        logger.d { "Updating ${ids.size} items in collection $collectionId" }
        return try {
            httpClient.post("$apiBase/collections/$collectionId/update") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChromaUpdateRequest(
                        ids = ids,
                        embeddings = embeddings,
                        documents = documents,
                        metadatas = metadatas
                    )
                )
            }
            logger.d { "Successfully updated ${ids.size} items" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Error updating items in collection $collectionId" }
            throw e
        }
    }

    /**
     * Upserts items in a collection (add or update).
     * Automatically adds new items or updates existing ones.
     * @param collectionId Collection ID
     * @param ids List of IDs
     * @param embeddings List of embedding vectors
     * @param documents Optional list of document texts
     * @param metadatas Optional list of metadata maps
     * @return Success status
     */
    suspend fun upsert(
        collectionId: String,
        ids: List<String>,
        embeddings: List<List<Float>>,
        documents: List<String>? = null,
        metadatas: List<Map<String, String>>? = null
    ): Boolean {
        logger.d { "Upserting ${ids.size} items into collection $collectionId" }
        return try {
            httpClient.post("$apiBase/collections/$collectionId/upsert") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChromaUpsertRequest(
                        ids = ids,
                        embeddings = embeddings,
                        documents = documents,
                        metadatas = metadatas
                    )
                )
            }
            logger.d { "Successfully upserted ${ids.size} items" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Error upserting items to collection $collectionId" }
            throw e
        }
    }

    /**
     * Gets items from a collection by IDs or filters.
     * @param collectionId Collection ID
     * @param ids Optional list of specific IDs to retrieve
     * @param where Optional metadata filter
     * @param limit Optional max number of results
     * @param offset Optional number of results to skip
     * @param include Fields to include in response
     * @return Get result with requested items
     */
    suspend fun get(
        collectionId: String,
        ids: List<String>? = null,
        where: Map<String, String>? = null,
        limit: Int? = null,
        offset: Int? = null,
        include: List<String> = listOf("metadatas", "documents")
    ): ChromaGetResult {
        logger.d { "Getting items from collection $collectionId" }
        val response: HttpResponse = httpClient.post("$apiBase/collections/$collectionId/get") {
            contentType(ContentType.Application.Json)
            setBody(
                ChromaGetRequest(
                    ids = ids,
                    where = where,
                    limit = limit,
                    offset = offset,
                    include = include
                )
            )
        }
        return response.body()
    }

    /**
     * Queries a collection for similar vectors.
     * @param collectionId Collection ID
     * @param queryEmbeddings List of query vectors (typically one)
     * @param nResults Number of results per query
     * @param where Optional metadata filter
     * @param include Fields to include in response
     * @return Query results with similar items
     */
    suspend fun query(
        collectionId: String,
        queryEmbeddings: List<List<Float>>,
        nResults: Int = 10,
        where: Map<String, String>? = null,
        include: List<String> = listOf("metadatas", "documents", "distances")
    ): ChromaQueryResponse {
        logger.d { "Querying collection=$collectionId, nResults=$nResults" }
        val response: HttpResponse = httpClient.post("$apiBase/collections/$collectionId/query") {
            contentType(ContentType.Application.Json)
            setBody(
                ChromaQueryRequest(
                    queryEmbeddings = queryEmbeddings,
                    topK = nResults,
                    where = where,
                    include = include
                )
            )
        }
        return response.body()
    }

    /**
     * Deletes items from a collection.
     * @param collectionId Collection ID
     * @param ids Optional list of specific IDs to delete
     * @param where Optional metadata filter for deletion
     * @return Success status
     */
    suspend fun delete(
        collectionId: String,
        ids: List<String>? = null,
        where: Map<String, String>? = null
    ): Boolean {
        logger.d { "Deleting items from collection $collectionId" }
        return try {
            httpClient.post("$apiBase/collections/$collectionId/delete") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChromaDeleteRequest(
                        ids = ids,
                        where = where
                    )
                )
            }
            logger.d { "Successfully deleted items" }
            true
        } catch (e: Exception) {
            logger.e(e) { "Error deleting items from collection $collectionId" }
            throw e
        }
    }

    /**
     * Counts items in a collection.
     * @param collectionId Collection ID
     * @return Number of items
     */
    suspend fun count(collectionId: String): Int {
        logger.d { "Counting items in collection $collectionId" }
        val response: HttpResponse = httpClient.get("$apiBase/collections/$collectionId/count")
        val result = response.body<ChromaCountResponse>()
        return result.count
    }

    /**
     * Peeks at the first N items in a collection.
     * @param collectionId Collection ID
     * @param limit Number of items to peek at
     * @return Sample items
     */
    suspend fun peek(collectionId: String, limit: Int = 10): ChromaGetResult {
        logger.d { "Peeking at $limit items in collection $collectionId" }
        return get(
            collectionId = collectionId,
            limit = limit,
            include = listOf("metadatas", "documents", "embeddings")
        )
    }
}
