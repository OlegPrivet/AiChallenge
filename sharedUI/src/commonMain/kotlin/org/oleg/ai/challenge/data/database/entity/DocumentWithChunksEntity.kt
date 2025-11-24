package org.oleg.ai.challenge.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation grouping a document with its chunk rows.
 */
data class DocumentWithChunksEntity(
    @Embedded
    val document: DocumentEntity,
    @Relation(
        parentColumn = "documentId",
        entityColumn = "documentId"
    )
    val chunks: List<DocumentChunkEntity>
)
