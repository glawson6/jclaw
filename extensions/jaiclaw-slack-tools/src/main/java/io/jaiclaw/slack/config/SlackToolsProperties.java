package io.jaiclaw.slack.config;

import java.util.List;

/**
 * Configuration properties for the Slack tools MCP server.
 *
 * @param enabled        whether the Slack tools MCP server is active
 * @param allowedActions action whitelist — empty means all actions allowed
 */
public record SlackToolsProperties(
        boolean enabled,
        List<String> allowedActions
) {
    public SlackToolsProperties {
        if (allowedActions == null) allowedActions = List.of();
    }

    public SlackToolsProperties() {
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
