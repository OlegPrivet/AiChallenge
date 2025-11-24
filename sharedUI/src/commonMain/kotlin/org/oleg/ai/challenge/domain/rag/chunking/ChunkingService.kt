package org.oleg.ai.challenge.domain.rag.chunking

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk

data class ChunkRequest(
    val document: Document,
    val content: String,
    val embeddingModelVersion: String
)

/**
 * Coordinates chunk creation using pluggable strategies with batching support.
 */
class ChunkingService(
    private val defaultStrategy: ChunkingStrategy = RecursiveChunkingStrategy(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    suspend fun chunkDocument(
        request: ChunkRequest,
        strategy: ChunkingStrategy = defaultStrategy
    ): List<DocumentChunk> = withContext(dispatcher) {
        strategy.chunk(
            documentId = request.document.id,
            text = request.content,
            embeddingModelVersion = request.embeddingModelVersion
        )
    }.mapIndexed { index, chunk ->
        chunk.copy(
            id = buildChunkId(request.document.id, index),
            chunkIndex = index,
            chunkingStrategyVersion = strategy.version
        )
    }

    suspend fun chunkBatch(
        requests: List<ChunkRequest>,
        strategy: ChunkingStrategy = defaultStrategy,
        parallelism: Int = 4
    ): List<List<DocumentChunk>> = coroutineScope {
        val limitedParallelism = parallelism.coerceAtLeast(1)
        requests.chunked(limitedParallelism).flatMap { batch ->
            batch.map { req ->
                async(dispatcher) {
                    chunkDocument(req, strategy)
                }
            }.awaitAll()
        }
    }
}
