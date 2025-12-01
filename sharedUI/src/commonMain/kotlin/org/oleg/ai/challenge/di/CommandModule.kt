package org.oleg.ai.challenge.di

import org.koin.dsl.module
import org.oleg.ai.challenge.domain.command.*

/**
 * Koin module for command processing system
 * Provides all dependencies for the command processing pipeline
 */
val commandModule = module {
    // Validators
    single { GitHubUrlValidator() }

    // Core components
    single<CommandParser> { DefaultCommandParser() }
    single<CommandValidator> { DefaultCommandValidator(get()) }

    // Command handlers
    single { HelpCommandHandler() }

    // Registry with pre-registered handlers
    single {
        CommandRegistry().apply {
            // Register Help command handler
            register(Command.Help::class, get<HelpCommandHandler>())

            // Future commands can be registered here:
            // register(Command.Search::class, get<SearchCommandHandler>())
        }
    }

    // Orchestrator (main entry point for component layer)
    single {
        CommandOrchestrator(
            parser = get(),
            validator = get(),
            registry = get()
        )
    }
}
