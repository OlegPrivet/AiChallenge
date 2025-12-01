package org.oleg.ai.challenge.domain.command

/**
 * Result of command handler execution
 */
sealed interface HandlerResult {
    /**
     * Handler successfully transformed the command
     * @property message The transformed message to send to AI
     */
    data class Success(val message: String) : HandlerResult

    /**
     * Handler encountered an error
     * @property reason Human-readable error message
     */
    data class Error(val reason: String) : HandlerResult
}

/**
 * Handler for executing a specific command type
 * Transforms commands into AI-ready prompts
 *
 * @param T The type of command this handler processes
 */
interface CommandHandler<T : Command> {
    /**
     * Executes the command and transforms it into an AI prompt
     *
     * @param command The command to execute
     * @return HandlerResult with the transformed message or error
     */
    suspend fun execute(command: T): HandlerResult
}
