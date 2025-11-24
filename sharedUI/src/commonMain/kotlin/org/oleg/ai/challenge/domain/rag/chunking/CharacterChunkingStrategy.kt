package org.oleg.ai.challenge.domain.rag.chunking

import kotlin.math.max
import kotlin.math.min
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Simple character-based chunker with optional overlap.
 */
class CharacterChunkingStrategy(
    private val chunkSize: Int = 500,
    private val chunkOverlap: Int = 50,
    override val version: String = "char-1"
) : ChunkingStrategy {

    override val name: String = "character"

    @OptIn(ExperimentalTime::class)
    override suspend fun chunk(
        documentId: String,
        text: String,
        embeddingModelVersion: String
    ): List<DocumentChunk> {
        if (text.isBlank()) return emptyList()

        val normalizedOverlap = max(0, min(chunkOverlap, chunkSize / 2))
        val chunks = mutableListOf<DocumentChunk>()
        var start = 0
        var index = 0

        while (start < text.length) {
            val endExclusive = min(text.length, start + chunkSize)
            val content = text.substring(start, endExclusive)

            chunks.add(
                DocumentChunk(
                    id = buildChunkId(documentId, index),
                    documentId = documentId,
                    content = content,
                    chunkIndex = index,
                    tokenCount = content.length,
                    embedding = null,
                    chunkingStrategyVersion = version,
                    embeddingModelVersion = embeddingModelVersion,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                )
            )

            index++
            start += (chunkSize - normalizedOverlap).coerceAtLeast(1)
        }

        return chunks
    }
}

internal fun buildChunkId(documentId: String, chunkIndex: Int): String =
    "$documentId::$chunkIndex"
