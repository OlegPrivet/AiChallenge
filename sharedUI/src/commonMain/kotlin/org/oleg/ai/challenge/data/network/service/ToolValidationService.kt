package org.oleg.ai.challenge.data.network.service

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Service for validating AI responses against MCP tool input schemas.
 *
 * This service intercepts AI responses and validates their JSON structure
 * against the expected tool input schema before forwarding to the MCP server.
 */
class ToolValidationService(
    private val logger: Logger = Logger.withTag("ToolValidationService")
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Result of validation operation.
     */
    sealed class ValidationResult {
        /**
         * Validation succeeded.
         * @property parsedJson The validated JSON object
         * @property toolName The name of the tool to invoke
         * @property arguments The arguments map to pass to the tool
         */
        data class Valid(
            val parsedJson: JsonObject,
            val toolName: String,
            val arguments: Map<String, Any>
        ) : ValidationResult()

        /**
         * Validation failed.
         * @property reason The reason for failure
         */
        data class Invalid(val reason: String) : ValidationResult()

        /**
         * No tool call detected in the response.
         */
        data object NoToolCall : ValidationResult()
    }

    /**
     * Validates an AI response against a tool's input schema.
     *
     * Expected AI response format:
     * ```json
     * {
     *   "tool": "tool_name",
     *   "arguments": {
     *     "param1": "value1",
     *     "param2": "value2"
     *   }
     * }
     * ```
     *
     * @param aiResponse The raw AI response text
     * @param availableTools List of available tools to validate against
     * @return ValidationResult indicating success or failure
     */
    fun validateResponse(
        aiResponse: String,
        availableTools: List<McpClientService.ToolInfo>
    ): ValidationResult {
        logger.d { "Validating AI response: ${aiResponse.take(200)}..." }

        // Try to extract JSON from the response
        val jsonString = extractJson(aiResponse)
        if (jsonString == null) {
            logger.d { "No JSON found in response, treating as regular text" }
            return ValidationResult.NoToolCall
        }

        // Parse the JSON
        val jsonObject = try {
            json.parseToJsonElement(jsonString).jsonObject
        } catch (e: Exception) {
            logger.w(e) { "Failed to parse JSON from response" }
            return ValidationResult.Invalid("Failed to parse JSON: ${e.message}")
        }

        // Check for tool call structure
        val toolName = jsonObject["tool"]?.jsonPrimitive?.content
        if (toolName == null) {
            logger.d { "No 'tool' field in JSON, treating as regular response" }
            return ValidationResult.NoToolCall
        }

        // Find the matching tool
        val tool = availableTools.find { it.name == toolName }
        if (tool == null) {
            logger.w { "Tool '$toolName' not found in available tools" }
            return ValidationResult.Invalid("Tool '$toolName' not found. Available tools: ${availableTools.map { it.name }}")
        }

        // Extract and validate arguments
        val argumentsElement = jsonObject["arguments"]
        if (argumentsElement == null) {
            logger.w { "No 'arguments' field for tool '$toolName'" }
            return ValidationResult.Invalid("Missing 'arguments' field for tool '$toolName'")
        }

        val arguments = try {
            convertJsonToMap(argumentsElement)
        } catch (e: Exception) {
            logger.w(e) { "Failed to convert arguments to map" }
            return ValidationResult.Invalid("Invalid arguments format: ${e.message}")
        }

        // Validate arguments against tool schema
        val schemaValidation = validateAgainstSchema(arguments, tool.parameters)
        if (schemaValidation != null) {
            logger.w { "Schema validation failed: $schemaValidation" }
            return ValidationResult.Invalid(schemaValidation)
        }

        logger.i { "Validation successful for tool '$toolName' with ${arguments.size} arguments" }
        return ValidationResult.Valid(
            parsedJson = jsonObject,
            toolName = toolName,
            arguments = arguments
        )
    }

    /**
     * Extracts JSON from a text that may contain markdown code blocks or plain JSON.
     */
    private fun extractJson(text: String): String? {
        val trimmed = text.trim()

        // Try to find JSON in markdown code blocks
        val codeBlockPattern = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val codeBlockMatch = codeBlockPattern.find(trimmed)
        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[1].trim()
        }

        // Try to find standalone JSON object
        val jsonObjectPattern = Regex("\\{[\\s\\S]*\\}")
        val jsonMatch = jsonObjectPattern.find(trimmed)
        if (jsonMatch != null) {
            return jsonMatch.value
        }

        return null
    }

    /**
     * Converts a JsonElement to a Map<String, Any>.
     */
    private fun convertJsonToMap(element: JsonElement): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        when (element) {
            is JsonObject -> {
                element.forEach { (key, value) ->
                    result[key] = convertJsonElementToAny(value)
                }
            }
            else -> throw IllegalArgumentException("Expected JSON object for arguments")
        }

        return result
    }

    /**
     * Converts a JsonElement to its corresponding Kotlin type.
     */
    private fun convertJsonElementToAny(element: JsonElement): Any {
        return when (element) {
            is JsonObject -> {
                val map = mutableMapOf<String, Any>()
                element.forEach { (key, value) ->
                    map[key] = convertJsonElementToAny(value)
                }
                map
            }

            is kotlinx.serialization.json.JsonArray -> {
                element.map { convertJsonElementToAny(it) }
            }

            is kotlinx.serialization.json.JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    element.content.contains('.') -> element.content.toDoubleOrNull() ?: element.content
                    else -> element.content.toLongOrNull() ?: element.content
                }
            }
        }
    }

    /**
     * Validates arguments against a tool's input schema.
     *
     * @param arguments The arguments to validate
     * @param inputSchema The schema string to validate against
     * @return Error message if validation fails, null if successful
     */
    private fun validateAgainstSchema(
        arguments: Map<String, Any>,
        inputSchema: McpClientService.Input
    ): String? {
        // Parse the schema to extract required properties
        // The inputSchema is typically a JSON string like: {property1={type=string}, property2={type=number}}

        // For now, we do basic validation - ensure all provided arguments are strings or primitives
        // Full JSON Schema validation would require additional library support

        try {
            // Basic type checking for common types
            arguments.forEach { (key, value) ->
                when (value) {
                    is String, is Number, is Boolean, is List<*>, is Map<*, *> -> {
                        // Valid types
                    }
                    else -> {
                        return "Invalid type for argument '$key': ${value::class.simpleName}"
                    }
                }
            }

            // Schema validation passed
            return null
        } catch (e: Exception) {
            return "Schema validation error: ${e.message}"
        }
    }

    /**
     * Creates a prompt to send to the AI when validation fails.
     *
     * @param validationError The validation error message
     * @param toolName The tool that was being invoked
     * @param inputSchema The expected input schema
     * @return A prompt string to send to the AI
     */
    fun createValidationErrorPrompt(
        validationError: String,
        toolName: String,
        inputSchema: McpClientService.Input?
    ): String {
        return """
            |Формат ответа не соответствует входному параметру MCP сервера.
            |
            |Ошибка: $validationError
            |
            |Для инструмента '$toolName' ожидается следующий формат: '${inputSchema?.properties} ${inputSchema?.required}'
            |Пожалуйста, исправьте формат ответа.
        """.trimMargin()
    }
}
