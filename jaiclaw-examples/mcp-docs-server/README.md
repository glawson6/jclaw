# MCP Docs Server

## Problem

Developers integrating JaiClaw need quick access to architecture docs, operations guides, and feature references — ideally from within their AI-powered tools (Claude Code, Cursor, VS Code with MCP plugins). Hosting documentation as a static website doesn't integrate with MCP-aware clients that can browse resources and search programmatically.

## Solution

This example runs a JaiClaw gateway that exposes the full JaiClaw documentation set as an **MCP resource server** with a companion **search tool**. MCP clients can:

- Browse all available docs via `resources/list`
- Read any document by URI via `resources/read`
- Full-text keyword search via the `search_docs` tool

The `jaiclaw-docs` extension loads ~27 markdown files from the classpath covering architecture, operations, features, FAQs, and the developer guide.

## Architecture

```
MCP Client (Claude Code, Cursor, etc.)
    │
    │  SSE / JSON-RPC 2.0
    ▼
┌────────────────────────────────────────┐
│  McpSseServerController                │
│  /mcp/docs/sse + /mcp/docs/jsonrpc     │
├────────────────────────────────────────┤
│  McpController (REST)                  │
│  GET  /mcp/docs/resources              │
│  POST /mcp/docs/resources/read         │
│  POST /mcp/docs/tools/search_docs      │
├────────────────────────────────────────┤
│  DocsMcpResourceProvider               │  ← resources/list, resources/read
│  DocsMcpToolProvider                   │  ← tools/call (search_docs)
│         │                              │
│         ▼                              │
│  DocsRepository                        │  ← classpath:docs/**/*.md
│  (in-memory, keyword search)           │
└────────────────────────────────────────┘
```

## Design

- **MCP dual-capability server**: The `docs` server name is shared by both a `McpResourceProvider` (for browsing) and a `McpToolProvider` (for search). The gateway merges both into a single logical server.
- **Classpath loading**: `DocsRepository` scans `classpath:docs/**/*.md` at startup via Spring's `PathMatchingResourcePatternResolver`. No file system access needed at runtime.
- **URI scheme**: `docs://{path}` — e.g., `docs://architecture`, `docs://features/browser`, `docs://dev-guide/core-modules`.
- **Keyword search**: Simple in-memory keyword matching with weighted scoring (name > tags > content frequency). No vector store or external search engine required.

## Build & Run

### Prerequisites

- Java 21+
- `ANTHROPIC_API_KEY` environment variable (for the agent runtime; not needed for MCP resource browsing)

### Build

```bash
export JAVA_HOME=/Users/tap/.sdkman/candidates/java/21.0.9-oracle
./mvnw package -pl :jaiclaw-example-mcp-docs-server -am -DskipTests
```

### Run

```bash
ANTHROPIC_API_KEY=your-key \
  java -jar jaiclaw-examples/mcp-docs-server/target/jaiclaw-example-mcp-docs-server-0.1.0-SNAPSHOT.jar
```

Or with Maven:

```bash
ANTHROPIC_API_KEY=your-key \
  ./mvnw spring-boot:run -pl :jaiclaw-example-mcp-docs-server
```

### Verify

```bash
# List all MCP servers (should show "docs" with toolCount and resourceCount)
curl http://localhost:8888/mcp

# List available documentation resources
curl http://localhost:8888/mcp/docs/resources

# Read a specific document
curl -X POST http://localhost:8888/mcp/docs/resources/read \
  -H 'Content-Type: application/json' \
  -d '{"uri": "docs://architecture"}'

# Search documentation
curl -X POST http://localhost:8888/mcp/docs/tools/search_docs \
  -H 'Content-Type: application/json' \
  -d '{"query": "multi-tenancy", "maxResults": 3}'
```

### MCP Client Configuration

To use with Claude Code or other MCP clients, configure the SSE transport:

```json
{
  "mcpServers": {
    "jaiclaw-docs": {
      "url": "http://localhost:8888/mcp/docs/sse"
    }
  }
}
```
