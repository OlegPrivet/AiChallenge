package org.oleg.ai.challenge.domain.rag.chunking

import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Sentence-based chunker that groups sentences up to a max size.
 */
class SentenceChunkingStrategy(
    private val maxCharacters: Int = 900,
    private val minCharacters: Int = 200,
    override val version: String = "sentence-1"
) : ChunkingStrategy {

    override val name: String = "sentence"

    @OptIn(ExperimentalTime::class)
    override suspend fun chunk(
        documentId: String,
        text: String,
        embeddingModelVersion: String
    ): List<DocumentChunk> {
        if (text.isBlank()) return emptyList()

        val sentences = splitIntoSentences(text)
        if (sentences.isEmpty()) return emptyList()

        val chunks = mutableListOf<DocumentChunk>()
        val now = Clock.System.now().toEpochMilliseconds()
        var buffer = StringBuilder()
        var index = 0

        for (sentence in sentences) {
            if (buffer.isNotEmpty() && buffer.length + sentence.length > maxCharacters) {
                val content = buffer.toString().trim()
                if (content.isNotEmpty()) {
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
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    index++
                    buffer = StringBuilder()
                }
            }
            if (buffer.isNotEmpty()) buffer.append(" ")
            buffer.append(sentence.trim())
        }

        // Flush remainder
        val remaining = buffer.toString().trim()
        if (remaining.isNotEmpty()) {
            chunks.add(
                DocumentChunk(
                    id = buildChunkId(documentId, index),
                    documentId = documentId,
                    content = remaining,
                    chunkIndex = index,
                    tokenCount = remaining.length,
                    embedding = null,
                    chunkingStrategyVersion = version,
                    embeddingModelVersion = embeddingModelVersion,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        // Merge very small chunks with previous one
        return mergeSmallChunks(chunks, minCharacters)
    }

    private fun splitIntoSentences(text: String): List<String> {
        val regex = Regex("(?<=[.!?])\\s+")
        return text.split(regex).filter { it.isNotBlank() }
    }

    private fun mergeSmallChunks(
        chunks: List<DocumentChunk>,
        minCharacters: Int
    ): List<DocumentChunk> {
        if (chunks.size <= 1) return chunks
        val merged = mutableListOf<DocumentChunk>()
        var buffer: DocumentChunk? = null
        var index = 0

        for (chunk in chunks) {
            if (buffer == null) {
                buffer = chunk.copy(chunkIndex = index)
                index++
                continue
            }

            val combinedLength = buffer.content.length + 1 + chunk.content.length
            if (buffer.content.length < minCharacters || chunk.content.length < minCharacters) {
                val combinedContent = "${buffer.content} ${chunk.content}".trim()
                buffer = buffer.copy(
                    content = combinedContent,
                    tokenCount = combinedContent.length
                )
            } else {
                merged.add(buffer)
                buffer = chunk.copy(chunkIndex = index)
                index++
            }
        }

        buffer?.let { merged.add(it) }
        return merged
    }
}
