package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.oleg.ai.challenge.data.network.model.ChromaAddRequest
import org.oleg.ai.challenge.data.network.model.ChromaCollection
import org.oleg.ai.challenge.data.network.model.ChromaCreateCollectionRequest
import org.oleg.ai.challenge.data.network.model.ChromaCreateCollectionResponse
import org.oleg.ai.challenge.data.network.model.ChromaDeleteRequest
import org.oleg.ai.challenge.data.network.model.ChromaQueryRequest
import org.oleg.ai.challenge.data.network.model.ChromaQueryResponse

data class ChromaRecord(
    val id: String,
    val embedding: List<Float>,
    val document: String,
    val metadata: Map<String, String>
)

class ChromaClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val logger: Logger = Logger.withTag("ChromaClient")
) {

    suspend fun ensureCollection(
        name: String,
        metadata: Map<String, String>? = null
    ): ChromaCollection {
        logger.d { "Ensuring Chroma collection: $name" }
        val response: HttpResponse = httpClient.post("${baseUrl.trimEnd('/')}/collections") {
            contentType(ContentType.Application.Json)
            setBody(
                ChromaCreateCollectionRequest(
                    name = name,
                    metadata = metadata
                )
            )
        }
        return response.body<ChromaCreateCollectionResponse>().collection
    }

    suspend fun addEmbeddings(
        collectionId: String,
        records: List<ChromaRecord>
    ) {
        if (records.isEmpty()) return
        logger.d { "Upserting ${records.size} vectors into collection $collectionId" }

        httpClient.post("${baseUrl.trimEnd('/')}/collections/$collectionId/add") {
            contentType(ContentType.Application.Json)
            setBody(
                ChromaAddRequest(
                    ids = records.map { it.id },
                    embeddings = records.map { it.embedding },
                    documents = records.map { it.document },
                    metadatas = records.map { it.metadata }
                )
            )
        }
    }

    suspend fun query(
        collectionId: String,
        embedding: List<Float>,
        topK: Int = 8,
        where: Map<String, String>? = null
    ): ChromaQueryResponse {
        logger.d { "Querying collection=$collectionId topK=$topK" }
        val response: HttpResponse = httpClient.post("${baseUrl.trimEnd('/')}/collections/$collectionId/query") {
            contentType(ContentType.Application.Json)
            setBody(
                ChromaQueryRequest(
                    queryEmbeddings = listOf(embedding),
                    topK = topK,
                    where = where
                )
            )
        }
        return response.body()
    }

    suspend fun health(): Boolean {
        return try {
            val response: HttpResponse = httpClient.get("${baseUrl.trimEnd('/')}/heartbeat") {
                parameter("ping", "1")
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            logger.w(e) { "Chroma health check failed" }
            false
        }
    }

    suspend fun deleteByDocumentId(
        collectionId: String,
        documentId: String
    ) {
        logger.d { "Deleting document=$documentId from Chroma collection=$collectionId" }
        httpClient.post("${baseUrl.trimEnd('/')}/collections/$collectionId/delete") {
            contentType(ContentType.Application.Json)
            setBody(
                ChromaDeleteRequest(
                    where = mapOf("documentId" to documentId)
                )
            )
        }
    }
}
