# Camel HTML Summarizer

## Problem

You have a directory of HTML files — scraped articles, reports, documentation — and need concise summaries of each. Manually reading and summarizing is tedious. You want a drop-folder workflow: put an HTML file in, get a summary out, with no code changes needed per file.

## Solution

This example uses JaiClaw's Camel SEDA routing to build a filesystem-based summarization pipeline. Apache Camel watches an inbox directory for HTML files, routes them through a JaiClaw agent (with the `summarize` skill), and writes the summaries as text files to an outbox directory. The entire pipeline is async and file-driven — no HTTP endpoints, no manual triggers.

## Architecture

```
{app.inbox}/*.html
       |
       v
[HtmlIngestRoute]  file:{inbox}  (polls for .html, moves processed to .done/)
       |
       v
seda:jaiclaw-html-summarizer-in  -->  handler.onMessage()  -->  async agent
                                                                     |
                                          sendMessage(response) -----+
                                                 |
                                                 v
                                  seda:jaiclaw-html-summarizer-out
                                                 |
                                                 v
                                  [SummaryLoggerRoute]  file:{outbox}
                                                 |
                                                 v
                                  {app.outbox}/{name}-summary.txt
```

### Key Classes

| Class | Role |
|-------|------|
| `CamelHtmlSummarizerApp` | Spring Boot entry point |
| `HtmlIngestRoute` | Camel RouteBuilder — watches `${app.inbox}` for `.html` files, sets peerId from filename, forwards to SEDA inbound |
| `SummaryLoggerRoute` | Camel RouteBuilder — consumes from SEDA outbound, writes `{name}-summary.txt` to `${app.outbox}` |

The JaiClaw auto-configuration (`JaiClawCamelAutoConfiguration`) handles:
- Registering the `html-summarizer` channel from YAML config
- Creating the SEDA inbound consumer route that dispatches to the agent
- Detecting that `SummaryLoggerRoute` already consumes the outbound SEDA queue (skips logger fallback)

## Design

**Why filesystem?** The Camel file component is zero-dependency and universally available. It demonstrates the SEDA routing pattern without requiring Kafka, S3, or any external service. The same architecture works by swapping `file:` URIs for `aws2-s3:`, `kafka:`, or any of Camel's 390+ connectors.

**Why SEDA?** The `seda:` endpoints decouple the inbound file polling from agent processing and outbound writing. Each stage runs on its own thread pool. If the agent is slow, files still get picked up and queued. If multiple files arrive at once, they're processed concurrently.

**Configurable paths:** Inbox and outbox directories are configurable via `app.inbox` and `app.outbox` properties (env vars `APP_INBOX` / `APP_OUTBOX`). Defaults to `target/data/inbox` and `target/data/outbox` so Maven cleans them automatically.

**File lifecycle:** Processed HTML files are moved to `{inbox}/.done/` (not deleted), so you can re-process by moving them back. The `readLock=changed` option prevents picking up files that are still being written.

**Filename as session key:** The HTML filename (without extension) is used as the `peerId`, which becomes part of the agent session key. This means re-processing the same file continues the same conversation context.

**Sample HTML files:** Three test HTML files are included in `src/test/resources/html/` for quick testing without authoring your own.

## Build & Run

### Prerequisites

- Java 21+
- A MiniMax API key (the example defaults to MiniMax's Anthropic-compatible endpoint)

### Environment Variables

```bash
# Required — MiniMax API key (used via Anthropic-compatible endpoint)
export ANTHROPIC_API_KEY=sk-cp-your-key-here

# Optional — override defaults
export AI_PROVIDER=minimax          # default: minimax (native API)
export ANTHROPIC_BASE_URL=https://api.minimax.io/anthropic  # default
export ANTHROPIC_MODEL=M2-her      # default
export MINIMAX_API_KEY=sk-your-key  # for native MiniMax API

# Optional — override inbox/outbox paths (default: target/data/inbox, target/data/outbox)
export APP_INBOX=/path/to/custom/inbox
export APP_OUTBOX=/path/to/custom/outbox
```

### Build

```bash
export JAVA_HOME=/Users/tap/.sdkman/candidates/java/21.0.9-oracle

# From the jaiclaw root:
./mvnw package -pl :jaiclaw-example-camel-html-summarizer -am -DskipTests
```

### Run

```bash
cd jaiclaw-examples/camel-html-summarizer

# Create the inbox directory
mkdir -p target/data/inbox

# Start the application
java -jar target/jaiclaw-example-camel-html-summarizer-0.1.0-SNAPSHOT.jar
```

Or with Maven:

```bash
./mvnw spring-boot:run -pl :jaiclaw-example-camel-html-summarizer
```

### Verify

1. Copy the sample HTML files into the inbox:

```bash
cp src/test/resources/html/*.html target/data/inbox/
```

2. Watch the logs — you should see:
   - `html-file-ingest` route picks up each file
   - Agent processes the content with the `summarize` skill
   - `Wrote summary to target/data/outbox/edge-computing-summary.txt`
   - (repeat for `quantum-computing` and `green-hydrogen`)

3. Check the output:

```bash
ls target/data/outbox/
# edge-computing-summary.txt  quantum-computing-summary.txt  green-hydrogen-summary.txt

cat target/data/outbox/edge-computing-summary.txt
```

4. The original files move to `target/data/inbox/.done/`.
