package org.oleg.ai.challenge.data.model

/**
 * UI state for MCP (Model Context Protocol) operations in the chat.
 *
 * This state tracks the progress of MCP tool invocations and provides
 * information to display loading indicators and status messages.
 */
data class McpUiState(
    /**
     * Whether an MCP tool is currently being invoked.
     */
    val isMcpRunning: Boolean = false,

    /**
     * The name of the currently executing tool, if any.
     */
    val currentToolName: String? = null,

    /**
     * The current phase of MCP processing.
     */
    val processingPhase: McpProcessingPhase = McpProcessingPhase.Idle,

    /**
     * Validation error message, if any.
     */
    val validationError: String? = null,

    /**
     * Number of validation retry attempts made.
     */
    val retryCount: Int = 0
) {
    companion object {
        /**
         * Maximum number of validation retries before giving up.
         */
        const val MAX_RETRIES = 2
    }
}

/**
 * Phases of MCP processing pipeline.
 */
enum class McpProcessingPhase {
    /**
     * No MCP operation in progress.
     */
    Idle,

    /**
     * Validating AI response against tool schema.
     */
    Validating,

    /**
     * Invoking the MCP tool on the server.
     */
    InvokingTool,

    /**
     * Sending MCP result back to AI for final response.
     */
    GeneratingFinalResponse,

    /**
     * Retrying after validation failure.
     */
    Retrying
}
