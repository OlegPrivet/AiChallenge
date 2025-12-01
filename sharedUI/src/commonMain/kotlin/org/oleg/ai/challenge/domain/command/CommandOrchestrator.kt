package org.oleg.ai.challenge.domain.command

/**
 * Orchestrates command processing pipeline: parse → validate → execute
 * Single entry point for component layer to process user input
 */
class CommandOrchestrator(
    private val parser: CommandParser,
    private val validator: CommandValidator,
    private val registry: CommandRegistry
) {

    /**
     * Processes user input through the complete pipeline
     *
     * @param input The raw user input from chat
     * @return ProcessingResult indicating how to handle the input
     */
    suspend fun process(input: String): ProcessingResult {
        // Step 1: Parse
        when (val parseResult = parser.parse(input)) {
            is ParseResult.NotACommand -> return ProcessingResult.NotACommand
            is ParseResult.Error -> return ProcessingResult.Error(parseResult.message)
            is ParseResult.Success -> {
                val command = parseResult.command

                // Step 2: Validate
                when (val validationResult = validator.validate(command)) {
                    is ValidationResult.Invalid -> {
                        return ProcessingResult.Error(validationResult.reason)
                    }
                    is ValidationResult.Valid -> {
                        // Step 3: Execute
                        val handler = registry.getHandler(command) ?: return ProcessingResult.Error(
                            "No handler registered for command: ${command::class.simpleName}"
                        )

                        return when (val handlerResult = handler.execute(command)) {
                            is HandlerResult.Success -> ProcessingResult.Success(
                                transformedMessage = handlerResult.message,
                                originalCommand = command
                            )
                            is HandlerResult.Error -> ProcessingResult.Error(handlerResult.reason)
                        }
                    }
                }
            }
        }
    }

    /**
     * Result of processing user input
     */
    sealed interface ProcessingResult {
        /**
         * Input is not a command - process as normal message
         */
        data object NotACommand : ProcessingResult

        /**
         * Command was successfully processed
         * @property transformedMessage The message to send to AI
         * @property originalCommand The original command for reference
         */
        data class Success(
            val transformedMessage: String,
            val originalCommand: Command
        ) : ProcessingResult

        /**
         * Command processing failed
         * @property message Error message to display to user
         */
        data class Error(val message: String) : ProcessingResult
    }
}
