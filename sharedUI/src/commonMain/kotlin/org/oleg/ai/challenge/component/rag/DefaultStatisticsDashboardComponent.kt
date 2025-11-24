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
import org.oleg.ai.challenge.data.repository.QueryHistoryRepository
import org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository

class DefaultStatisticsDashboardComponent(
    componentContext: ComponentContext,
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val queryHistoryRepository: QueryHistoryRepository,
    private val onBack: () -> Unit
) : StatisticsDashboardComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _stats = MutableValue(KnowledgeBaseStats())
    override val stats: Value<KnowledgeBaseStats> = _stats

    private val _queryHistoryStats = MutableValue(QueryHistoryStats())
    override val queryHistoryStats: Value<QueryHistoryStats> = _queryHistoryStats

    init {
        // Observe documents and update stats
        knowledgeBaseRepository.observeDocuments()
            .onEach { documents ->
                scope.launch {
                    var totalChunks = 0
                    for (doc in documents) {
                        val chunks = knowledgeBaseRepository.getChunks(doc.id)
                        totalChunks += chunks.size
                    }
                    val lastUpdated = documents.maxOfOrNull { it.updatedAt }

                    _stats.value = KnowledgeBaseStats(
                        totalDocuments = documents.size,
                        totalChunks = totalChunks,
                        embeddingModel = BuildConfig.DEFAULT_EMBEDDING_MODEL,
                        defaultChunkingStrategy = documents.firstOrNull()?.chunkingStrategyVersion ?: "recursive",
                        lastUpdated = lastUpdated
                    )
                }
            }
            .launchIn(scope)

        // Observe query history and update stats
        queryHistoryRepository.queryHistory
            .onEach { queries ->
                scope.launch {
                    val statistics = queryHistoryRepository.getQueryStatistics()
                    _queryHistoryStats.value = QueryHistoryStats(
                        totalQueries = statistics.totalQueries,
                        averageRelevanceScore = statistics.averageRelevanceScore,
                        recentQueries = queries.take(10) // Show last 10 queries
                    )
                }
            }
            .launchIn(scope)
    }

    override fun refresh() {
        // Refresh is handled automatically by observeDocuments flow
    }

    override fun onBack() = onBack.invoke()
}
