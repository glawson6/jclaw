package io.jaiclaw.core.mcp;

/**
 * Describes a single MCP resource that a server can expose.
 *
 * @param uri         stable URI for the resource (e.g. {@code docs://architecture})
 * @param name        human-readable name
 * @param mimeType    MIME type (e.g. {@code text/markdown})
 * @param description optional description of the resource content
 */
public record McpResourceDefinition(
        String uri,
        String name,
        String mimeType,
        String description
) {}
