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
}
