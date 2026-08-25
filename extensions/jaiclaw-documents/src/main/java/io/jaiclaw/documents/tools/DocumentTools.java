package io.jaiclaw.documents.tools;

import io.jaiclaw.core.tool.ToolCallback;
import io.jaiclaw.tools.ToolRegistry;

import java.util.List;

/**
 * Static factory for document-related tools (PDF form reading, filling).
 */
public final class DocumentTools {

    private DocumentTools() {}

    /**
     * @return the built-in document tools with workspace-boundary enforcement enabled.
     */
    public static List<ToolCallback> all() {
        return all(true);
    }

    /**
     * @param enforceWorkspaceBoundary when true, PDF tools reject paths that escape
     *                                 {@link io.jaiclaw.core.tool.ToolContext#workspaceDir()}.
     */
    public static List<ToolCallback> all(boolean enforceWorkspaceBoundary) {
        return List.of(
                new PdfReadFieldsTool(enforceWorkspaceBoundary),
                new PdfFillFormTool(enforceWorkspaceBoundary)
        );
    }

    public static void registerAll(ToolRegistry registry) {
        registerAll(registry, true);
    }

    public static void registerAll(ToolRegistry registry, boolean enforceWorkspaceBoundary) {
        registry.registerAll(all(enforceWorkspaceBoundary));
    }
}
