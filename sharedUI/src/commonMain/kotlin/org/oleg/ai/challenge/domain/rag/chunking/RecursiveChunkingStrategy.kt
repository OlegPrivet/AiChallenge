package org.oleg.ai.challenge.domain.rag.chunking

import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Recursive strategy that prefers sentence chunks and falls back to smaller character slices.
 */
class RecursiveChunkingStrategy(
    private val sentenceStrategy: SentenceChunkingStrategy = SentenceChunkingStrategy(),
    private val fallbackCharStrategy: CharacterChunkingStrategy = CharacterChunkingStrategy(chunkSize = 500, chunkOverlap = 50),
    private val maxChunkCharacters: Int = 1200,
    override val version: String = "recursive-1"
) : ChunkingStrategy {

    override val name: String = "recursive"

    @OptIn(ExperimentalTime::class)
    override suspend fun chunk(
        documentId: String,
        text: String,
        embeddingModelVersion: String
    ): List<DocumentChunk> {
        if (text.isBlank()) return emptyList()

        val initialChunks = sentenceStrategy.chunk(documentId, text, embeddingModelVersion)
        if (initialChunks.isEmpty()) return fallbackCharStrategy.chunk(documentId, text, embeddingModelVersion)

        val now = Clock.System.now().toEpochMilliseconds()
        val adjusted = mutableListOf<DocumentChunk>()
        var index = 0

        for (chunk in initialChunks) {
            if (chunk.content.length <= maxChunkCharacters) {
                adjusted.add(
                    chunk.copy(
                        id = buildChunkId(documentId, index),
                        chunkIndex = index,
                        chunkingStrategyVersion = version,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                index++
            } else {
                val smaller = fallbackCharStrategy.chunk(documentId, chunk.content, embeddingModelVersion)
                smaller.forEach { small ->
                    adjusted.add(
                        small.copy(
                            id = buildChunkId(documentId, index),
                            chunkIndex = index,
                            chunkingStrategyVersion = version,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    index++
                }
            }
        }

        return adjusted
    }
}
