package org.oleg.ai.challenge.data.network.service

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered

/**
 * JVM implementation of MCP process spawning using ProcessBuilder.
 *
 * This implementation:
 * - Uses Java's ProcessBuilder to spawn local processes
 * - Converts Java InputStream/OutputStream to kotlinx.io Source/Sink
 * - Provides proper process lifecycle management
 * - Supports environment variables and command arguments
 */
actual fun spawnMcpProcess(
    command: String,
    args: List<String>,
    env: Map<String, String>,
): McpProcess {
    return try {
        // Build the full command list
        val fullCommand = listOf(command) + args

        // Create ProcessBuilder
        val processBuilder = ProcessBuilder(fullCommand)

        // Add environment variables if provided
        if (env.isNotEmpty()) {
            val environment = processBuilder.environment()
            env.forEach { (key, value) ->
                environment[key] = value
            }
        }

        // Redirect error stream to output stream for unified logging
        processBuilder.redirectErrorStream(true)

        // Start the process
        val process = processBuilder.start()

        // Return wrapped process
        JvmMcpProcess(process)
    } catch (e: Exception) {
        throw Exception(
            "Failed to spawn MCP process: $command ${args.joinToString(" ")}\n" +
                    "Error: ${e.message}",
            e
        )
    }
}

/**
 * JVM-specific implementation of McpProcess wrapping a Java Process.
 *
 * Converts Java I/O streams to kotlinx.io for compatibility with MCP SDK.
 */
private class JvmMcpProcess(
    private val process: Process
) : McpProcess {

    /**
     * Input stream from the process (stdout).
     * Converts Java InputStream to kotlinx.io Source.
     */
    override val inputStream: Source = process.inputStream.asSource().buffered()

    /**
     * Output stream to the process (stdin).
     * Converts Java OutputStream to kotlinx.io Sink.
     */
    override val outputStream: Sink = process.outputStream.asSink().buffered()

    /**
     * Destroy the process forcibly.
     * Closes all streams and terminates the process.
     */
    override fun destroy() {
        try {
            // Close streams first
            runCatching { outputStream.close() }
            runCatching { inputStream.close() }

            // Destroy the process
            if (process.isAlive) {
                process.destroyForcibly()
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }

    /**
     * Check if the process is still running.
     */
    override fun isAlive(): Boolean = process.isAlive
}
