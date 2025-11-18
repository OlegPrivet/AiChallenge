package org.oleg.ai.challenge.data.network.service

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered

/**
 * Android implementation of MCP process spawning using ProcessBuilder.
 *
 * **Important Security Notes**:
 * - Android's security model restricts process spawning
 * - Apps run in sandboxed environments with limited permissions
 * - Only certain system commands may be available
 * - Custom executables must be bundled with the app and extracted to app-specific directories
 * - SELinux policies may prevent execution of arbitrary binaries
 *
 * **Recommended Usage**:
 * - For local MCP servers on Android, consider using SSE transport with a localhost server
 * - Bundle MCP server binaries as assets and extract to app cache directory
 * - Ensure executables have proper permissions (chmod +x)
 *
 * This implementation uses the same ProcessBuilder approach as JVM but includes
 * Android-specific error handling and guidance.
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
        AndroidMcpProcess(process)
    } catch (e: Exception) {
        throw Exception(
            "Failed to spawn MCP process on Android: $command ${args.joinToString(" ")}\n" +
                    "Android security restrictions may prevent execution.\n" +
                    "Consider using SSE transport or bundling executables in app assets.\n" +
                    "Error: ${e.message}",
            e
        )
    }
}

/**
 * Android-specific implementation of McpProcess wrapping a Java Process.
 *
 * Identical to JVM implementation but with Android-specific context.
 */
private class AndroidMcpProcess(
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
