package org.oleg.ai.challenge.data.rag.di

import co.touchlab.kermit.Logger
import org.koin.dsl.module
import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.network.service.ChromaClient
import org.oleg.ai.challenge.data.network.service.OllamaEmbeddingClient
import org.oleg.ai.challenge.data.rag.agent.DefaultSourceValidator
import org.oleg.ai.challenge.data.rag.agent.HeuristicConflictResolver
import org.oleg.ai.challenge.data.rag.agent.LlmBasedConflictDetector
import org.oleg.ai.challenge.data.rag.agent.LlmBasedQueryDecomposer
import org.oleg.ai.challenge.data.rag.agent.SequentialMultiStepReasoner
import org.oleg.ai.challenge.data.rag.agent.SimpleQueryDecomposer
import org.oleg.ai.challenge.data.rag.agent.WebSearchExternalToolGateway
import org.oleg.ai.challenge.data.rag.embedding.OllamaEmbeddingService
import org.oleg.ai.challenge.data.rag.pipeline.RerankerPipeline
import org.oleg.ai.challenge.data.rag.pipeline.VectorSearchPipeline
import org.oleg.ai.challenge.data.rag.repository.DocumentIngestionRepository
import org.oleg.ai.challenge.data.rag.reranker.BM25Scorer
import org.oleg.ai.challenge.data.rag.reranker.FallbackBM25Scorer
import org.oleg.ai.challenge.data.rag.reranker.HybridRerankerConfig
import org.oleg.ai.challenge.data.rag.reranker.HybridRerankerService
import org.oleg.ai.challenge.data.rag.reranker.ScoreFusion
import org.oleg.ai.challenge.data.rag.reranker.SemanticSimilarityScorer
import org.oleg.ai.challenge.data.rag.search.NoopLexicalSearchService
import org.oleg.ai.challenge.data.rag.vectorstore.ChromaVectorStore
import org.oleg.ai.challenge.data.rag.vectorstore.ChromaVectorStoreService
import org.oleg.ai.challenge.domain.rag.SearchPipeline
import org.oleg.ai.challenge.domain.rag.agent.ConflictResolver
import org.oleg.ai.challenge.domain.rag.agent.ExternalToolGateway
import org.oleg.ai.challenge.domain.rag.agent.MultiStepReasoner
import org.oleg.ai.challenge.domain.rag.agent.QueryDecomposer
import org.oleg.ai.challenge.domain.rag.agent.SourceValidator
import org.oleg.ai.challenge.domain.rag.chunking.CharacterChunkingStrategy
import org.oleg.ai.challenge.domain.rag.chunking.ChunkingService
import org.oleg.ai.challenge.domain.rag.chunking.RecursiveChunkingStrategy
import org.oleg.ai.challenge.domain.rag.chunking.SentenceChunkingStrategy
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingCache
import org.oleg.ai.challenge.domain.rag.embedding.EmbeddingService
import org.oleg.ai.challenge.domain.rag.embedding.InMemoryEmbeddingCache
import org.oleg.ai.challenge.domain.rag.lexical.LexicalSearchService
import org.oleg.ai.challenge.domain.rag.orchestrator.RagOrchestrator
import org.oleg.ai.challenge.domain.rag.reranker.RerankerService
import org.oleg.ai.challenge.domain.rag.vector.VectorStore

