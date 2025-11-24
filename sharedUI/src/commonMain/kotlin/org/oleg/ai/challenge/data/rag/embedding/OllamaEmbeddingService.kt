package org.oleg.ai.challenge.data.rag.embedding

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oleg.ai.challenge.data.network.service.OllamaEmbeddingClient
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingCache
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingMath
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingRequest
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingService
import org.oleg.ai.challenge.domain.rag.embedding.InMemoryEmbeddingCache
import org.oleg.ai.challenge.domain.rag.model.Embedding
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class OllamaEmbeddingService(
    private val client: OllamaEmbeddingClient,
    private val cache: EmbeddingCache = InMemoryEmbeddingCache(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val logger: Logger = Logger.withTag("OllamaEmbeddingService")
) : EmbeddingService {

    @OptIn(ExperimentalTime::class)
    override suspend fun embed(request: EmbeddingRequest): List<Embedding> = withContext(dispatcher) {
        if (request.texts.isEmpty()) return@withContext emptyList()

        val results = MutableList<Embedding?>(request.texts.size) { null }
        val missing = mutableListOf<Pair<Int, String>>()

        request.texts.forEachIndexed { index, text ->
            val cached = cache.get(text, request.embeddingModelVersion)
            if (cached != null && (!request.normalize || cached.normalized)) {
                results[index] = cached
            } else {
                missing.add(index to text)
            }
        }

        missing.chunked(request.batchSize.coerceAtLeast(1)).forEach { batch ->
            val payload = batch.map { it.second }
            val response = client.embed(
                model = request.model,
                inputs = payload
            )

            if (response.embeddings.isNullOrEmpty()) return@withContext emptyList()

            response.embeddings.forEachIndexed { idx, values ->
                val processedValues = prepareValues(values, request)
                val embedding = Embedding(
                    values = processedValues,
                    model = response.model ?: "unknown",
                    embeddingModelVersion = request.embeddingModelVersion,
                    dimensions = processedValues.size,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    normalized = request.normalize
                )

                val targetIndex = batch[idx].first
                results[targetIndex] = embedding
                cache.put(batch[idx].second, request.embeddingModelVersion, embedding)
            }
        }

        results.mapIndexed { idx, embedding ->
            embedding ?: error("Missing embedding for index $idx")
        }
    }

    private fun prepareValues(values: List<Float>, request: EmbeddingRequest): List<Float> {
        var processed = values
        if (request.useFp16Quantization) {
            processed = EmbeddingMath.quantizeFp16(processed)
        }
        if (request.normalize) {
            processed = EmbeddingMath.l2Normalize(processed)
        }
        return processed
    }
}
