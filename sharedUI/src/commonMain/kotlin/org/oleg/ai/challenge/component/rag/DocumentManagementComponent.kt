package org.oleg.ai.challenge.component.rag

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.model.KnowledgeSourceType

interface DocumentManagementComponent {
    val documents: Value<List<DocumentUiModel>>
    val isIngesting: Value<Boolean>
    val statusMessage: Value<String>
    val uploadProgress: Value<UploadProgress>

    fun onIngest(
        title: String,
        description: String,
        content: String,
        sourceType: KnowledgeSourceType
    )

    fun onIngestSample()

    fun onUploadFiles()

    fun onDelete(documentId: String)

    fun onBack()
}

sealed class UploadProgress {
    data object None : UploadProgress()
    data class Uploading(
        val currentFile: Int,
        val totalFiles: Int,
        val fileName: String
    ) : UploadProgress()
}

data class DocumentUiModel(
    val id: String,
    val title: String,
    val sourceType: KnowledgeSourceType,
    val updatedAt: Long,
    val chunkingVersion: String,
    val embeddingVersion: String,
    val chunks: List<DocumentChunk> = emptyList()
)
