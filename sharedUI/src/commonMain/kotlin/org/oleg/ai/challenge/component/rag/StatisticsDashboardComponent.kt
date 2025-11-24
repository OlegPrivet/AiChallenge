package org.oleg.ai.challenge.component.rag

import com.arkivanov.decompose.value.Value
import org.oleg.ai.challenge.data.model.QueryHistory

data class KnowledgeBaseStats(
    val totalDocuments: Int = 0,
    val totalChunks: Int = 0,
    val embeddingModel: String = "nomic-embed-text",
    val defaultChunkingStrategy: String = "recursive",
    val lastUpdated: Long? = null
)

data class QueryHistoryStats(
    val totalQueries: Int = 0,
    val averageRelevanceScore: Double = 0.0,
    val recentQueries: List<QueryHistory> = emptyList()
)

interface StatisticsDashboardComponent {
    val stats: Value<KnowledgeBaseStats>
    val queryHistoryStats: Value<QueryHistoryStats>

    fun refresh()
    fun onBack()
}
