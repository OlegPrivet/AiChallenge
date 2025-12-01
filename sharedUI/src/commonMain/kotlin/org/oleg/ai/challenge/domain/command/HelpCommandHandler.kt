package org.oleg.ai.challenge.domain.command

/**
 * Handler for the /help command
 * Transforms GitHub repository URLs into AI prompts requesting repository overview
 */
class HelpCommandHandler : CommandHandler<Command.Help> {
    override suspend fun execute(command: Command.Help): HandlerResult {
        // Transform command into AI-friendly prompt
        val aiPrompt = "Provide a general overview of the repository at ${command.githubUrl}"
        return HandlerResult.Success(aiPrompt)
    }
}
