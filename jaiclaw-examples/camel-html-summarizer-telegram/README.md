# Camel HTML Summarizer (Telegram)

## Problem

You have a directory of HTML files — scraped articles, reports, documentation — and need concise summaries delivered to a Telegram chat. You want a drop-folder workflow: put an HTML file in, get the summary sent directly to your Telegram bot conversation, with no manual intervention.

## Solution

This example extends the base Camel HTML Summarizer by replacing the console/file output with Telegram delivery. Apache Camel watches an inbox directory for HTML files, routes them through a JaiClaw agent for AI-powered summarization, and sends the results to a configured Telegram chat via the Telegram Bot API. The pipeline is fully async and file-driven.

## Architecture

```
{app.inbox}/*.html
       |
       v
[HtmlIngestRoute]  file:{inbox}  (polls for .html, moves processed to .done/)
       |
       v
seda:jaiclaw-html-summarizer-telegram-in  -->  handler.onMessage()  -->  async agent
                                                                             |
                                          sendMessage(response) -------------+
                                                 |
                                                 v
                                  seda:jaiclaw-html-summarizer-telegram-out
                                                 |
                                                 v
                                  [TelegramSummaryRoute]
                                                 |
                                                 v
                                  ChannelRegistry.get("telegram")
                                                 |
                                                 v
                                  TelegramAdapter.sendMessage(chatId, summary)
                                                 |
                                                 v
                                  POST https://api.telegram.org/bot{TOKEN}/sendMessage
                                                 |
                                                 v
                                  User receives: "**[filename]**\n\nSummary text..."
```

### Key Classes

| Class | Role |
|-------|------|
| `CamelHtmlSummarizerTelegramApp` | Spring Boot entry point |
| `HtmlIngestRoute` | Camel RouteBuilder — watches `${app.inbox}` for `.html` files, sets peerId from filename, forwards to SEDA inbound |
| `TelegramSummaryRoute` | Camel RouteBuilder — consumes from SEDA outbound, looks up `TelegramAdapter` via `ChannelRegistry`, sends summary with configured chat ID |

The JaiClaw auto-configuration (`JaiClawCamelAutoConfiguration`) handles:
- Registering the `html-summarizer-telegram` channel from YAML config
- Creating the SEDA inbound consumer route that dispatches to the agent
- Detecting that `TelegramSummaryRoute` already consumes the outbound SEDA queue (skips logger fallback)

## Design

**Why not use the built-in `outbound: "telegram"` bridge?** The Camel auto-config's cross-channel bridge passes the Camel exchange's `peerId` header (the filename) as the Telegram chat ID. Telegram requires a valid numeric chat ID to deliver messages. This example uses a custom outbound route that sets the correct chat ID from configuration.

**Why ChannelRegistry lookup?** Rather than injecting `TelegramAdapter` directly, the route looks it up via `ChannelRegistry.get("telegram")`. This follows the standard JaiClaw pattern for cross-channel communication and ensures the adapter is fully initialized by auto-configuration before use.

**Stateless channel:** Each HTML file gets a fresh ephemeral session — no conversation history accumulates. This is ideal for batch summarization where each file is independent.

**File lifecycle:** Processed HTML files are moved to `{inbox}/.done/` (not deleted), so you can re-process by moving them back. The `readLock=changed` option prevents picking up files that are still being written.

## Build & Run

### Prerequisites

- Java 21+
- An Anthropic API key (or compatible endpoint)
- A Telegram Bot Token (get one from [@BotFather](https://t.me/BotFather))
- A Telegram Chat ID — the numeric ID of the conversation where the bot sends summaries. To get one:
  1. Find your bot in Telegram and send it any message (e.g. `/start`)
  2. Call `curl https://api.telegram.org/bot{TOKEN}/getUpdates`
  3. Look for `"chat":{"id":123456789}` in the response — that number is your chat ID
  4. For group chats: add the bot to the group, send a message, then call `getUpdates` (group IDs are negative, e.g. `-100123456789`)

### Environment Variables

```bash
# Required
export ANTHROPIC_API_KEY=sk-ant-your-key-here
export TELEGRAM_BOT_TOKEN=123456:ABC-your-bot-token
export TELEGRAM_CHAT_ID=123456789

# Optional — override defaults
export AI_PROVIDER=anthropic           # default: anthropic
export ANTHROPIC_BASE_URL=https://api.anthropic.com  # default
export ANTHROPIC_MODEL=claude-haiku-4-5  # default
export APP_INBOX=/path/to/custom/inbox   # default: target/data/inbox
```

### Build

```bash
export JAVA_HOME=/Users/tap/.sdkman/candidates/java/21.0.9-oracle

# From the jaiclaw root:
./mvnw package -pl :jaiclaw-example-camel-html-summarizer-telegram -am -DskipTests
```

### Run

```bash
cd jaiclaw-examples/camel-html-summarizer-telegram

# Create the inbox directory
mkdir -p target/data/inbox

# Start the application
java -jar target/jaiclaw-example-camel-html-summarizer-telegram-0.1.0-SNAPSHOT.jar
```

Or with Maven:

```bash
./mvnw spring-boot:run -pl :jaiclaw-example-camel-html-summarizer-telegram
```

### Verify

1. Drop an HTML file into the inbox:

```bash
echo '<html><body><h1>Test</h1><p>Spring AI provides a unified API for integrating AI models into Spring applications.</p></body></html>' \
  > jaiclaw-examples/camel-html-summarizer-telegram/target/data/inbox/test.html
```

2. Watch the logs — you should see:
   - `html-file-ingest` route picks up the file
   - Agent processes the content
   - `Sent summary for [test] to Telegram chat 123456789`

3. Check your Telegram — you should receive a message:
   ```
   **test**

   Spring AI provides a unified API for integrating AI models...
   ```

4. The original file moves to `target/data/inbox/.done/`.