val ragModule = module {
    single { CharacterChunkingStrategy() }
    single { SentenceChunkingStrategy() }
    single { RecursiveChunkingStrategy(sentenceStrategy = get(), fallbackCharStrategy = get()) }

    single {
        ChunkingService(
            defaultStrategy = get<RecursiveChunkingStrategy>()
        )
    }

    single<EmbeddingCache> { InMemoryEmbeddingCache() }

    single<EmbeddingService> {
        OllamaEmbeddingService(
            client = get<OllamaEmbeddingClient>(),
            cache = get(),
            logger = Logger.withTag("OllamaEmbeddingService")
        )
    }

    single {
        ChromaVectorStore(
            client = get<ChromaClient>(),
            defaultCollection = "rag",
            logger = Logger.withTag("ChromaVectorStore")
        )
    }

    single<VectorStore> { ChromaVectorStoreService(store = get()) }

    single<LexicalSearchService> { NoopLexicalSearchService() }

    // Hybrid Reranker components
    single<BM25Scorer> {
        // Platform-specific BM25 scorer will be provided by platform modules (JVM, iOS)
        // For now, use fallback scorer
        FallbackBM25Scorer()
    }

    single { SemanticSimilarityScorer(logger = Logger.withTag("SemanticSimilarityScorer")) }

    single { ScoreFusion(logger = Logger.withTag("ScoreFusion")) }

    single<RerankerService> {
        val ragSettingsService = get<org.oleg.ai.challenge.data.settings.RagSettingsService>()
        val settings = ragSettingsService.loadSettings()

        HybridRerankerService(
            embeddingService = get(),
            bm25Scorer = get(),
            semanticScorer = get(),
            scoreFusion = get(),
            config = HybridRerankerConfig(
                bm25Weight = settings.bm25Weight,
                semanticWeight = settings.semanticWeight,
                minimumScore = settings.rerankerThreshold
            ),
            embeddingModel = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            embeddingModelVersion = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            logger = Logger.withTag("HybridRerankerService")
        )
    }

    // Agent implementations with LLM enhancements
    single { SimpleQueryDecomposer() }  // Fallback decomposer
    single<QueryDecomposer> {
        LlmBasedQueryDecomposer(
            chatApiService = get(),
            fallbackDecomposer = get<SimpleQueryDecomposer>(),
            logger = Logger.withTag("LlmBasedQueryDecomposer")
        )
    }

    single { HeuristicConflictResolver() }  // Base resolver
    single<ConflictResolver> {
        LlmBasedConflictDetector(
            baseResolver = get<HeuristicConflictResolver>(),
            chatApiService = get(),
            enableSemanticAnalysis = true,
            logger = Logger.withTag("LlmBasedConflictDetector")
        )
    }

    single<SourceValidator> { DefaultSourceValidator() }

    single<ExternalToolGateway> {
        WebSearchExternalToolGateway(
            mcpClientService = get(),
            maxResults = 5,
            logger = Logger.withTag("WebSearchExternalToolGateway")
        )
    }

    single<MultiStepReasoner> {
        SequentialMultiStepReasoner(
            queryDecomposer = get(),
            searchPipeline = get(),
            topK = 4
        )
    }

    single {
        DocumentIngestionRepository(
            knowledgeBaseRepository = get(),
            chunkingService = get(),
            embeddingService = get(),
            vectorStore = get(),
            lexicalSearchService = get(),
            defaultEmbeddingModel = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            logger = Logger.withTag("DocumentIngestionRepository")
        )
    }

    single {
        VectorSearchPipeline(
            embeddingService = get(),
            vectorStore = get(),
            knowledgeBaseRepository = get(),
            embeddingModel = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            embeddingModelVersion = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            logger = Logger.withTag("VectorSearchPipeline")
        )
    }

    single<SearchPipeline> {
        val ragSettingsService = get<org.oleg.ai.challenge.data.settings.RagSettingsService>()
        val settings = ragSettingsService.loadSettings()
        val basePipeline = get<VectorSearchPipeline>()

        if (settings.enableReranker) {
            RerankerPipeline(
                innerPipeline = basePipeline,
                rerankerService = get(),
                relevanceThreshold = settings.rerankerThreshold.toDouble(),
                enabled = true,
                logger = Logger.withTag("RerankerPipeline")
            )
        } else {
            basePipeline
        }
    }

    single {
        RagOrchestrator(
            searchPipeline = get(),
            multiStepReasoner = get(),
            sourceValidator = get(),
            conflictResolver = get(),
            externalToolGateway = get(),
            embeddingService = get(),
            queryHistoryRepository = get(),
            defaultEmbeddingModel = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            embeddingModelVersion = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            logger = Logger.withTag("RagOrchestrator")
        )
    }
}
