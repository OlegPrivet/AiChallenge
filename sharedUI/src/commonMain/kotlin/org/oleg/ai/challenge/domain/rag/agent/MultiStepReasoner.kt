package org.oleg.ai.challenge.domain.rag.agent

import org.oleg.ai.challenge.domain.rag.model.RetrievalResult

data class MultiStepResult(
    val steps: List<StepResult>,
    val mergedResults: List<RetrievalResult>
)

data class StepResult(
    val query: String,
    val results: List<RetrievalResult>
)

interface MultiStepReasoner {
    suspend fun run(query: String): MultiStepResult
}
