package org.oleg.ai.challenge.data.mapper.rag

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.oleg.ai.challenge.data.database.entity.DocumentChunkEntity
import org.oleg.ai.challenge.data.database.entity.DocumentEntity
import org.oleg.ai.challenge.data.database.entity.DocumentWithChunksEntity
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.model.DocumentWithChunks
import org.oleg.ai.challenge.domain.rag.model.Embedding

private val ragJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val metadataSerializer = MapSerializer(String.serializer(), String.serializer())

internal fun Document.toEntity(): DocumentEntity = DocumentEntity(
    documentId = id,
    title = title,
    description = description,
    sourceType = sourceType,
    uri = uri,
    metadataJson = ragJson.encodeToString(metadataSerializer, metadata),
    createdAt = createdAt,
    updatedAt = updatedAt,
    chunkingStrategyVersion = chunkingStrategyVersion,
    embeddingModelVersion = embeddingModelVersion
)

private fun decodeMetadata(metadataJson: String): Map<String, String> =
    runCatching { ragJson.decodeFromString(metadataSerializer, metadataJson) }
        .getOrDefault(emptyMap())

internal fun DocumentEntity.toDomain(): Document = Document(
    id = documentId,
    title = title,
    description = description,
    sourceType = sourceType,
    uri = uri,
    metadata = decodeMetadata(metadataJson),
    createdAt = createdAt,
    updatedAt = updatedAt,
    chunkingStrategyVersion = chunkingStrategyVersion,
    embeddingModelVersion = embeddingModelVersion
)

internal fun DocumentChunk.toEntity(): DocumentChunkEntity = DocumentChunkEntity(
    chunkId = id,
    documentId = documentId,
    content = content,
    chunkIndex = chunkIndex,
    tokenCount = tokenCount,
    embeddingJson = embedding?.let { ragJson.encodeToString(Embedding.serializer(), it) },
    embeddingModelVersion = embeddingModelVersion,
    chunkingStrategyVersion = chunkingStrategyVersion,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun decodeEmbedding(embeddingJson: String?): Embedding? {
    if (embeddingJson.isNullOrBlank()) return null
    return runCatching { ragJson.decodeFromString(Embedding.serializer(), embeddingJson) }
        .getOrNull()
}

internal fun DocumentChunkEntity.toDomain(): DocumentChunk = DocumentChunk(
    id = chunkId,
    documentId = documentId,
    content = content,
    chunkIndex = chunkIndex,
    tokenCount = tokenCount,
    embedding = decodeEmbedding(embeddingJson),
    chunkingStrategyVersion = chunkingStrategyVersion,
    embeddingModelVersion = embeddingModelVersion,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun DocumentWithChunksEntity.toDomain(): DocumentWithChunks = DocumentWithChunks(
    document = document.toDomain(),
    chunks = chunks.sortedBy { it.chunkIndex }.map { it.toDomain() }
)
