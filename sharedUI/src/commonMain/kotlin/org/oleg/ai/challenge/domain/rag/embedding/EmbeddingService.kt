package org.oleg.ai.challenge.domain.rag.embedding

import org.oleg.ai.challenge.domain.rag.model.Embedding

interface EmbeddingService {
    suspend fun embed(request: EmbeddingRequest): List<Embedding>
}

interface EmbeddingCache {
    suspend fun get(text: String, modelVersion: String): Embedding?
    suspend fun put(text: String, modelVersion: String, embedding: Embedding)
}

class InMemoryEmbeddingCache : EmbeddingCache {
    private val storage = mutableMapOf<String, Embedding>()

    override suspend fun get(text: String, modelVersion: String): Embedding? {
        return storage[key(text, modelVersion)]
    }

    override suspend fun put(text: String, modelVersion: String, embedding: Embedding) {
        storage[key(text, modelVersion)] = embedding
    }

    private fun key(text: String, modelVersion: String): String = "${modelVersion}::${text.hashCode()}"
}
