package org.oleg.ai.challenge.data.rag.agent

import org.oleg.ai.challenge.domain.rag.agent.Conflict
import org.oleg.ai.challenge.domain.rag.agent.ConflictResolution
import org.oleg.ai.challenge.domain.rag.agent.ConflictResolver
import org.oleg.ai.challenge.domain.rag.agent.ValidatedChunk
import org.oleg.ai.challenge.domain.rag.model.KnowledgeSourceType

/**
 * Lightweight heuristic conflict resolver using source reliability + quality scores.
 */
class HeuristicConflictResolver : ConflictResolver {

    override suspend fun resolve(results: List<ValidatedChunk>): ConflictResolution {
        if (results.size <= 1) return ConflictResolution(results, emptyList())

        // Group by document id to detect potential contradictions across docs
        val groupedByDoc = results.groupBy { it.result.document.id }
        val conflicts = mutableListOf<Conflict>()

        // Simple heuristic: if two chunks have same sourceType but diverging titles or IDs, leave as is
        // More robust detection is text-level; here we flag when two different documents share the same title
        val titleGroups = results.groupBy { it.result.document.title }
        titleGroups.forEach { (title, chunks) ->
            val uniqueDocs = chunks.map { it.result.document.id }.distinct()
            if (uniqueDocs.size > 1) {
                conflicts.add(
                    Conflict(
                        chunkIds = chunks.map { it.result.chunk.id },
                        reason = "Multiple documents share title '$title'"
                    )
                )
            }
        }

        // Rank by quality score and source reliability
        val ranked = results.sortedWith(
            compareByDescending<ValidatedChunk> { it.qualityScore }
                .thenByDescending { reliabilityWeight(it.result.document.sourceType) }
        )

        return ConflictResolution(
            resolved = ranked,
            conflicts = conflicts
        )
    }

    private fun reliabilityWeight(sourceType: KnowledgeSourceType): Double = when (sourceType) {
        KnowledgeSourceType.INTERNAL -> 1.0
        KnowledgeSourceType.USER -> 0.9
        KnowledgeSourceType.REMOTE -> 0.8
        KnowledgeSourceType.CACHED -> 0.6
    }
}
