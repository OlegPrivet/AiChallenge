package org.oleg.ai.challenge.data.rag.agent

import co.touchlab.kermit.Logger
import org.oleg.ai.challenge.data.network.service.McpClientService
import org.oleg.ai.challenge.domain.rag.agent.ExternalToolGateway

/**
 * External tool gateway that uses MCP web search server to fetch external context.
 *
 * This implementation:
 * 1. Connects to an MCP web search server
 * 2. Invokes the search tool with the query
 * 3. Parses and returns search result snippets
 */
class WebSearchExternalToolGateway(
    private val mcpClientService: McpClientService,
    private val searchToolName: String = "search",
    private val maxResults: Int = 5,
    private val logger: Logger = Logger.withTag("WebSearchExternalToolGateway")
) : ExternalToolGateway {

    override suspend fun fetch(query: String): List<String> {
        return try {
            logger.d { "Fetching external context via MCP web search for query: $query" }

            // Check if we have available tools
            val availableTools = mcpClientService.availableToolsAsList
            if (availableTools == null || availableTools.isEmpty()) {
                logger.w { "No MCP tools available for web search" }
                return emptyList()
            }

            // Find the search tool
            val searchTool = availableTools.find { tool ->
                tool.name.contains("search", ignoreCase = true) ||
                tool.name == searchToolName
            }

            if (searchTool == null) {
                logger.w { "Web search tool not found in available MCP tools: ${availableTools.map { it.name }}" }
                return emptyList()
            }

            logger.d { "Using MCP tool: ${searchTool.name}" }

            // Call the MCP search tool
            val result = mcpClientService.callTool(
                name = searchTool.name,
                arguments = mapOf(
                    "query" to query,
                    "num_results" to maxResults
                )
            )

            // Parse the result
            when {
                result.isSuccess -> {
                    val resultText = result.getOrNull() ?: ""
                    parseSearchResults(resultText)
                }
                else -> {
                    val error = result.exceptionOrNull()
                    logger.e(error) { "Failed to call MCP search tool" }
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Exception during web search via MCP" }
            emptyList()
        }
    }

    /**
     * Parses search results from MCP tool response.
     *
     * The MCP tool response format may vary, so we try multiple parsing strategies:
     * 1. JSON array/object with title/snippet fields
     * 2. Plain text with line breaks
     * 3. Markdown-formatted results
     */
    private fun parseSearchResults(resultText: String): List<String> {
        return try {
            // Strategy 1: Split by common delimiters and extract meaningful snippets
            val snippets = resultText.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { line ->
                    // Filter out metadata lines
                    !line.startsWith("[") &&
                    !line.startsWith("Source:") &&
                    !line.startsWith("URL:") &&
                    line.length > 20 // Minimum snippet length
                }
                .take(maxResults)

            if (snippets.isEmpty()) {
                // Strategy 2: Use the entire result as a single snippet
                listOf(resultText.take(500))
            } else {
                logger.d { "Parsed ${snippets.size} search result snippets" }
                snippets
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to parse search results, returning raw text" }
            listOf(resultText.take(500))
        }
    }
}
