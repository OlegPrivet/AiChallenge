package org.oleg.ai.challenge.data.network.service

/**
 * iOS implementation of MCP process spawning.
 *
 * **iOS does NOT support arbitrary process spawning** due to platform security restrictions:
 * - iOS apps run in a sandboxed environment
 * - Apps cannot spawn external processes or execute arbitrary binaries
 * - This is a fundamental security restriction of the iOS platform
 *
 * **Alternatives for iOS**:
 * 1. Use SSE transport to connect to remote MCP servers
 * 2. Use SSE transport to connect to localhost MCP servers (if running via debugging/development)
 * 3. Implement MCP server functionality directly within the iOS app (in-process)
 *
 * This implementation throws an UnsupportedOperationException with helpful guidance.
 */
actual fun spawnMcpProcess(
    command: String,
    args: List<String>,
    env: Map<String, String>,
): McpProcess {
    throw UnsupportedOperationException(
        """
        StdIO transport is not supported on iOS.

        iOS security restrictions prevent apps from spawning external processes.
        This is a fundamental platform limitation.

        Alternatives:
        1. Use SSE transport to connect to remote MCP servers
        2. Use SSE transport to connect to localhost servers (development only)
        3. Implement MCP functionality in-process within the iOS app

        Attempted command: $command ${args.joinToString(" ")}
        """.trimIndent()
    )
}
