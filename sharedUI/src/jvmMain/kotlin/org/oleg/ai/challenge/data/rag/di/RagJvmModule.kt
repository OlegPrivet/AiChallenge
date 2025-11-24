package org.oleg.ai.challenge.data.rag.di

import co.touchlab.kermit.Logger
import org.koin.dsl.module
import org.oleg.ai.challenge.BuildConfig
import org.oleg.ai.challenge.data.rag.pipeline.HybridSearchPipeline
import org.oleg.ai.challenge.data.rag.pipeline.LexicalSearchPipeline
import org.oleg.ai.challenge.data.rag.pipeline.VectorSearchPipeline
import org.oleg.ai.challenge.data.rag.search.LuceneBm25SearchService
import org.oleg.ai.challenge.domain.rag.SearchPipeline
import org.oleg.ai.challenge.domain.rag.lexical.LexicalSearchService

val ragJvmModule = module {
    single<LexicalSearchService> {
        LuceneBm25SearchService(
            logger = Logger.withTag("LuceneBm25SearchService")
        )
    }

    single {
        LexicalSearchPipeline(
            lexicalSearchService = get(),
            knowledgeBaseRepository = get(),
            logger = Logger.withTag("LexicalSearchPipeline")
        )
    }

    single<SearchPipeline> {
        HybridSearchPipeline(
            vectorPipeline = get<VectorSearchPipeline>(),
            lexicalPipeline = get<LexicalSearchPipeline>(),
            embeddingService = get(),
            embeddingModel = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            embeddingModelVersion = BuildConfig.DEFAULT_EMBEDDING_MODEL,
            logger = Logger.withTag("HybridSearchPipeline")
        )
    }
}
