package io.jaiclaw.documents.tools

import io.jaiclaw.core.tool.ToolContext
import io.jaiclaw.core.tool.ToolResult
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

/**
 * SEV-004 regression guard — {@link PdfReadFieldsTool} and
 * {@link PdfFillFormTool} must reject any path outside
 * {@link ToolContext#workspaceDir()} when
 * {@code jaiclaw.tools.documents.workspace-boundary=true}.
 */
class PdfToolsWorkspaceBoundarySpec extends Specification {

    @TempDir
    Path workspace

    def "pdf_read_fields rejects an absolute path that escapes the workspace"() {
        given:
        def tool = new PdfReadFieldsTool()
        def context = new ToolContext("agent", "session", "sid", workspace.toString())

        when:
        def result = tool.execute([path: "/etc/passwd"], context)

        then:
        result instanceof ToolResult.Error
        (result as ToolResult.Error).message().toLowerCase().contains("path traversal")
    }

    def "pdf_fill_form rejects an absolute template path that escapes the workspace"() {
        given:
        def tool = new PdfFillFormTool()
        def context = new ToolContext("agent", "session", "sid", workspace.toString())

        when:
        def result = tool.execute([
                templatePath: "/etc/passwd",
                outputPath: "/tmp/output.pdf",
                fields: [:]
        ], context)

        then:
        result instanceof ToolResult.Error
        (result as ToolResult.Error).message().toLowerCase().contains("path traversal")
    }

    def "pdf_read_fields accepts a workspace-relative path (file not found is a functional failure, not a security block)"() {
        given:
        def tool = new PdfReadFieldsTool()
        def context = new ToolContext("agent", "session", "sid", workspace.toString())

        when:
        def result = tool.execute([path: "does-not-exist.pdf"], context)

        then:
        result instanceof ToolResult.Error
        (result as ToolResult.Error).message().toLowerCase().contains("not found")
        !(result as ToolResult.Error).message().toLowerCase().contains("path traversal")
    }

    def "pdf_read_fields with boundary disabled accepts absolute paths"() {
        given:
        def tool = new PdfReadFieldsTool(false)
        def context = new ToolContext("agent", "session", "sid", workspace.toString())

        when:
        def result = tool.execute([path: "/etc/does-not-exist-anywhere"], context)

        then:
        // With boundary disabled the tool proceeds and reports "file not found"
        // instead of a security block. This proves the flag is honored.
        result instanceof ToolResult.Error
        !(result as ToolResult.Error).message().toLowerCase().contains("path traversal")
    }
}
