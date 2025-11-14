package org.oleg.ai.challenge.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.oleg.ai.challenge.data.database.entity.ChatEntity

/**
 * Data Access Object for chat/conversation operations.
 */
@Dao
interface ChatDao {

    /**
     * Get all chats ordered by most recently updated first.
     */
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    /**
     * Get a specific chat by ID.
     */
    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    suspend fun getChatById(chatId: Long): ChatEntity?

    /**
     * Get a specific chat by ID as Flow (for reactive updates).
     */
    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    fun getChatByIdFlow(chatId: Long): Flow<ChatEntity?>

    /**
     * Insert a new chat and return its auto-generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    /**
     * Update an existing chat.
     */
    @Update
    suspend fun updateChat(chat: ChatEntity)

    /**
     * Delete a chat by ID (cascades to related messages and agents).
     */
    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: Long)

    /**
     * Delete a chat entity.
     */
    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    /**
     * Update chat name and updatedAt timestamp.
     */
    @Query("UPDATE chats SET chatName = :chatName, updatedAt = :updatedAt WHERE chatId = :chatId")
    suspend fun updateChatName(chatId: Long, chatName: String, updatedAt: Long)
}
