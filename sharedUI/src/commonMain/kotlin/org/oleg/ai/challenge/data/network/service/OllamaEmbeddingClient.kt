package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.oleg.ai.challenge.data.network.model.OllamaEmbeddingRequest
import org.oleg.ai.challenge.data.network.model.OllamaEmbeddingResponse

class OllamaEmbeddingClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val logger: Logger = Logger.withTag("OllamaEmbeddingClient"),
) {

    suspend fun embed(
        model: String,
        inputs: List<String>,
        options: Map<String, String>? = null,
    ): OllamaEmbeddingResponse {
        logger.d { "Requesting embeddings from Ollama model=$model, count=${inputs.size}" }
        val response = httpClient.post("${baseUrl.trimEnd('/')}/embed") {
            contentType(ContentType.Application.Json)
            setBody(
                OllamaEmbeddingRequest(
                    model = model,
                    input = inputs,
                    options = options
                )
            )
        }
        logger.d { response.body() }
        return response.body()
    }
}
