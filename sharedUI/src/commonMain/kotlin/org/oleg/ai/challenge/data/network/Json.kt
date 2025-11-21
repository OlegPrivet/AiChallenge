package org.oleg.ai.challenge.data.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.oleg.ai.challenge.data.network.model.Instructions

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    classDiscriminator = "type" // ← ОБЯЗАТЕЛЬНО
    serializersModule = SerializersModule {
        polymorphic(Instructions::class) {
            subclass(Instructions.CallMCPTool::class)
            subclass(Instructions.CallAi::class)
        }
    }
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    prettyPrintIndent = "  " // 2 spaces for indentation
    encodeDefaults = true
}
