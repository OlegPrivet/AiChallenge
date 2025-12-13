package org.oleg.ai.challenge.data.network

import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    prettyPrintIndent = "  " // 2 spaces for indentation
    encodeDefaults = true
}
