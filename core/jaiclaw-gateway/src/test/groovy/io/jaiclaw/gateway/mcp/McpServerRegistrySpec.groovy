package io.jaiclaw.gateway.mcp

import io.jaiclaw.core.mcp.McpResourceContent
import io.jaiclaw.core.mcp.McpResourceDefinition
import io.jaiclaw.core.mcp.McpResourceProvider
import io.jaiclaw.core.mcp.McpToolDefinition
import io.jaiclaw.core.mcp.McpToolProvider
import io.jaiclaw.core.mcp.McpToolResult
import io.jaiclaw.core.tenant.TenantContext
import spock.lang.Specification

class McpServerRegistrySpec extends Specification {

    def "register and retrieve provider"() {
        given:
        def registry = new McpServerRegistry()
        def provider = createToolProvider("resources", "Resource server", 2)

        when:
        registry.register(provider)

        then:
        registry.get("resources").isPresent()
        registry.get("resources").get().getServerName() == "resources"
        registry.size() == 1
        registry.serverNames() == ["resources"]
    }

    def "get returns empty for unknown server"() {
        given:
        def registry = new McpServerRegistry()

        expect:
        registry.get("unknown").isEmpty()
    }

    def "constructor accepts list of providers"() {
        given:
        def p1 = createToolProvider("server1", "Server 1", 1)
        def p2 = createToolProvider("server2", "Server 2", 3)

        when:
        def registry = new McpServerRegistry([p1, p2])

        then:
        registry.size() == 2
        registry.serverNames().containsAll(["server1", "server2"])
    }

    def "register and retrieve resource provider"() {
        given:
        def registry = new McpServerRegistry()
        def provider = createResourceProvider("docs", "Documentation", 3)

        when:
        registry.registerResource(provider)

        then:
        registry.getResourceProvider("docs").isPresent()
        registry.getResourceProvider("docs").get().getServerName() == "docs"
        registry.hasResourceProvider("docs")
        !registry.hasToolProvider("docs")
        registry.size() == 1
        registry.serverNames() == ["docs"]
    }

    def "getResourceProvider returns empty for unknown server"() {
        given:
        def registry = new McpServerRegistry()

        expect:
        registry.getResourceProvider("unknown").isEmpty()
    }

    def "full constructor accepts tools and resources"() {
        given:
        def tp = createToolProvider("server1", "Tool Server", 1)
        def rp = createResourceProvider("server2", "Resource Server", 2)

        when:
        def registry = new McpServerRegistry([tp], [rp])

        then:
        registry.size() == 2
        registry.hasToolProvider("server1")
        registry.hasResourceProvider("server2")
        registry.serverNames().containsAll(["server1", "server2"])
    }

    def "serverNames returns union of tool and resource providers"() {
        given:
        def tp = createToolProvider("shared", "Shared Server", 1)
        def rp = createResourceProvider("shared", "Shared Server", 2)
        def tp2 = createToolProvider("tools-only", "Tools", 1)
        def rp2 = createResourceProvider("resources-only", "Resources", 1)

        when:
        def registry = new McpServerRegistry([tp, tp2], [rp, rp2])

        then: "shared server name is not duplicated"
        registry.size() == 3
        registry.serverNames().containsAll(["shared", "tools-only", "resources-only"])
    }

    def "hasToolProvider and hasResourceProvider are independent"() {
        given:
        def registry = new McpServerRegistry()
        registry.register(createToolProvider("tools", "Tools", 1))
        registry.registerResource(createResourceProvider("resources", "Resources", 1))

        expect:
        registry.hasToolProvider("tools")
        !registry.hasResourceProvider("tools")
        !registry.hasToolProvider("resources")
        registry.hasResourceProvider("resources")
    }

    def "backward-compatible single-arg constructor works"() {
        given:
        def tp = createToolProvider("server", "Server", 2)

        when:
        def registry = new McpServerRegistry([tp])

        then:
        registry.hasToolProvider("server")
        registry.size() == 1
    }

    // ── helpers ──

    private McpToolProvider createToolProvider(String name, String desc, int toolCount) {
        def tools = (1..toolCount).collect { new McpToolDefinition("tool$it", "Tool $it", "{}") }
        return new McpToolProvider() {
            String getServerName() { name }
            String getServerDescription() { desc }
            List<McpToolDefinition> getTools() { tools }
            McpToolResult execute(String toolName, Map<String, Object> args, TenantContext tenant) {
                McpToolResult.success("executed $toolName")
            }
        }
    }

    private McpResourceProvider createResourceProvider(String name, String desc, int resourceCount) {
        def resources = (1..resourceCount).collect {
            new McpResourceDefinition("docs://res$it", "Resource $it", "text/markdown", "Description $it")
        }
        return new McpResourceProvider() {
            String getServerName() { name }
            String getServerDescription() { desc }
            List<McpResourceDefinition> getResources() { resources }
            Optional<McpResourceContent> read(String uri, TenantContext tenant) {
                def res = resources.find { it.uri() == uri }
                res ? Optional.of(new McpResourceContent(uri, "text/markdown", "Content of $uri")) : Optional.empty()
            }
        }
    }
}
