package org.oleg.ai.challenge.data.repository

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.oleg.ai.challenge.data.database.dao.AgentDao
import org.oleg.ai.challenge.data.database.dao.ChatDao
import org.oleg.ai.challenge.data.database.dao.MessageDao
import org.oleg.ai.challenge.data.database.entity.ChatEntity
import org.oleg.ai.challenge.data.mapper.AgentMapper
import org.oleg.ai.challenge.data.mapper.AgentMapper.toDomain
import org.oleg.ai.challenge.data.mapper.AgentMapper.toEntity
import org.oleg.ai.challenge.data.mapper.ChatMessageMapper
import org.oleg.ai.challenge.data.mapper.ChatMessageMapper.toDomain
import org.oleg.ai.challenge.data.mapper.ChatMessageMapper.toEntity
import org.oleg.ai.challenge.data.mapper.ConversationMapper.toDomain
import org.oleg.ai.challenge.data.mapper.ConversationMapper.toEntity
import org.oleg.ai.challenge.data.model.Agent
import org.oleg.ai.challenge.data.model.ChatMessage
import org.oleg.ai.challenge.data.model.Conversation

/**
 * Default implementation of ChatRepository using Room DAOs.
 */
class DefaultChatRepository(
    private val chatDao: ChatDao,
    private val agentDao: AgentDao,
    private val messageDao: MessageDao
) : ChatRepository {

    override fun getAllConversations(): Flow<List<Conversation>> {
        return chatDao.getAllChats().map { entities ->
            entities.toDomain()
        }
    }

    override suspend fun getConversationById(chatId: Long): Conversation? {
        return chatDao.getChatById(chatId)?.toDomain()
    }

    override fun getMessagesForChat(chatId: Long): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForChat(chatId).map { entities ->
            entities.toDomain()
        }
    }

    override suspend fun getMessagesForChatSuspend(chatId: Long): List<ChatMessage> {
        return messageDao.getMessagesForChatSuspend(chatId).toDomain()
    }

    override fun getAgentsForChat(chatId: Long): Flow<List<Agent>> {
        return agentDao.getAgentsForChat(chatId).map { entities ->
            entities.toDomain()
        }
    }

    override suspend fun getAgentsForChatSuspend(chatId: Long): List<Agent> {
        return agentDao.getAgentsForChatSuspend(chatId).toDomain()
    }

    override suspend fun getMainAgent(chatId: Long): Agent? {
        return agentDao.getMainAgentForChat(chatId)?.toDomain()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun createChat(name: String): Long {
        val chatEntity = ChatEntity(
            chatName = name,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        return chatDao.insertChat(chatEntity)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun updateChatName(chatId: Long, name: String) {
        // Truncate to 50 chars with "..." if longer
        val truncatedName = if (name.length > 50) {
            name.take(50) + "..."
        } else {
            name
        }

        chatDao.updateChatName(
            chatId = chatId,
            chatName = truncatedName,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun saveMessage(chatId: Long, message: ChatMessage) {
        messageDao.insertMessage(message.toEntity(chatId))
    }

    override suspend fun saveMessages(chatId: Long, messages: List<ChatMessage>) {
        messageDao.insertMessages(messages.toEntity(chatId))
    }

    override suspend fun saveAgent(
        chatId: Long,
        agent: Agent,
        isMain: Boolean,
        parentAgentId: String?
    ) {
        agentDao.insertAgent(
            agent.toEntity(
                chatId = chatId,
                isMain = isMain,
                parentAgentId = parentAgentId
            )
        )
    }

    override suspend fun saveAgents(
        chatId: Long,
        mainAgent: Agent,
        subAgents: List<Agent>
    ) {
        val agentEntities = mutableListOf(
            // Main agent
            mainAgent.toEntity(chatId = chatId, isMain = true)
        )

        // Add subagents with parentAgentId pointing to main agent
        subAgents.forEach { subAgent ->
            agentEntities.add(
                subAgent.toEntity(
                    chatId = chatId,
                    isMain = false,
                    parentAgentId = mainAgent.id
                )
            )
        }

        agentDao.insertAgents(agentEntities)
    }

    override suspend fun deleteChat(chatId: Long) {
        chatDao.deleteChat(chatId)
        // Cascade delete will handle messages and agents
    }

    override suspend fun updateChatNameFromFirstMessage(chatId: Long) {
        // Get the chat to check its current name
        val chat = chatDao.getChatById(chatId) ?: return

        // Only update if chat name is still default
        if (chat.chatName == "New chat") {
            // Get the first user message
            val firstMessage = messageDao.getFirstUserMessage(chatId)

            if (firstMessage != null && firstMessage.text.isNotBlank()) {
                updateChatName(chatId, firstMessage.text)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun updateChatRagMode(chatId: Long, isRagEnabled: Boolean) {
        chatDao.updateChatRagMode(
            chatId = chatId,
            isRagEnabled = isRagEnabled,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }
}
