package io.jaiclaw.discord.config;

import java.util.List;

/**
 * Configuration properties for the Discord tools MCP server.
 *
 * @param enabled        whether the Discord tools MCP server is active
 * @param allowedActions action whitelist — empty means all actions allowed
 */
public record DiscordToolsProperties(
        boolean enabled,
        List<String> allowedActions
) {
    public DiscordToolsProperties {
        if (allowedActions == null) allowedActions = List.of();
    }

    public DiscordToolsProperties() {
        this(false, List.of());
    }

    /**
     * Returns true if the given action is permitted by the whitelist.
     * An empty whitelist means all actions are allowed.
     */
    public boolean isActionAllowed(String action) {
        return allowedActions.isEmpty() || allowedActions.contains(action);
    }
}
