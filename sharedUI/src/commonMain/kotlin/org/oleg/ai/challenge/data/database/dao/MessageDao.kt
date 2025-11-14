package org.oleg.ai.challenge.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.oleg.ai.challenge.data.database.entity.MessageEntity

/**
 * Data Access Object for message operations.
 */
@Dao
interface MessageDao {

    /**
     * Get all messages for a specific chat ordered by timestamp ascending.
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    /**
     * Get all messages for a specific chat (suspend version).
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatSuspend(chatId: Long): List<MessageEntity>

    /**
     * Get the first user message for a chat (for setting chat name).
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isFromUser = 1 AND isVisibleInUI = 1 ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstUserMessage(chatId: Long): MessageEntity?

    /**
     * Get message count for a chat.
     */
    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun getMessageCount(chatId: Long): Int

    /**
     * Insert a single message.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Insert multiple messages.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>): List<Long>

    /**
     * Delete all messages for a specific chat.
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: Long)

    /**
     * Delete a specific message by its database ID.
     */
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    /**
     * Delete a specific message by its messageId.
     */
    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessageByMessageId(messageId: String)
}
