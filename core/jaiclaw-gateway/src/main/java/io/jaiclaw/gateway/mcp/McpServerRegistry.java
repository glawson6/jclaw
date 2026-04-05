package io.jaiclaw.gateway.mcp;

import io.jaiclaw.core.mcp.McpResourceProvider;
import io.jaiclaw.core.mcp.McpToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of MCP tool and resource providers keyed by server name.
 * The gateway uses this to route incoming MCP requests to the correct provider.
 */
public class McpServerRegistry {

    private static final Logger log = LoggerFactory.getLogger(McpServerRegistry.class);

    private final Map<String, McpToolProvider> toolProviders = new ConcurrentHashMap<>();
    private final Map<String, McpResourceProvider> resourceProviders = new ConcurrentHashMap<>();

    public McpServerRegistry() {}

    /** Backward-compatible constructor — tools only. */
    public McpServerRegistry(List<McpToolProvider> toolProviders) {
        toolProviders.forEach(this::register);
    }

    /** Full constructor — tools and resources. */
    public McpServerRegistry(List<McpToolProvider> toolProviders, List<McpResourceProvider> resourceProviders) {
        toolProviders.forEach(this::register);
        resourceProviders.forEach(this::registerResource);
    }

    public void register(McpToolProvider provider) {
        toolProviders.put(provider.getServerName(), provider);
        log.info("Registered MCP server: {} ({} tools)", provider.getServerName(), provider.getTools().size());
    }

    public void registerResource(McpResourceProvider provider) {
        resourceProviders.put(provider.getServerName(), provider);
        log.info("Registered MCP resource server: {} ({} resources)", provider.getServerName(), provider.getResources().size());
    }

    public Optional<McpToolProvider> get(String serverName) {
        return Optional.ofNullable(toolProviders.get(serverName));
    }

    public Optional<McpResourceProvider> getResourceProvider(String serverName) {
        return Optional.ofNullable(resourceProviders.get(serverName));
    }

    public boolean hasToolProvider(String serverName) {
        return toolProviders.containsKey(serverName);
    }

    public boolean hasResourceProvider(String serverName) {
        return resourceProviders.containsKey(serverName);
    }

    /** Returns the union of all server names (tool + resource providers). */
    public List<String> serverNames() {
        Set<String> names = new LinkedHashSet<>(toolProviders.keySet());
        names.addAll(resourceProviders.keySet());
        return List.copyOf(names);
    }

    public int size() {
        return serverNames().size();
    }
}
