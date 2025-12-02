package org.oleg.ai.challenge.data.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

@Serializable
data class ResponseContent(
    val message: String,
    val instructions: List<Instructions>? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("Instructions")
@JsonClassDiscriminator("type")
sealed class Instructions {
    abstract val isCompleted: Boolean

    @Serializable
    @SerialName("CallMCPTool")
    data class CallMCPTool(
        val name: String,
        val arguments: JsonObject,
        override val isCompleted: Boolean,
    ) : Instructions()

    @Serializable
    @SerialName("CallAi")
    data class CallAi(
        val expectedResultOfInstruction: String,
        val actualResultOfInstruction: String = "",
        override val isCompleted: Boolean,
    ) : Instructions()

    @Serializable
    @SerialName("RetrieveFromKnowledge")
    data class RetrieveFromKnowledge(
        val query: String,
        val topK: Int = 6,
        val filters: Map<String, String> = emptyMap(),
        val similarityThreshold: Double = 0.5,
        val hybridSearchEnabled: Boolean = false,
        val hybridSearchWeight: Double? = null,
        val retrievedContext: String = "",
        val citationCount: Int = 0,
        override val isCompleted: Boolean,
    ) : Instructions()

    @Serializable
    @SerialName("AddToKnowledge")
    data class AddToKnowledge(
        val title: String,
        val content: String,
        val description: String? = null,
        val sourceType: String = "USER",
        val uri: String? = null,
        val metadata: Map<String, String> = emptyMap(),
        val chunkingStrategy: String = "recursive",
        val chunkingStrategyParams: Map<String, String> = emptyMap(),
        val documentId: String = "",
        val chunksCreated: Int = 0,
        override val isCompleted: Boolean,
    ) : Instructions()
}
