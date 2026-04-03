# Camel HTML Summarizer (Embabel)

## Problem

HTML files from web scraping, RSS feeds, or content pipelines need to be summarized into structured data (JSON with topics, sentiment, reading time) — not free-form text. A traditional LLM chat loop produces unstructured prose, requiring extra parsing and validation. You need typed, machine-readable output that downstream systems can consume directly.

## Solution

This example combines **Apache Camel** file watching with **Embabel GOAP planning** via the `EmbabelAgentLoopDelegate`. When an HTML file lands in the inbox, the Embabel planner automatically chains two LLM-powered actions — content extraction and summarization — producing a typed `HtmlSummary` record serialized as JSON. The JaiClaw message pipeline handles routing; Embabel handles structured orchestration.

This is the Embabel variant of the `camel-html-summarizer` example. Where the original uses JaiClaw's default ChatClient loop for free-form summaries, this version delegates to Embabel's GOAP planner for typed, structured output.

## Architecture

```
HTML file → HtmlIngestRoute (file:inbox → seda:in)
  → CamelChannelAdapter → GatewayService.handleMessage()
    → AgentRuntime.executeSync()
      → AgentLoopDelegateRegistry.resolve() → EmbabelAgentLoopDelegate
        → AgentPlatform.runAgentFrom(HtmlSummaryAgent, input=htmlContent)
          → GOAP planner chains:
              extractContent(String) → ContentAnalysis
              summarize(ContentAnalysis) → HtmlSummary (@AchievesGoal)
        → ObjectMapper.writeValueAsString(HtmlSummary)
      → AssistantMessage(jsonContent)
    → sendMessage(response)
  → seda:out → SummaryLoggerRoute (logs structured JSON)
```

**Key classes:**

| Class | Role |
|-------|------|
| `HtmlSummaryAgent` | Embabel `@Agent` with two `@Action` methods and GOAP goal |
| `ContentAnalysis` | Intermediate blackboard type (precondition for summarize) |
| `HtmlSummary` | Goal type — structured output serialized to JSON |
| `HtmlIngestRoute` | Camel file watcher → SEDA inbound queue |
| `SummaryLoggerRoute` | SEDA outbound queue → console logger |
| `EmbabelAgentLoopDelegate` | Bridges JaiClaw's agent pipeline to Embabel's `AgentPlatform` |

## Design

- **GOAP over prompt chaining**: Instead of a single monolithic prompt, the agent decomposes summarization into extraction + summarization. The GOAP planner resolves the type dependency (`String → ContentAnalysis → HtmlSummary`) automatically.
- **Typed output**: Both actions use `OperationContext.ai().createObject()` which forces the LLM to produce valid JSON matching the target record schema. No parsing, no "here is your summary" preamble.
- **Delegate pattern**: The `EmbabelAgentLoopDelegate` is a reusable bridge — any JaiClaw agent can opt into Embabel by setting `loop-delegate.delegate-id: embabel` in its config. The delegate serializes the goal object to JSON for the message pipeline.
- **Stateless processing**: Each HTML file gets a fresh ephemeral session (no conversation history), matching the batch-processing nature of file summarization.
- **Three-layer LLM config**: Embabel model configuration has three independent layers that must be aligned (see `application.yml` comments for details):
  1. **`embabel.models.default-llm`** — selects which registered LLM service to use (must match a name from the Embabel provider starter, e.g., `claude-sonnet-4-5`)
  2. **`spring.ai.anthropic.*`** — controls the HTTP client (API key, base URL, model name sent in the request body)
  3. **Maven dependency** — which Embabel provider starter is on the classpath (`embabel-agent-starter-anthropic` for Anthropic-compatible endpoints; do NOT mix with `embabel-agent-starter-minimax` as it creates a competing OpenAI client)

## Build & Run

### Prerequisites

- Java 21
- `ANTHROPIC_API_KEY` environment variable (or configure a different AI provider)

### Build

```bash
export JAVA_HOME=/Users/tap/.sdkman/candidates/java/21.0.9-oracle

# From the jaiclaw root directory
./mvnw package -pl :jaiclaw-example-camel-html-summarizer-embabel -am -DskipTests
```

### Run

```bash
cd jaiclaw-examples/camel-html-summarizer-embabel
../../mvnw spring-boot:run
```

### Verify

1. Create the inbox directory if it doesn't exist:
   ```bash
   mkdir -p target/data/inbox
   ```

2. Drop an HTML file:
   ```bash
   echo '<html><body><h1>Spring AI 1.1</h1><p>Spring AI provides a familiar Spring-based API for integrating with AI models.</p></body></html>' > target/data/inbox/test.html
   ```

3. Watch the logs for:
   - `Routing to agent delegate 'embabel'` — confirms delegate dispatch
   - `Executing Embabel agent 'HtmlSummaryAgent'` — GOAP execution starting
   - `===== STRUCTURED SUMMARY [test] =====` — JSON output with `summary`, `topics`, `sentiment`, `readingTimeMinutes`

### Using with MiniMax

This example is pre-configured for MiniMax's Anthropic-compatible endpoint. Set:

```bash
export ANTHROPIC_API_KEY=your-minimax-api-key
export ANTHROPIC_BASE_URL=https://api.minimax.io/anthropic
export ANTHROPIC_MODEL=MiniMax-M2.7
```

**MiniMax thinking filter**: MiniMax reasoning models always return thinking content blocks in the response, even when thinking mode is disabled. The `MiniMaxThinkingFilter` class wraps Embabel's ChatModel to filter these blocks, ensuring only text generations reach the JSON parser. This filter is only needed for MiniMax — it's a no-op for Anthropic models that don't return thinking blocks.
