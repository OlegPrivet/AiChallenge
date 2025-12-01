package org.oleg.ai.challenge.domain.command

/**
 * Result of parsing user input for commands
 */
sealed interface ParseResult {
    /**
     * Successfully parsed a command
     */
    data class Success(val command: Command) : ParseResult

    /**
     * Input is a command but has an error (e.g., missing arguments)
     */
    data class Error(val message: String) : ParseResult

    /**
     * Input is not a command - should be processed as a regular message
     */
    data object NotACommand : ParseResult
}

/**
 * Parser for chat commands.
 * Responsible only for syntax parsing - does not perform validation.
 */
interface CommandParser {
    /**
     * Parses user input to detect and extract commands
     *
     * @param input The raw user input
     * @return ParseResult indicating whether input is a command, and if so, which type
     */
    fun parse(input: String): ParseResult
}

/**
 * Default implementation of CommandParser
 * Handles commands with slash prefix (e.g., /help, /search)
 */
class DefaultCommandParser : CommandParser {
    companion object {
        private const val COMMAND_PREFIX = "/"
        private const val HELP_COMMAND = "help"
    }

    override fun parse(input: String): ParseResult {
        val trimmed = input.trim()

        // Not a command if doesn't start with /
        if (!trimmed.startsWith(COMMAND_PREFIX)) {
            return ParseResult.NotACommand
        }

        // Extract command name and arguments
        val parts = trimmed.substring(1).split(Regex("\\s+"), limit = 2)
        if (parts.isEmpty()) {
            return ParseResult.Error("Empty command")
        }

        val commandName = parts[0].lowercase()
        val args = parts.getOrNull(1)?.trim() ?: ""

        return when (commandName) {
            HELP_COMMAND -> parseHelpCommand(trimmed, args)
            else -> ParseResult.Success(
                Command.Unknown(
                    rawInput = trimmed,
                    commandName = commandName
                )
            )
        }
    }

    private fun parseHelpCommand(rawInput: String, args: String): ParseResult {
        return if (args.isBlank()) {
            ParseResult.Error("Usage: /help <github_url>")
        } else {
            ParseResult.Success(
                Command.Help(
                    rawInput = rawInput,
                    githubUrl = args
                )
            )
        }
    }
}
