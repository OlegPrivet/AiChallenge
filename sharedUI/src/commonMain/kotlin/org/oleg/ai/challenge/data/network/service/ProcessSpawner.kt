package org.oleg.ai.challenge.data.network.service

import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Platform-agnostic process wrapper for StdIO MCP transport.
 *
 * Implementations should provide input and output streams from the spawned process
 * that are compatible with kotlinx.io Source and Sink interfaces.
 */
interface McpProcess {
    /**
     * Input stream from the process (process stdout).
     * The MCP client reads from this stream.
     */
    val inputStream: Source

    /**
     * Output stream to the process (process stdin).
     * The MCP client writes to this stream.
     */
    val outputStream: Sink

    /**
     * Destroy/terminate the process.
     * Should be called when disconnecting to clean up resources.
     */
    fun destroy()

    /**
     * Check if the process is still running.
     */
    fun isAlive(): Boolean
}

/**
 * Spawn an MCP server process for StdIO transport.
 *
 * This is a platform-specific operation that requires different implementations:
 * - **JVM/Desktop**: Uses ProcessBuilder to spawn local processes
 * - **Android**: Uses ProcessBuilder (may have security restrictions)
 * - **iOS**: Not supported (throws UnsupportedOperationException)
 *
 * @param command Path to the executable or command to run
 * @param args Command line arguments to pass to the process
 * @param env Environment variables for the process
 * @return McpProcess wrapper providing input/output streams
 * @throws UnsupportedOperationException on platforms that don't support process spawning
 * @throws Exception if process creation fails
 */
expect fun spawnMcpProcess(
    command: String,
    args: List<String>,
    env: Map<String, String>,
): McpProcess
