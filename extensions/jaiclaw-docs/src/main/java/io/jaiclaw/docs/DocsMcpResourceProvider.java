package io.jaiclaw.docs;

import io.jaiclaw.core.mcp.McpResourceContent;
import io.jaiclaw.core.mcp.McpResourceDefinition;
import io.jaiclaw.core.mcp.McpResourceProvider;
import io.jaiclaw.core.tenant.TenantContext;

import java.util.List;
import java.util.Optional;

/**
 * MCP resource provider that exposes JaiClaw documentation as browsable resources.
 */
public class DocsMcpResourceProvider implements McpResourceProvider {

    private static final String SERVER_NAME = "docs";

    private final DocsRepository repository;

    public DocsMcpResourceProvider(DocsRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getServerName() {
        return SERVER_NAME;
    }

    @Override
    public String getServerDescription() {
        return "JaiClaw documentation — architecture, operations, features, developer guide";
    }

    @Override
    public List<McpResourceDefinition> getResources() {
        return repository.findAll().stream()
                .map(entry -> new McpResourceDefinition(
                        entry.uri(),
                        entry.name(),
                        entry.mimeType(),
                        entry.content().substring(0, Math.min(200, entry.content().length())) + "..."
                ))
                .toList();
    }

    @Override
    public Optional<McpResourceContent> read(String uri, TenantContext tenant) {
        return repository.findByUri(uri)
                .map(entry -> new McpResourceContent(entry.uri(), entry.mimeType(), entry.content()));
    }
}
