package io.jclaw.plugin;

import io.jclaw.core.agent.AgentHookDispatcher;
import io.jclaw.core.hook.HookName;

/**
 * Adapts the existing {@link HookRunner} to the {@link AgentHookDispatcher} SPI,
 * allowing the agent runtime to fire hooks without a direct dependency on jclaw-plugin-sdk.
 */
public class HookRunnerAdapter implements AgentHookDispatcher {

    private final HookRunner hookRunner;

    public HookRunnerAdapter(HookRunner hookRunner) {
        this.hookRunner = hookRunner;
    }

    @Override
    public <E, C> void fireVoid(HookName hookName, E event, C context) {
        hookRunner.fireVoid(hookName, event, context);
    }

    @Override
    public <E, C> E fireModifying(HookName hookName, E event, C context) {
        return hookRunner.fireModifying(hookName, event, context);
    }

    @Override
    public boolean hasHandlers(HookName hookName) {
        return hookRunner.hasHandlers(hookName);
    }
}
