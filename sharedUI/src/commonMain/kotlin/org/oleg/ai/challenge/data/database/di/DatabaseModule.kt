package org.oleg.ai.challenge.data.database.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import org.oleg.ai.challenge.data.database.AppDatabase
import org.oleg.ai.challenge.data.database.getDatabaseBuilder
import org.oleg.ai.challenge.data.repository.ChatRepository
import org.oleg.ai.challenge.data.repository.DefaultChatRepository
import org.oleg.ai.challenge.data.repository.DefaultKnowledgeBaseRepository
import org.oleg.ai.challenge.data.repository.DefaultMcpServerRepository
import org.oleg.ai.challenge.data.repository.DefaultQueryHistoryRepository
import org.oleg.ai.challenge.data.repository.McpServerRepository
import org.oleg.ai.challenge.data.repository.QueryHistoryRepository
import org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository

/**
 * Koin module for database and repository dependencies.
 */
val databaseModule = module {

    // Provide the AppDatabase singleton
    single<AppDatabase> {
        getDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
    }

    // Provide DAOs
    single { get<AppDatabase>().chatDao() }
    single { get<AppDatabase>().agentDao() }
    single { get<AppDatabase>().messageDao() }
    single { get<AppDatabase>().mcpServerDao() }
    single { get<AppDatabase>().documentDao() }
    single { get<AppDatabase>().queryHistoryDao() }

    // Provide repositories
    single<ChatRepository> {
        DefaultChatRepository(
            chatDao = get(),
            agentDao = get(),
            messageDao = get()
        )
    }

    single<McpServerRepository> {
        DefaultMcpServerRepository(
            mcpServerDao = get(),
        )
    }

    single<KnowledgeBaseRepository> {
        DefaultKnowledgeBaseRepository(
            documentDao = get()
        )
    }

    single<QueryHistoryRepository> {
        DefaultQueryHistoryRepository(
            queryHistoryDao = get()
        )
    }
}
