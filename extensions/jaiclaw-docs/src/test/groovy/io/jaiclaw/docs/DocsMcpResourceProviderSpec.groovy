package io.jaiclaw.docs

import io.jaiclaw.core.mcp.McpResourceContent
import io.jaiclaw.core.mcp.McpResourceDefinition
import spock.lang.Specification

class DocsMcpResourceProviderSpec extends Specification {

    DocsRepository repository
    DocsMcpResourceProvider provider

    def setup() {
        repository = new DocsRepository([
            new DocsEntry("docs://arch", "architecture", "text/markdown",
                "# Architecture\nJaiClaw architecture overview", ["architecture"]),
            new DocsEntry("docs://ops", "operations", "text/markdown",
                "# Operations\nDeployment guide", ["operations"])
        ])
        provider = new DocsMcpResourceProvider(repository)
    }

    def "server name is docs"() {
        expect:
        provider.getServerName() == "docs"
    }

    def "getResources returns all docs as resource definitions"() {
        when:
        def resources = provider.getResources()

        then:
        resources.size() == 2
        resources.every { it instanceof McpResourceDefinition }
        resources.any { it.uri() == "docs://arch" && it.name() == "architecture" }
        resources.any { it.uri() == "docs://ops" && it.name() == "operations" }
        resources.every { it.mimeType() == "text/markdown" }
    }

    def "read returns content for known URI"() {
        when:
        def result = provider.read("docs://arch", null)

        then:
        result.isPresent()
        result.get().uri() == "docs://arch"
        result.get().mimeType() == "text/markdown"
        result.get().text().contains("Architecture")
    }

    def "read returns empty for unknown URI"() {
        when:
        def result = provider.read("docs://nonexistent", null)

        then:
        result.isEmpty()
    }
}
