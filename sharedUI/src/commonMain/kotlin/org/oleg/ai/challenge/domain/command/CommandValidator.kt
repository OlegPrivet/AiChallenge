package org.oleg.ai.challenge.domain.command

/**
 * Result of command validation
 */
sealed interface ValidationResult {
    /**
     * Command is valid and can be executed
     */
    data class Valid(val command: Command) : ValidationResult

    /**
     * Command is invalid
     * @property command The invalid command
     * @property reason Human-readable error message
     */
    data class Invalid(val command: Command, val reason: String) : ValidationResult
}

/**
 * Validator for parsed commands.
 * Performs business rule validation (e.g., URL format, argument constraints)
 */
interface CommandValidator {
    /**
     * Validates a parsed command
     *
     * @param command The command to validate
     * @return ValidationResult indicating whether the command is valid
     */
    fun validate(command: Command): ValidationResult
}

/**
 * Default implementation of CommandValidator
 */
class DefaultCommandValidator(
    private val githubUrlValidator: GitHubUrlValidator
) : CommandValidator {

    override fun validate(command: Command): ValidationResult {
        return when (command) {
            is Command.Help -> validateHelpCommand(command)
            is Command.Unknown -> ValidationResult.Invalid(
                command = command,
                reason = "Unknown command: /${command.commandName}. Available commands: /help"
            )
        }
    }

    private fun validateHelpCommand(command: Command.Help): ValidationResult {
        val urlValidation = githubUrlValidator.validate(command.githubUrl)
        return when (urlValidation) {
            is GitHubUrlValidation.Valid -> ValidationResult.Valid(command)
            is GitHubUrlValidation.Invalid -> ValidationResult.Invalid(
                command = command,
                reason = urlValidation.reason
            )
        }
    }
}
