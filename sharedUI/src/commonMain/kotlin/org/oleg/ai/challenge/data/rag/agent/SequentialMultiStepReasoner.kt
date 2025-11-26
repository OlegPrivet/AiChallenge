package org.oleg.ai.challenge.data.rag.agent

import org.oleg.ai.challenge.domain.rag.SearchPipeline
import org.oleg.ai.challenge.domain.rag.agent.MultiStepReasoner
import org.oleg.ai.challenge.domain.rag.agent.MultiStepResult
import org.oleg.ai.challenge.domain.rag.agent.QueryDecomposer
import org.oleg.ai.challenge.domain.rag.agent.StepResult
import org.oleg.ai.challenge.domain.rag.model.RetrievalResult

/**
 * Executes sequential retrieval for decomposed sub-queries with semantic synthesis.
 *
 * This reasoner:
 * 1. Decomposes queries into sub-queries
 * 2. Retrieves results for each sub-query
 * 3. Performs intelligent deduplication and synthesis:
 *    - Groups semantically similar chunks
 *    - Prioritizes diverse information sources
 *    - Boosts scores for chunks matching multiple sub-queries
 */
class SequentialMultiStepReasoner(
    private val queryDecomposer: QueryDecomposer,
    private val searchPipeline: SearchPipeline,
    private val topK: Int = 4
) : MultiStepReasoner {

    override suspend fun run(query: String): MultiStepResult {
        val subQueries = queryDecomposer.decompose(query)
        val steps = mutableListOf<StepResult>()
        val merged = mutableListOf<RetrievalResult>()

        for (subQuery in subQueries) {
            val results = searchPipeline.search(subQuery, topK = topK)
            steps.add(
                StepResult(
                    query = subQuery,
                    results = results
                )
            )
            merged.addAll(results)
        }

        // Intelligent semantic synthesis
        val synthesized = synthesizeResults(merged, steps)

        return MultiStepResult(
            steps = steps,
            mergedResults = synthesized
        )
    }

    /**
     * Synthesizes results from multiple retrieval steps using:
     * 1. Deduplication by chunk ID (keep highest score)
     * 2. Frequency boosting (chunks appearing in multiple steps get higher scores)
     * 3. Diversity promotion (prefer chunks from different documents)
     * 4. Content similarity detection (remove near-duplicate content)
     */
    private fun synthesizeResults(
        results: List<RetrievalResult>,
        steps: List<StepResult>
    ): List<RetrievalResult> {
        if (results.isEmpty()) return emptyList()

        // Step 1: Count how many sub-queries each chunk matched
        val chunkFrequency = mutableMapOf<String, Int>()
        steps.forEach { step ->
            step.results.forEach { result ->
                chunkFrequency[result.chunk.id] = (chunkFrequency[result.chunk.id] ?: 0) + 1
            }
        }

        // Step 2: Deduplicate by chunk ID and boost score by frequency
        val dedupedWithBoost = results.groupBy { it.chunk.id }
            .map { (chunkId, values) ->
                val best = values.maxBy { it.score }
                val frequency = chunkFrequency[chunkId] ?: 1
                val frequencyBoost = 1.0 + (frequency - 1) * 0.2 // +20% per additional match

                best.copy(score = best.score * frequencyBoost)
            }

        // Step 3: Remove near-duplicate content (similar text from different chunks)
        val withoutDuplicates = removeSimilarContent(dedupedWithBoost)

        // Step 4: Promote diversity - prefer different document sources
        val diversified = promoteDiversity(withoutDuplicates)

        // Step 5: Sort by final boosted score
        return diversified.sortedByDescending { it.score }
    }

    /**
     * Removes chunks with very similar content using simple text similarity.
     */
    private fun removeSimilarContent(results: List<RetrievalResult>): List<RetrievalResult> {
        if (results.size <= 1) return results

        val filtered = mutableListOf<RetrievalResult>()
        val seen = mutableSetOf<String>()

        for (result in results.sortedByDescending { it.score }) {
            val normalizedContent = result.chunk.content
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(200) // Compare first 200 chars

            // Check if we've seen very similar content
            val isDuplicate = seen.any { seenContent ->
                calculateSimilarity(normalizedContent, seenContent) > 0.85
            }

            if (!isDuplicate) {
                filtered.add(result)
                seen.add(normalizedContent)
            }
        }

        return filtered
    }

    /**
     * Calculates simple Jaccard similarity between two strings.
     */
    private fun calculateSimilarity(text1: String, text2: String): Double {
        val words1 = text1.split(" ").toSet()
        val words2 = text2.split(" ").toSet()
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
    }

    /**
     * Promotes diversity by boosting scores of chunks from underrepresented documents.
     */
    private fun promoteDiversity(results: List<RetrievalResult>): List<RetrievalResult> {
        if (results.size <= 1) return results

        // Count chunks per document
        val docCount = results.groupingBy { it.document.id }.eachCount()
        val avgDocCount = docCount.values.average()

        return results.map { result ->
            val count = docCount[result.document.id] ?: 1
            // Boost underrepresented documents (count < average)
            val diversityBoost = if (count < avgDocCount) 1.1 else 1.0
            result.copy(score = result.score * diversityBoost)
        }
    }
}
