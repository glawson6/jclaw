package io.jaiclaw.documents.tools;

import io.jaiclaw.core.tool.ToolCallback;
import io.jaiclaw.tools.ToolRegistry;

import java.util.List;

/**
 * Static factory for document-related tools (PDF form reading, filling).
 */
public final class DocumentTools {

    private DocumentTools() {}

    public static List<ToolCallback> all() {
        return List.of(
                new PdfReadFieldsTool(),
                new PdfFillFormTool()
        );
    }

    public static void registerAll(ToolRegistry registry) {
        registry.registerAll(all());
    }
}
