package org.oleg.ai.challenge.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * Utility object for detecting and formatting JSON responses
 */
object JsonFormatter {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  " // 2 spaces for indentation
    }

    /**
     * Checks if the given text is valid JSON and formats it with proper indentation.
     * If the text is not valid JSON, returns the original text unchanged.
     *
     * @param text The text to check and potentially format
     * @return Formatted JSON if valid, otherwise the original text
     */
    fun formatIfJson(text: String): String {
        val trimmedText = text.trim()

        // Quick check: JSON objects/arrays start with { or [
        if (!trimmedText.startsWith("{") && !trimmedText.startsWith("[")) {
            return text
        }

        return try {
            // Parse to validate JSON structure
            val jsonElement = Json.parseToJsonElement(trimmedText)

            // Format with pretty print
            json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                jsonElement
            )
        } catch (e: Exception) {
            // Not valid JSON or parsing error - return original text
            text
        }
    }

    /**
     * Checks if the given text appears to be JSON
     *
     * @param text The text to check
     * @return true if the text is valid JSON, false otherwise
     */
    fun isJson(text: String): Boolean {
        val trimmedText = text.trim()

        if (!trimmedText.startsWith("{") && !trimmedText.startsWith("[")) {
            return false
        }

        return try {
            Json.parseToJsonElement(trimmedText)
            true
        } catch (e: Exception) {
            false
        }
    }
}