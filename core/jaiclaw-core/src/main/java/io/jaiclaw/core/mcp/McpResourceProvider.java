package io.jaiclaw.core.mcp;

import io.jaiclaw.core.tenant.TenantContext;

import java.util.List;
import java.util.Optional;

/**
 * SPI for contributing resources to a hosted MCP server.
 * Modules implement this interface to expose read-only resources
 * (documentation, configuration, data) that external MCP clients can browse and read.
 *
 * <p>Each provider is associated with a named MCP server (e.g. "docs").
 * The gateway hosts each server at {@code /mcp/{serverName}}.
 */
public interface McpResourceProvider {

    /** Logical server name — must match any co-located {@link McpToolProvider} for the same server. */
    String getServerName();

    /** Human-readable description of this resource server. */
    String getServerDescription();

    /** List all available resources. */
    List<McpResourceDefinition> getResources();

    /** Read a single resource by URI. Returns empty if the URI is unknown. */
    Optional<McpResourceContent> read(String uri, TenantContext tenant);
}
