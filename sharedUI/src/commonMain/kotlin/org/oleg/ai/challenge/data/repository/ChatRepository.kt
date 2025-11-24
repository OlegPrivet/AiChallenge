package org.oleg.ai.challenge.data.repository

import kotlinx.coroutines.flow.Flow
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.Conversation

/**
 * Repository interface for chat/conversation operations.
 *
 * Provides a clean abstraction over the database layer for managing
 * conversations, messages, and agents.
 */
interface ChatRepository {

    /**
     * Get all conversations ordered by most recently updated.
     */
    fun getAllConversations(): Flow<List<Conversation>>

    /**
     * Get a specific conversation by ID.
     */
    suspend fun getConversationById(chatId: Long): Conversation?

    /**
     * Get all messages for a specific chat.
     */
    fun getMessagesForChat(chatId: Long): Flow<List<ChatMessage>>

    /**
     * Get messages for a chat (suspend version).
     */
    suspend fun getMessagesForChatSuspend(chatId: Long): List<ChatMessage>

    /**
     * Get all agents for a specific chat.
     */
    fun getAgentsForChat(chatId: Long): Flow<List<Agent>>

    /**
     * Get agents for a chat (suspend version).
     */
    suspend fun getAgentsForChatSuspend(chatId: Long): List<Agent>

    /**
     * Get the main agent for a chat.
     */
    suspend fun getMainAgent(chatId: Long): Agent?

    /**
     * Create a new conversation/chat and return its ID.
     *
     * @param name Optional name for the chat (defaults to "New chat")
     * @return The auto-generated chat ID
     */
    suspend fun createChat(name: String = "New chat"): Long

    /**
     * Update the chat name (typically called after first message).
     *
     * @param chatId The chat ID to update
     * @param name The new chat name (will be truncated to 50 chars with "..." if longer)
     */
    suspend fun updateChatName(chatId: Long, name: String)

    /**
     * Save a single message to the database.
     *
     * @param chatId The chat this message belongs to
     * @param message The message to save
     */
    suspend fun saveMessage(chatId: Long, message: ChatMessage)

    /**
     * Save multiple messages to the database.
     *
     * @param chatId The chat these messages belong to
     * @param messages The messages to save
     */
    suspend fun saveMessages(chatId: Long, messages: List<ChatMessage>)

    /**
     * Save a single agent to the database.
     *
     * @param chatId The chat this agent belongs to
     * @param agent The agent to save
     * @param isMain Whether this is the main chat agent
     * @param parentAgentId Optional parent agent ID for subagents
     */
    suspend fun saveAgent(
        chatId: Long,
        agent: Agent,
        isMain: Boolean = false,
        parentAgentId: String? = null
    )

    /**
     * Save multiple agents to the database.
     *
     * @param chatId The chat these agents belong to
     * @param mainAgent The main agent for the chat
     * @param subAgents List of subagents (optional)
     */
    suspend fun saveAgents(
        chatId: Long,
        mainAgent: Agent,
        subAgents: List<Agent> = emptyList()
    )

    /**
     * Delete a conversation and all its associated messages and agents.
     *
     * @param chatId The chat ID to delete
     */
    suspend fun deleteChat(chatId: Long)

    /**
     * Check if this is the first user message in a chat, and if so, update the chat name.
     * This should be called after saving a new user message.
     *
     * @param chatId The chat ID
     */
    suspend fun updateChatNameFromFirstMessage(chatId: Long)

    /**
     * Update the RAG mode for a specific chat.
     *
     * @param chatId The chat ID to update
     * @param isRagEnabled Whether RAG mode should be enabled
     */
    suspend fun updateChatRagMode(chatId: Long, isRagEnabled: Boolean)
}
