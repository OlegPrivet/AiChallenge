package org.oleg.ai.challenge.domain.command

/**
 * Sealed interface representing chat commands.
 * Commands are user-triggered actions that transform input text before sending to AI.
 *
 * Example: "/help https://github.com/user/repo" → "Provide a general overview of..."
 */
sealed interface Command {
    /**
     * The raw input text from the user (including command prefix)
     */
    val rawInput: String

    /**
     * Help command: /help <github_url>
     * Requests AI to provide a general overview of a GitHub repository
     *
     * @property githubUrl The GitHub repository URL to analyze
     */
    data class Help(
        override val rawInput: String,
        val githubUrl: String
    ) : Command

    /**
     * Unknown command - represents a command that is not recognized
     * Used for graceful error handling
     *
     * @property commandName The name of the unrecognized command
     */
    data class Unknown(
        override val rawInput: String,
        val commandName: String
    ) : Command
}
