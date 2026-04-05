package io.jaiclaw.core.mcp;

/**
 * Content returned when reading an MCP resource.
 *
 * @param uri      the resource URI that was read
 * @param mimeType MIME type of the content
 * @param text     the resource content as text
 */
public record McpResourceContent(
        String uri,
        String mimeType,
        String text
) {}
