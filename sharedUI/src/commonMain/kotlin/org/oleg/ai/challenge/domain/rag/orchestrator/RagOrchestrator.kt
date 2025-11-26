package org.oleg.ai.challenge.domain.rag.orchestrator

import co.touchlab.kermit.Logger
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.oleg.ai.challenge.domain.rag.QueryEmbedding
import org.oleg.ai.challenge.domain.rag.SearchPipeline
import org.oleg.ai.challenge.domain.rag.agent.ConflictResolver
import org.oleg.ai.challenge.domain.rag.agent.MultiStepReasoner
import org.oleg.ai.challenge.domain.rag.agent.SourceValidator
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingRequest
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingService
import org.oleg.ai.challenge.domain.rag.model.Document
import org.oleg.ai.challenge.domain.rag.model.DocumentChunk
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult
import org.oleg.ai.challenge.domain.rag.agent.ExternalToolGateway
import org.oleg.ai.challenge.data.model.QueryHistory
import org.oleg.ai.challenge.data.repository.QueryHistoryRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Serializable
data class Citation(
    val index: Int,
    val documentId: String,
    val chunkIndex: Int?,
    val sourceTitle: String?
)

@Serializable
data class RetrievalTrace @OptIn(ExperimentalTime::class) constructor(
    val query: String,
    val embedding: QueryEmbedding,
    val results: List<RetrievalResult>,
    val topK: Int,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

data class RagResult(
    val context: String,
    val citations: List<Citation>,
    val results: List<RetrievalResult>,
    val trace: RetrievalTrace,
    val conflicts: List<org.oleg.ai.challenge.domain.rag.agent.Conflict> = emptyList()
)

/**
 * Orchestrates end-to-end retrieval for a query.
 */
class RagOrchestrator(
    private val searchPipeline: SearchPipeline,
    private val multiStepReasoner: MultiStepReasoner,
    private val sourceValidator: SourceValidator,
    private val conflictResolver: ConflictResolver,
    private val externalToolGateway: ExternalToolGateway,
    private val embeddingService: EmbeddingService,
    private val queryHistoryRepository: QueryHistoryRepository,
    private val defaultEmbeddingModel: String,
    private val embeddingModelVersion: String,
    private val logger: Logger = Logger.withTag("RagOrchestrator")
) {
    // Coroutine scope for async query logging (fire-and-forget)
    private val loggingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun retrieve(
        query: String,
        topK: Int = 6,
        filters: Map<String, String> = emptyMap(),
        similarityThreshold: Double = 0.7,
        hybridSearchEnabled: Boolean = false,
        hybridSearchWeight: Double? = null
    ): RagResult {
        val queryEmbedding = embedQuery(query)

        // Multi-step retrieval to gather broader context
        val multiStep = multiStepReasoner.run(query)

        // Merge multi-step results then run main search for refinement
        val primaryResults = searchPipeline.search(
            query = query,
            embedding = queryEmbedding,
            topK = topK,
            filters = filters
        )
        val combined = (multiStep.mergedResults + primaryResults)
            .groupBy { it.chunk.id }
            .map { (_, values) -> values.maxBy { it.score } }

        val validated = sourceValidator.validate(combined)
        val resolved = conflictResolver.resolve(validated)
        var finalResults = resolved.resolved.take(topK).map { it.result }

        // Knowledge gap detection: trigger external search if:
        // 1. No results found (existing behavior)
        // 2. Low relevance: max score < threshold
        // 3. Insufficient coverage: results < topK/2
        val hasKnowledgeGap = detectKnowledgeGap(finalResults, topK)
        if (hasKnowledgeGap) {
            logger.d { "Knowledge gap detected, fetching external context" }
            val external = fetchExternalContext(query, topK)
            if (external.isNotEmpty()) {
                // Merge external results with internal results
                finalResults = (finalResults + external).take(topK)
            }
        }

        val context = buildContext(finalResults)
        val citations = buildCitations(finalResults)
        val trace = RetrievalTrace(
            query = query,
            embedding = queryEmbedding,
            results = finalResults,
            topK = topK
        )

        val rerankedCount = finalResults.count { it.rerankedScore != null }
        logger.d {
            "RAG retrieved ${finalResults.size} chunks (validated=${validated.size}, conflicts=${resolved.conflicts.size}, reranked=$rerankedCount) for query='$query'"
        }

        // Log query history asynchronously (fire-and-forget)
        logQueryHistory(
            query = query,
            results = finalResults,
            citations = citations,
            topK = topK,
            similarityThreshold = similarityThreshold,
            hybridSearchEnabled = hybridSearchEnabled,
            hybridSearchWeight = hybridSearchWeight
        )

        return RagResult(
            context = context,
            citations = citations,
            results = finalResults,
            trace = trace,
            conflicts = resolved.conflicts
        )
    }

    private suspend fun embedQuery(query: String): QueryEmbedding {
        val embeddings = embeddingService.embed(
            EmbeddingRequest(
                texts = listOf(query),
                model = defaultEmbeddingModel,
                embeddingModelVersion = embeddingModelVersion,
                normalize = true
            )
        )
        val embedding = embeddings.firstOrNull() ?: error("Failed to embed query")
        return QueryEmbedding(
            vector = embedding.values,
            model = embedding.model,
            version = embedding.embeddingModelVersion
        )
    }

    private fun buildContext(results: List<RetrievalResult>): String {
        // Sort by reranked score if available, otherwise use original score
        val sorted = results.sortedByDescending { it.rerankedScore ?: it.score }
        return sorted.mapIndexed { index, result ->
            "[${index + 1}] ${result.chunk.content}"
        }.joinToString(separator = "\n\n")
    }

    private fun buildCitations(results: List<RetrievalResult>): List<Citation> {
        return results.mapIndexed { index, result ->
            Citation(
                index = index + 1,
                documentId = result.document.id,
                chunkIndex = result.chunk.chunkIndex,
                sourceTitle = result.document.title
            )
        }
    }

    /**
     * Detects if there's a knowledge gap in the retrieved results that should
     * trigger external tool usage.
     *
     * A knowledge gap is detected if:
     * 1. No results found
     * 2. Max relevance score is below threshold (low confidence)
     * 3. Insufficient coverage (too few results)
     */
    private fun detectKnowledgeGap(
        results: List<RetrievalResult>,
        topK: Int,
        relevanceThreshold: Double = 0.5,
        coverageThreshold: Double = 0.5
    ): Boolean {
        // No results
        if (results.isEmpty()) {
            logger.d { "Knowledge gap: no results found" }
            return true
        }

        // Low relevance: max score below threshold
        val maxScore = results.maxOfOrNull { it.score } ?: 0.0
        if (maxScore < relevanceThreshold) {
            logger.d { "Knowledge gap: low relevance (max score: $maxScore < $relevanceThreshold)" }
            return true
        }

        // Insufficient coverage: too few results
        val coverageRatio = results.size.toDouble() / topK.toDouble()
        if (coverageRatio < coverageThreshold) {
            logger.d { "Knowledge gap: insufficient coverage (${results.size}/$topK = $coverageRatio < $coverageThreshold)" }
            return true
        }

        return false
    }

    private suspend fun fetchExternalContext(query: String, topK: Int): List<RetrievalResult> {
        val externalSnippets = externalToolGateway.fetch(query)
        if (externalSnippets.isEmpty()) return emptyList()

        return externalSnippets.take(topK).mapIndexed { idx, text ->
            val doc = Document(
                id = "external-$idx",
                title = "External Source #${idx + 1}",
                sourceType = org.oleg.ai.challenge.domain.rag.model.KnowledgeSourceType.REMOTE,
                chunkingStrategyVersion = "external",
                embeddingModelVersion = embeddingModelVersion
            )
            val chunk = DocumentChunk(
                id = "external-$idx-chunk",
                documentId = doc.id,
                content = text,
                chunkIndex = idx,
                chunkingStrategyVersion = "external",
                embeddingModelVersion = embeddingModelVersion
            )
            RetrievalResult(
                document = doc,
                chunk = chunk,
                score = 0.0,
                rerankedScore = null
            )
        }
    }

    /**
     * Log query history asynchronously (fire-and-forget).
     * This does not block the main retrieval flow.
     */
    @OptIn(ExperimentalTime::class)
    private fun logQueryHistory(
        query: String,
        results: List<RetrievalResult>,
        citations: List<Citation>,
        topK: Int,
        similarityThreshold: Double,
        hybridSearchEnabled: Boolean,
        hybridSearchWeight: Double?
    ) {
        loggingScope.launch {
            try {
                val averageScore = if (results.isNotEmpty()) {
                    results.map { it.score }.average()
                } else {
                    0.0
                }
                val documentIds = results.map { it.document.id }.distinct()

                // Calculate reranker metrics
                val rerankedResults = results.filter { it.rerankedScore != null }
                val rerankerEnabled = rerankedResults.isNotEmpty()
                val averageScoreImprovement = if (rerankedResults.isNotEmpty()) {
                    rerankedResults.map { (it.rerankedScore!! - it.score) }.average()
                } else {
                    null
                }

                val queryHistory = QueryHistory(
                    queryText = query,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    resultsCount = results.size,
                    averageRelevanceScore = averageScore,
                    citationsCount = citations.size,
                    documentIds = documentIds,
                    topK = topK,
                    similarityThreshold = similarityThreshold,
                    hybridSearchEnabled = hybridSearchEnabled,
                    hybridSearchWeight = hybridSearchWeight,
                    rerankerEnabled = rerankerEnabled,
                    rerankerThreshold = null, // Will be added from settings if needed
                    resultsBeforeReranking = null, // Would require tracking before/after
                    averageScoreImprovement = averageScoreImprovement
                )

                queryHistoryRepository.saveQueryHistory(queryHistory)
                logger.d { "Logged query history for: '$query'" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to log query history for: '$query'" }
            }
        }
    }
}
