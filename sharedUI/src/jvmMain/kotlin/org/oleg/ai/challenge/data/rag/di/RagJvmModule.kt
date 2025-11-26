package org.oleg.ai.challenge.data.rag.di

import co.touchlab.kermit.Logger
import org.koin.dsl.module
import org.oleg.ai.challenge.data.rag.reranker.BM25Scorer
import org.oleg.ai.challenge.data.rag.reranker.LuceneBM25Scorer
import org.oleg.ai.challenge.data.rag.search.LuceneBm25SearchService
import org.oleg.ai.challenge.domain.rag.lexical.LexicalSearchService

val ragJvmModule = module {
    single<LexicalSearchService> {
        LuceneBm25SearchService(
            logger = Logger.withTag("LuceneBm25SearchService")
        )
    }

    // Override the BM25Scorer with Lucene-specific implementation
    single<BM25Scorer> {
        LuceneBM25Scorer(
            lexicalSearchService = get(),
            logger = Logger.withTag("LuceneBM25Scorer")
        )
    }

//    single {
//        LexicalSearchPipeline(
//            lexicalSearchService = get(),
//            knowledgeBaseRepository = get(),
//            logger = Logger.withTag("LexicalSearchPipeline")
//        )
//    }

//    single<SearchPipeline> {
//        HybridSearchPipeline(
//            vectorPipeline = get<VectorSearchPipeline>(),
//            lexicalPipeline = get<LexicalSearchPipeline>(),
//            embeddingService = get(),
//            embeddingModel = BuildConfig.DEFAULT_EMBEDDING_MODEL,
//            embeddingModelVersion = BuildConfig.DEFAULT_EMBEDDING_MODEL,
//            logger = Logger.withTag("HybridSearchPipeline")
//        )
//    }
}
