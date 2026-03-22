package io.jclaw.memory;

import io.jclaw.core.agent.MemoryProvider;

import java.nio.file.Path;

/**
 * Adapts the existing {@link WorkspaceMemoryManager} to the {@link MemoryProvider} SPI,
 * allowing the agent runtime to load workspace memory without a direct dependency on jclaw-memory.
 */
public class WorkspaceMemoryProvider implements MemoryProvider {

    @Override
    public String loadMemory(String workspaceDir) {
        if (workspaceDir == null || workspaceDir.isBlank()) {
            return "";
        }
        var manager = new WorkspaceMemoryManager(Path.of(workspaceDir));
        return manager.readMemory();
    }
}
