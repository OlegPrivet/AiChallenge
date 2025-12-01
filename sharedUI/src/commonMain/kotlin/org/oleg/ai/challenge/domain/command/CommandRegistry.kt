package org.oleg.ai.challenge.domain.command

import kotlin.reflect.KClass

/**
 * Registry for command handlers
 * Enables extensibility by mapping command types to their handlers
 *
 * To add a new command:
 * 1. Create command data class in Command.kt
 * 2. Create handler implementing CommandHandler<YourCommand>
 * 3. Register in Koin module: registry.register(Command.YourCommand::class, handler)
 */
class CommandRegistry {
    private val handlers = mutableMapOf<KClass<out Command>, CommandHandler<*>>()

    /**
     * Registers a handler for a specific command type
     *
     * @param commandClass The command class (e.g., Command.Help::class)
     * @param handler The handler instance
     */
    fun <T : Command> register(commandClass: KClass<T>, handler: CommandHandler<T>) {
        handlers[commandClass] = handler
    }

    /**
     * Retrieves the handler for a given command
     *
     * @param command The command instance
     * @return The registered handler, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Command> getHandler(command: T): CommandHandler<T>? {
        return handlers[command::class] as? CommandHandler<T>
    }

    /**
     * Checks if a handler is registered for a command type
     *
     * @param commandClass The command class to check
     * @return true if a handler is registered
     */
    fun isRegistered(commandClass: KClass<out Command>): Boolean {
        return handlers.containsKey(commandClass)
    }
}
