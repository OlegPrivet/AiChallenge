package org.oleg.ai.challenge.data.rag.di

import org.koin.core.module.Module

/**
 * Platform-specific RAG modules (e.g., Lucene BM25 on JVM).
 */
expect fun ragPlatformModules(): List<Module>
