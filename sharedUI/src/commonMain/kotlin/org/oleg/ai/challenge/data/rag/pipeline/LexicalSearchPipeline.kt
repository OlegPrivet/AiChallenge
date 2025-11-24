package org.oleg.ai.challenge.data.rag.pipeline

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.domain.rag.QueryEmbedding
import org.oleg.ai.challenge.domain.rag.SearchPipeline
import org.oleg.ai.challenge.domain.rag.lexical.LexicalSearchService
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult
import org.oleg.ai.challenge.domain.rag.repository.KnowledgeBaseRepository

class LexicalSearchPipeline(
    private val lexicalSearchService: LexicalSearchService,
    private val knowledgeBaseRepository: KnowledgeBaseRepository,
    private val logger: Logger = Logger.withTag("LexicalSearchPipeline")
) : SearchPipeline {

    override suspend fun search(
        query: String,
        embedding: QueryEmbedding?,
        topK: Int,
        filters: Map<String, String>
    ): List<RetrievalResult> {
        val matches = lexicalSearchService.search(query, topK, filters)

        val documentCache = mutableMapOf<String, org.oleg.ai.challenge.domain.rag.model.DocumentWithChunks?>()
        return matches.mapNotNull { match ->
            val docWithChunks = documentCache.getOrPut(match.documentId) {
                knowledgeBaseRepository.getDocumentWithChunks(match.documentId)
            } ?: return@mapNotNull null

            val chunk = docWithChunks.chunks.firstOrNull { it.id == match.chunkId }
                ?: docWithChunks.chunks.firstOrNull { it.chunkIndex == match.chunkIndex }
                ?: return@mapNotNull null

            RetrievalResult(
                document = docWithChunks.document,
                chunk = chunk,
                score = match.score,
                rerankedScore = null
            )
        }.also { results ->
            logger.d { "Lexical pipeline returned ${results.size} results for query='$query'" }
        }
    }
}
