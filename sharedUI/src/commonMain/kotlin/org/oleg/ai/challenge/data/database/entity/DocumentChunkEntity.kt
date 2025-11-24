package org.oleg.ai.challenge.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted chunk of a document, optionally holding serialized embeddings.
 */
@Entity(
    tableName = "document_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["documentId"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["chunkId"], unique = true),
        Index(value = ["embeddingModelVersion"]),
        Index(value = ["chunkingStrategyVersion"])
    ]
)
data class DocumentChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chunkId: String,
    val documentId: String,
    val content: String,
    val chunkIndex: Int,
    val tokenCount: Int? = null,
    val embeddingJson: String? = null,
    val embeddingModelVersion: String,
    val chunkingStrategyVersion: String,
    val createdAt: Long,
    val updatedAt: Long
)
