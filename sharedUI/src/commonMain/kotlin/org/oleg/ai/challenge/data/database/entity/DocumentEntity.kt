package org.oleg.ai.challenge.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.oleg.ai.challenge.domain.rag.model.KnowledgeSourceType

/**
 * Persisted document metadata used by the RAG pipeline.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val documentId: String,
    val title: String,
    val description: String? = null,
    val sourceType: KnowledgeSourceType,
    val uri: String? = null,
    val metadataJson: String = "{}",
    val createdAt: Long,
    val updatedAt: Long,
    val chunkingStrategyVersion: String,
    val embeddingModelVersion: String
)
