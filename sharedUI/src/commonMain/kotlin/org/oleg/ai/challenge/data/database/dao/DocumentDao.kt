package org.oleg.ai.challenge.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.oleg.ai.challenge.data.database.entity.DocumentChunkEntity
import org.oleg.ai.challenge.data.database.entity.DocumentEntity
import org.oleg.ai.challenge.data.database.entity.DocumentWithChunksEntity

/**
 * DAO for managing persisted documents and their chunks.
 */
@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun observeDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE documentId = :documentId")
    suspend fun getDocument(documentId: String): DocumentEntity?

    @Transaction
    @Query("SELECT * FROM documents WHERE documentId = :documentId")
    suspend fun getDocumentWithChunks(documentId: String): DocumentWithChunksEntity?

    @Query("SELECT * FROM document_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunks(documentId: String): List<DocumentChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocuments(documents: List<DocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChunks(chunks: List<DocumentChunkEntity>)

    @Query("DELETE FROM documents WHERE documentId = :documentId")
    suspend fun deleteDocument(documentId: String)

    @Query("DELETE FROM document_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksForDocument(documentId: String)

    @Transaction
    suspend fun replaceDocumentWithChunks(
        document: DocumentEntity,
        chunks: List<DocumentChunkEntity>
    ) {
        upsertDocument(document)
        deleteChunksForDocument(document.documentId)
        if (chunks.isNotEmpty()) {
            upsertChunks(chunks)
        }
    }

    @Query(
        "SELECT DISTINCT documentId FROM document_chunks " +
            "WHERE embeddingModelVersion != :expectedVersion"
    )
    suspend fun findDocumentsWithOutdatedEmbeddings(expectedVersion: String): List<String>

    @Query(
        "SELECT DISTINCT documentId FROM document_chunks " +
            "WHERE chunkingStrategyVersion != :expectedVersion"
    )
    suspend fun findDocumentsWithOutdatedChunking(expectedVersion: String): List<String>
}
