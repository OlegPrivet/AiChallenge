package org.oleg.ai.challenge.component.rag

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.rag.repository.DocumentIngestionRepository
import org.oleg.ai.challenge.domain.rag.chunking.ChunkingStrategy
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.KnowledgeSourceType
import org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DefaultDocumentManagementComponent(
    componentContext: ComponentContext,
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val ingestionRepository: DocumentIngestionRepository,
    private val defaultChunkingStrategy: ChunkingStrategy,
    private val filePickerService: org.oleg.ai.challenge.data.file.FilePickerService,
    private val onBack: () -> Unit
) : DocumentManagementComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _documents = MutableValue<List<DocumentUiModel>>(emptyList())
    override val documents: Value<List<DocumentUiModel>> = _documents

    private val _isIngesting = MutableValue(false)
    override val isIngesting: Value<Boolean> = _isIngesting

    private val _statusMessage = MutableValue("")
    override val statusMessage: Value<String> = _statusMessage

    private val _uploadProgress = MutableValue<UploadProgress>(UploadProgress.None)
    override val uploadProgress: Value<UploadProgress> = _uploadProgress

    init {
        knowledgeBaseRepository.observeDocuments()
            .onEach { docs ->
                // Load chunks for each document
                val documentsWithChunks = docs.map { doc ->
                    val chunks = try {
                        knowledgeBaseRepository.getChunks(doc.id)
                    } catch (e: Exception) {
                        emptyList() // Fallback to empty list if chunks can't be loaded
                    }
                    DocumentUiModel(
                        id = doc.id,
                        title = doc.title,
                        sourceType = doc.sourceType,
                        updatedAt = doc.updatedAt,
                        chunkingVersion = doc.chunkingStrategyVersion,
                        embeddingVersion = doc.embeddingModelVersion,
                        chunks = chunks
                    )
                }
                _documents.value = documentsWithChunks
            }
            .launchIn(scope)
    }

    override fun onIngest(
        title: String,
        description: String,
        content: String,
        sourceType: KnowledgeSourceType
    ) {
        if (title.isBlank() || content.isBlank()) {
            _statusMessage.value = "Title and content cannot be empty"
            return
        }
        scope.launch {
            ingestInternal(
                title = title,
                description = description,
                content = content,
                sourceType = sourceType
            )
        }
    }

    override fun onIngestSample() {
        scope.launch {
            ingestInternal(
                title = "Sample Guide",
                description = "Sample onboarding guide",
                content = SAMPLE_CONTENT,
                sourceType = KnowledgeSourceType.INTERNAL
            )
        }
    }

    override fun onUploadFiles() {
        scope.launch {
            try {
                val pickedFiles = filePickerService.pickFiles(
                    allowedExtensions = listOf("txt", "pdf", "md"),
                    allowMultiple = true
                )

                if (pickedFiles.isEmpty()) {
                    _statusMessage.value = "No files selected"
                    return@launch
                }

                _isIngesting.value = true
                _statusMessage.value = "Uploading ${pickedFiles.size} file(s)..."

                pickedFiles.forEachIndexed { index, file ->
                    _uploadProgress.value = UploadProgress.Uploading(
                        currentFile = index + 1,
                        totalFiles = pickedFiles.size,
                        fileName = file.name
                    )

                    val content = file.content.decodeToString()
                    val title = file.name.substringBeforeLast(".")

                    ingestInternal(
                        title = title,
                        description = "Uploaded from ${file.name}",
                        content = content,
                        sourceType = KnowledgeSourceType.USER
                    )
                }

                _uploadProgress.value = UploadProgress.None
                _statusMessage.value = "Successfully uploaded ${pickedFiles.size} file(s)"
            } catch (e: Exception) {
                _statusMessage.value = "Upload failed: ${e.message}"
            } finally {
                _isIngesting.value = false
                _uploadProgress.value = UploadProgress.None
            }
        }
    }

    override fun onDelete(documentId: String) {
        scope.launch {
            ingestionRepository.deleteDocument(documentId)
        }
    }

    override fun onBack() = onBack.invoke()

    @OptIn(ExperimentalTime::class)
    private suspend fun ingestInternal(
        title: String,
        description: String,
        content: String,
        sourceType: KnowledgeSourceType
    ) {
        _isIngesting.value = true
        _statusMessage.value = "Ingesting \"$title\"..."
        val now = Clock.System.now().toEpochMilliseconds()
        val documentId = "doc-${now}"
        val document = Document(
            id = documentId,
            title = title,
            description = description.ifBlank { null },
            sourceType = sourceType,
            metadata = emptyMap(),
            createdAt = now,
            updatedAt = now,
            chunkingStrategyVersion = defaultChunkingStrategy.version,
            embeddingModelVersion = BuildConfig.DEFAULT_EMBEDDING_MODEL
        )

        ingestionRepository.ingestDocument(
            document = document,
            content = content,
            strategy = defaultChunkingStrategy
        )
        _statusMessage.value = "Ingestion completed for \"$title\""
        _isIngesting.value = false
    }
}

private const val SAMPLE_CONTENT = """
Welcome to the AI Challenge knowledge base. This sample document demonstrates ingestion.
- Section 1: Setup steps
- Section 2: Running agents
- Section 3: Troubleshooting tips
"""
