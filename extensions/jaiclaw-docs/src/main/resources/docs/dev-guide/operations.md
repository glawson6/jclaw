# JaiClaw Operations Guide

[Back to Developer Guide](../JAICLAW-DEVELOPER-GUIDE.md)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start Scripts](#quick-start-scripts)
3. [Running Locally](#running-locally)
4. [Running with Docker](#running-with-docker)
5. [Running on Kubernetes](#running-on-kubernetes)
6. [Running on Bare Metal](#running-on-bare-metal)
7. [Environment Variables Reference](#environment-variables-reference)
8. [Security Modes](#security-modes)
9. [LLM Provider Configuration](#llm-provider-configuration)
10. [Channel Setup](#channel-setup)
11. [MCP Server Hosting](#mcp-server-hosting)
12. [Multiple Instances](#multiple-instances)
13. [Building](#building)
14. [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java | 21 (Oracle or Temurin) | Required for local builds and shell |
| Docker | Latest | For `quickstart.sh` and `start.sh docker` |
| Maven | Bundled (wrapper) | `./mvnw` — no separate install needed |
| LLM API Key | At least one | Anthropic, OpenAI, Google Gemini, or Ollama |

```bash
# Set JAVA_HOME (required)
export JAVA_HOME=/Users/tap/.sdkman/candidates/java/21.0.9-oracle
```

---

## Quick Start Scripts

### quickstart.sh — First-Time Setup

```bash
git clone https://github.com/jaiclaw/jaiclaw.git && cd jaiclaw
ANTHROPIC_API_KEY=sk-ant-... ./quickstart.sh
```

Detects Java, builds Docker image, starts Docker Compose, optionally pulls Ollama.

```bash
./quickstart.sh --force-build    # rebuild Docker image
./quickstart.sh --reconfigure    # re-run interactive setup (now includes OAuth providers)
AI_PROVIDER=anthropic ANTHROPIC_API_KEY=sk-ant-... ./quickstart.sh --non-interactive --docker
```

The reconfigure wizard includes OAuth providers (5-9): Chutes, OpenAI Codex, Gemini CLI, Qwen, MiniMax.

### setup.sh — Developer Setup

```bash
./setup.sh                       # build + launch shell
./setup.sh --gateway             # build + launch gateway
./setup.sh --build-only          # build only
```

Installs Java 21 via SDKMAN if needed.

### start.sh — Daily Driver

```bash
./start.sh                       # gateway locally (default)
./start.sh shell                 # interactive CLI (local Java)
./start.sh cli                   # interactive CLI (Docker)
./start.sh docker                # gateway via Docker Compose
./start.sh local                 # gateway locally (same as default)
./start.sh login                 # list available OAuth providers
./start.sh login chutes          # run OAuth login for a provider
./start.sh auth                  # show auth profile status (colored table)
./start.sh auth json             # show auth profile status (JSON)
./start.sh stop                  # stop Docker Compose stack
./start.sh logs                  # tail gateway logs
./start.sh --force-build         # rebuild then start gateway
./start.sh --force-build docker  # rebuild Docker image then start
./start.sh --force-build cli     # rebuild shell image then start
./start.sh help                  # show all commands
```

Configuration is read from `$JAICLAW_ENV_FILE` (default: `docker-compose/.env`). If `~/.jaiclawrc` exists, it's sourced to set `JAICLAW_ENV_FILE`.

All launch commands (`local`, `shell`, `cli`, `docker`) automatically check OAuth token status on startup and print a non-blocking warning if tokens are expiring or expired.

### Auth & OAuth Scripts

| Script | Purpose |
|--------|---------|
| `scripts/auth-status.sh` | Standalone auth status checker (3 modes: `full`, `json`, `simple`) |
| `scripts/auth-monitor.sh` | Cron-compatible expiry monitor (ntfy.sh + email notifications) |
| `scripts/setup-auth-monitor.sh` | Interactive wizard to install cron/systemd timer |
| `scripts/lib/docker-auth-dirs.sh` | Docker volume mapping for credential directories |
| `JaiClawAuth.java` | JBang script for OAuth login/status/logout outside the REPL |

OAuth providers: `chutes`, `openai-codex`, `google-gemini-cli`, `qwen-portal`, `minimax-portal`.

Docker mode auto-mounts credential directories (`~/.jaiclaw`, `~/.claude`, `~/.codex`, `~/.qwen`, `~/.minimax`) as read-only volumes. Control with `JAICLAW_DOCKER_AUTH_DIRS` (`auto`/`all`/`none`/comma-separated keys).

---

## Running Locally

### Shell (Interactive CLI)

```bash
# Anthropic (default)
ANTHROPIC_API_KEY=sk-ant-... ./mvnw spring-boot:run -pl :jaiclaw-shell

# OpenAI
AI_PROVIDER=openai OPENAI_ENABLED=true OPENAI_API_KEY=sk-... ./mvnw spring-boot:run -pl :jaiclaw-shell

# Ollama (free, local)
AI_PROVIDER=ollama OLLAMA_ENABLED=true ./mvnw spring-boot:run -pl :jaiclaw-shell

# Google Gemini
AI_PROVIDER=google-genai GEMINI_ENABLED=true GEMINI_API_KEY=... ./mvnw spring-boot:run -pl :jaiclaw-shell
```

### Shell Commands

| Command | Description |
|---|---|
| `chat <message>` | Send a message to the agent |
| `new-session` | Start a fresh conversation |
| `sessions` | List all active sessions |
| `session-history` | Show messages in current session |
| `status` | System status (identity, tools, sessions) |
| `config` | Current configuration |
| `models` | Configured LLM providers |
| `tools` | Available tools |
| `skills` | Loaded skills |
| `plugins` | Loaded plugins |
| `onboard` | Interactive setup wizard |

### Gateway (Local)

```bash
ANTHROPIC_API_KEY=sk-ant-... ./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

Endpoints served:

| Endpoint | Method | Description |
|---|---|---|
| `/api/chat` | POST | Synchronous chat (requires `X-API-Key`) |
| `/api/health` | GET | Health check |
| `/api/channels` | GET | List registered channels |
| `/webhook/{channel}` | POST | Inbound webhooks |
| `/ws/session/{key}` | WS | Streaming WebSocket |
| `/mcp/**` | Various | MCP server hosting (requires `X-API-Key`) |

Test the gateway:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $(cat ~/.jaiclaw/api-key)" \
  -d '{"content": "hello"}'

curl http://localhost:8080/api/health
curl http://localhost:8080/api/channels
```

### Cron Manager

```bash
./mvnw spring-boot:run -pl :jaiclaw-cron-manager
```

Runs scheduled jobs defined in JSON persistence files.

---

## Running with Docker

### Docker Compose

```bash
# Start gateway
./start.sh docker

# Start interactive shell
./start.sh cli

# Stop
./start.sh stop

# View logs
./start.sh logs
```

Docker Compose file: `docker-compose/docker-compose.yml`
Environment file: `docker-compose/.env`

### Building Docker Images

```bash
# Build gateway image
./mvnw package k8s:build -pl :jaiclaw-gateway-app -am -Pk8s -DskipTests

# Build shell image
./mvnw package k8s:build -pl :jaiclaw-shell -am -Pk8s -DskipTests

# Build both
./mvnw package k8s:build -pl :jaiclaw-gateway-app,:jaiclaw-shell -am -Pk8s -DskipTests
```

Images use `eclipse-temurin:21-jre` base. Image name convention: `io.jaiclaw/<module>:<version>`.

---

## Running on Kubernetes

### Deploy with JKube

```bash
# Build image
./mvnw package k8s:build -pl :jaiclaw-gateway-app -am -Pk8s -DskipTests

# Push to registry
./mvnw k8s:push -pl :jaiclaw-gateway-app -Pk8s

# Generate resources and deploy
./mvnw k8s:resource k8s:apply -pl :jaiclaw-gateway-app -Pk8s
```

### Required Secrets

```bash
kubectl create secret generic jaiclaw-secrets \
  --from-literal=JAICLAW_API_KEY=jaiclaw_ak_... \
  --from-literal=OPENAI_API_KEY=sk-... \
  --from-literal=ANTHROPIC_API_KEY=sk-ant-... \
  --from-literal=GEMINI_API_KEY=... \
  --from-literal=TELEGRAM_BOT_TOKEN=123456:ABC... \
  --from-literal=SLACK_BOT_TOKEN=xoxb-... \
  --from-literal=SLACK_SIGNING_SECRET=... \
  --from-literal=DISCORD_BOT_TOKEN=... \
  --from-literal=EMAIL_USERNAME=bot@example.com \
  --from-literal=EMAIL_PASSWORD=... \
  --from-literal=TWILIO_ACCOUNT_SID=AC... \
  --from-literal=TWILIO_AUTH_TOKEN=... \
  --from-literal=TWILIO_FROM_NUMBER=+15551234567
```

### Helm Chart

Shared Helm chart at `helm/spring-boot-app/` with `workloadType` toggle:

```yaml
# values-jaiclaw-gateway-app.yaml
workloadType: deployment
replicaCount: 2
image:
  repository: registry.taptech.net/jaiclaw-gateway-app
  tag: latest
service:
  port: 8080
ingress:
  enabled: true
  host: jaiclaw.taptech.net
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "gateway"
  - name: JAICLAW_SECURITY_MODE
    value: "api-key"
```

### Architecture

```
┌─────────────────── k8s cluster ───────────────────┐
│                                                     │
│  jaiclaw-gateway (Deployment, 2+ replicas)           │
│    - webhook receivers, WS control plane            │
│    - channel adapters, session routing              │
│    - Port 8080                                      │
│                                                     │
│  jaiclaw-app (Deployment, 2+ replicas)               │
│    - agent runtime, tools, skills                   │
│    - plugins, memory, Spring AI clients             │
│    - Port 8081                                      │
│                                                     │
│  ngrok Ingress → jaiclaw.taptech.net                 │
│  ConfigMap / Secrets → API keys, tokens             │
│                                                     │
│  External: Redis (sessions), Ollama (local LLM),   │
│            Kafka (events, optional)                 │
└─────────────────────────────────────────────────────┘
```

---

## Running on Bare Metal

### SSD Nodes Deploy Pattern

For bare-metal servers (e.g., SSD Nodes):

1. Install Java 21 and Docker
2. Clone the repository
3. Build the fat JAR:
   ```bash
   ./mvnw package -pl :jaiclaw-gateway-app -am -DskipTests
   ```
4. Run directly:
   ```bash
   java -jar apps/jaiclaw-gateway-app/target/jaiclaw-gateway-app-*.jar \
     --spring.profiles.active=gateway
   ```
5. Use systemd for process management:
   ```ini
   [Unit]
   Description=JaiClaw Gateway
   After=network.target

   [Service]
   Type=simple
   User=jaiclaw
   EnvironmentFile=/etc/jaiclaw/env
   ExecStart=/usr/bin/java -jar /opt/jaiclaw/jaiclaw-gateway-app.jar
   Restart=on-failure
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

External services (Redis, Ollama, Kafka) can run on the same or adjacent hosts.

---

## Environment Variables Reference

### HTTP Proxy

| Variable | Required | Default | Description |
|---|---|---|---|
| `HTTPS_PROXY` | No | — | Proxy URL for HTTPS traffic (e.g. `http://user:pass@proxy:8080`) |
| `HTTP_PROXY` | No | — | Proxy URL for HTTP traffic (fallback if `HTTPS_PROXY` not set) |
| `NO_PROXY` | No | — | Comma-separated hosts that bypass the proxy (e.g. `localhost,127.0.0.1,*.internal`) |

These standard env vars are auto-detected. Alternatively, configure explicitly in `application.yml`:

```yaml
jaiclaw:
  http:
    proxy:
      host: proxy.corp.com
      port: 8080
      username: user          # optional
      password: secret        # optional
      non-proxy-hosts: "localhost,127.0.0.1"
```

Explicit YAML config takes precedence over env vars. Resolution order: YAML > `HTTPS_PROXY` > `https_proxy` > `HTTP_PROXY` > `http_proxy`.

### Core

| Variable | Required | Default | Description |
|---|---|---|---|
| `JAVA_HOME` | Yes | — | Path to Java 21 JDK |
| `JAICLAW_HOME` | No | `~/.jaiclaw/` | Config directory |
| `JAICLAW_ENV_FILE` | No | `docker-compose/.env` | Path to `.env` file |
| `JAICLAW_SECURITY_MODE` | No | `api-key` | Security mode: `api-key`, `jwt`, `none` |
| `JAICLAW_API_KEY` | No | Auto-generated | Custom API key |
| `JAICLAW_API_KEY_FILE` | No | `~/.jaiclaw/api-key` | Path to API key file |
| `GATEWAY_PORT` | No | `8080` | Gateway HTTP port |

### LLM Providers

| Variable | Required | Default | Description |
|---|---|---|---|
| `AI_PROVIDER` | No | `anthropic` | Primary: `anthropic`, `openai`, `google-genai`, `ollama` |
| `ANTHROPIC_API_KEY` | One of these | — | Anthropic API key |
| `ANTHROPIC_ENABLED` | No | `true` | Enable Anthropic |
| `ANTHROPIC_MODEL` | No | `claude-sonnet-4-5` | Model name |
| `OPENAI_API_KEY` | One of these | — | OpenAI API key |
| `OPENAI_ENABLED` | No | `false` | Enable OpenAI |
| `GEMINI_API_KEY` | One of these | — | Google Gemini API key |
| `GEMINI_ENABLED` | No | `false` | Enable Gemini |
| `GEMINI_MODEL` | No | `gemini-2.0-flash` | Model name |
| `OLLAMA_ENABLED` | No | `false` | Enable Ollama |
| `OLLAMA_BASE_URL` | No | `http://localhost:11434` | Ollama API URL |

### Channels

| Variable | Required | Default | Description |
|---|---|---|---|
| `TELEGRAM_BOT_TOKEN` | For Telegram | — | Bot token from @BotFather |
| `TELEGRAM_WEBHOOK_URL` | No | — | Webhook URL (blank = polling) |
| `SLACK_BOT_TOKEN` | For Slack | — | Bot OAuth token |
| `SLACK_APP_TOKEN` | No | — | App-level token for Socket Mode |
| `SLACK_SIGNING_SECRET` | For Slack webhook | — | Signing secret (webhook mode) |
| `DISCORD_BOT_TOKEN` | For Discord | — | Bot token |
| `DISCORD_APPLICATION_ID` | For Discord webhook | — | Application ID (webhook mode) |
| `DISCORD_USE_GATEWAY` | No | `false` | Enable Gateway WebSocket mode |
| `EMAIL_IMAP_HOST` | For Email | — | IMAP server hostname |
| `EMAIL_SMTP_HOST` | For Email | — | SMTP server hostname |
| `EMAIL_USERNAME` | For Email | — | Email account username |
| `EMAIL_PASSWORD` | For Email | — | Email password or app password |
| `EMAIL_PROVIDER` | No | `imap` | Provider: `imap`, `gmail`, `outlook` |
| `EMAIL_IMAP_PORT` | No | `993` | IMAP port |
| `EMAIL_SMTP_PORT` | No | `587` | SMTP port |
| `EMAIL_POLL_INTERVAL` | No | `60` | Polling interval in seconds |
| `TWILIO_ACCOUNT_SID` | For SMS | — | Twilio Account SID |
| `TWILIO_AUTH_TOKEN` | For SMS | — | Twilio Auth Token |
| `TWILIO_FROM_NUMBER` | For SMS | — | Twilio phone number |

---

## Security Modes

| Mode | Header | Description |
|---|---|---|
| `api-key` (default) | `X-API-Key: <key>` | API key authentication |
| `jwt` | `Authorization: Bearer <token>` | JWT token authentication |
| `none` | — | No auth (development only) |

### API Key Resolution Order

1. `JAICLAW_API_KEY` environment variable
2. Key file at `JAICLAW_API_KEY_FILE` (default: `~/.jaiclaw/api-key`)
3. Auto-generate and write to `~/.jaiclaw/api-key`

```bash
# View current API key
cat ~/.jaiclaw/api-key

# Use custom key
JAICLAW_API_KEY=my-custom-key ./start.sh local

# Disable security (dev only)
JAICLAW_SECURITY_MODE=none ./start.sh local
```

### Security Hardening Profile

JaiClaw includes a `security-hardened` Spring profile that enables all opt-in security flags at once. All flags default to **off** (disabled) to preserve backward compatibility.

```bash
# Enable all security hardening flags
SPRING_PROFILES_ACTIVE=security-hardened ./start.sh local
```

Or enable individual flags in `application.yml`:

```yaml
jaiclaw:
  channels:
    slack:
      verify-signature: true          # HMAC-SHA256 verification of Slack webhooks
    telegram:
      verify-webhook: true            # Secret token verification of Telegram webhooks
      mask-bot-token: true            # Hash bot token in session keys (prevents leakage)
  tools:
    web:
      ssrf-protection: true           # Block requests to private/internal IPs
    code:
      workspace-boundary: true        # Path traversal protection in code tools
  security:
    timing-safe-api-key: true         # Constant-time API key comparison
```

| Flag | Scope | What It Does |
|---|---|---|
| `jaiclaw.channels.slack.verify-signature` | Slack webhook | HMAC-SHA256 signature + replay protection (5min window) |
| `jaiclaw.channels.telegram.verify-webhook` | Telegram webhook | `X-Telegram-Bot-Api-Secret-Token` header verification |
| `jaiclaw.channels.telegram.mask-bot-token` | Telegram sessions | SHA-256 hash prefix as accountId instead of raw token |
| `jaiclaw.tools.web.ssrf-protection` | WebFetchTool | Blocks localhost, private IPs, link-local, cloud metadata |
| `jaiclaw.tools.code.workspace-boundary` | Code tools | Path traversal prevention via WorkspaceBoundary |
| `jaiclaw.security.timing-safe-api-key` | API key auth | `MessageDigest.isEqual()` instead of `String.equals()` |

---

## LLM Provider Configuration

### Anthropic (Default)

```bash
ANTHROPIC_API_KEY=sk-ant-... ./start.sh shell
ANTHROPIC_MODEL=claude-opus-4-5 ANTHROPIC_API_KEY=sk-ant-... ./start.sh shell
```

### OpenAI

```bash
AI_PROVIDER=openai OPENAI_ENABLED=true OPENAI_API_KEY=sk-... ./start.sh shell
```

### Google Gemini

```bash
AI_PROVIDER=google-genai GEMINI_ENABLED=true GEMINI_API_KEY=... ./start.sh shell
GEMINI_MODEL=gemini-2.0-flash-lite GEMINI_ENABLED=true GEMINI_API_KEY=... ./start.sh shell
```

### Ollama (Local, Free)

```bash
brew install ollama && ollama serve
ollama pull llama3.2
AI_PROVIDER=ollama OLLAMA_ENABLED=true ./start.sh shell
```

### Multiple Providers

All four can be enabled simultaneously. `AI_PROVIDER` selects the primary `ChatClient`:

```bash
AI_PROVIDER=anthropic
ANTHROPIC_ENABLED=true
ANTHROPIC_API_KEY=sk-ant-...
OPENAI_ENABLED=true
OPENAI_API_KEY=sk-...
GEMINI_ENABLED=true
GEMINI_API_KEY=...
OLLAMA_ENABLED=true
```

---

## Channel Setup

### Local Dev Comparison

| Channel | Public Endpoint? | Setup Time | Best For |
|---|---|---|---|
| Spring Shell | No | None | Quick testing |
| REST API | No | None | API integration |
| Telegram (polling) | **No** | ~2 min | Mobile testing |
| Slack (Socket Mode) | **No** | ~5 min | Team testing |
| Discord (Gateway) | **No** | ~5 min | Community testing |
| Email (IMAP) | **No** | ~3 min | Async/document intake |
| SMS (Twilio) | Yes | ~10 min | Mobile outreach |
| WebSocket | No | None | Real-time/streaming |

### Telegram

**Local dev** (no public endpoint):
```bash
TELEGRAM_BOT_TOKEN=123456:ABC-DEF... \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```
Leave `TELEGRAM_WEBHOOK_URL` unset for polling mode.

**Production** (webhook):
```bash
TELEGRAM_BOT_TOKEN=123456:ABC-DEF... \
TELEGRAM_WEBHOOK_URL=https://jaiclaw.taptech.net/webhook/telegram \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

### Slack

**Local dev** (Socket Mode):
```bash
SLACK_BOT_TOKEN=xoxb-... \
SLACK_APP_TOKEN=xapp-... \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

**Production** (Events API):
```bash
SLACK_BOT_TOKEN=xoxb-... \
SLACK_SIGNING_SECRET=... \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

### Discord

**Local dev** (Gateway WebSocket):
```bash
DISCORD_BOT_TOKEN=... \
DISCORD_USE_GATEWAY=true \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

**Production** (Interactions webhook):
```bash
DISCORD_BOT_TOKEN=... \
DISCORD_APPLICATION_ID=... \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

### Email

```bash
EMAIL_IMAP_HOST=imap.gmail.com \
EMAIL_SMTP_HOST=smtp.gmail.com \
EMAIL_USERNAME=you@gmail.com \
EMAIL_PASSWORD=your-app-password \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

### SMS (Twilio)

```bash
TWILIO_ACCOUNT_SID=AC... \
TWILIO_AUTH_TOKEN=... \
TWILIO_FROM_NUMBER=+15551234567 \
ANTHROPIC_API_KEY=sk-ant-... \
./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

Requires ngrok for local dev: `ngrok http 8080`, then set Twilio webhook to `https://your-ngrok.ngrok.io/webhooks/sms`.

### Signal

Two modes:
- **EMBEDDED**: ProcessBuilder + JSON-RPC daemon (signal-cli)
- **HTTP_CLIENT**: REST sidecar polling (signal-cli-rest-api)

See [signal-channel-architecture](../../.claude/projects/-Users-tap-dev-workspaces-openclaw-jaiclaw/memory/signal-channel-architecture.md) for full details.

### Teams

Microsoft Teams channel adapter (in development).

---

## MCP Server Hosting

JaiClaw hosts MCP (Model Context Protocol) tool servers, making tools available to external AI clients.

```bash
# List MCP servers
curl http://localhost:8080/mcp \
  -H "X-API-Key: $(cat ~/.jaiclaw/api-key)"

# List tools for a server
curl http://localhost:8080/mcp/{serverName}/tools \
  -H "X-API-Key: $(cat ~/.jaiclaw/api-key)"

# Execute a tool
curl -X POST http://localhost:8080/mcp/{serverName}/tools/{toolName} \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $(cat ~/.jaiclaw/api-key)" \
  -d '{"arg1": "value1"}'
```

MCP tool providers implement the `McpToolProvider` SPI and are auto-discovered as Spring beans.

---

## Multiple Instances

Use `JAICLAW_HOME` to run isolated instances with separate configs:

### Shell Instances

```bash
# Personal assistant
JAICLAW_HOME=~/.jaiclaw-personal ./mvnw spring-boot:run -pl :jaiclaw-shell

# Work assistant
JAICLAW_HOME=~/.jaiclaw-work ./mvnw spring-boot:run -pl :jaiclaw-shell

# Dev/test (local LLM)
JAICLAW_HOME=~/.jaiclaw-dev ./mvnw spring-boot:run -pl :jaiclaw-shell
```

### Gateway Instances

```bash
# Production on port 8080
source ~/.jaiclaw-prod/.env
JAICLAW_HOME=~/.jaiclaw-prod SERVER_PORT=8080 ./mvnw spring-boot:run -pl :jaiclaw-gateway-app

# Staging on port 8081
source ~/.jaiclaw-staging/.env
JAICLAW_HOME=~/.jaiclaw-staging SERVER_PORT=8081 ./mvnw spring-boot:run -pl :jaiclaw-gateway-app
```

Each config directory contains its own `application-local.yml` and `.env`. Sessions are in-memory and isolated per JVM process.

---

## Building

```bash
# Compile all
./mvnw compile

# Run all tests (offline)
./mvnw test -o

# Run tests for one module
./mvnw test -pl :jaiclaw-tools -o

# Run a single Spock spec
./mvnw test -pl :jaiclaw-tools -Dtest=ToolRegistrySpec -o

# Install to local repo
./mvnw install -DskipTests

# Package as JARs
./mvnw package -DskipTests

# Build Docker images
./mvnw package k8s:build -pl :jaiclaw-gateway-app,:jaiclaw-shell -am -Pk8s -DskipTests
```

**Offline mode**: Use `-o` after initial dependency download. Nexus at tooling.taptech.net:8081 causes timeouts when unreachable. Run without `-o` when adding new dependencies.

**After editing starter code**: Always `./mvnw install -pl :jaiclaw-spring-boot-starter -DskipTests` before testing gateway-app or shell.

---

## Troubleshooting

### Nexus Timeouts During Build
```bash
./mvnw compile -o    # use offline mode
```

### Telegram Bot Not Responding
```bash
# Verify bot token
curl https://api.telegram.org/bot<TOKEN>/getMe

# Check webhook status
curl https://api.telegram.org/bot<TOKEN>/getWebhookInfo

# Switch to polling (delete webhook)
curl https://api.telegram.org/bot<TOKEN>/deleteWebhook
```

### No LLM Configured
If no `ChatClient.Builder` bean is available, `AgentRuntime` won't be created. Set at least one of: `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `GEMINI_API_KEY`, or enable Ollama.

### Port Conflicts
Default ports: Shell (no port), Gateway (8080), Ollama (11434).

### ConditionalOnBean Not Working
Ensure beans you depend on are defined in an earlier auto-configuration phase. See [design-patterns.md](design-patterns.md#auto-configuration-ordering).

### HTTP Proxy Not Working
- Verify env vars: `echo $HTTPS_PROXY` — must be a full URL like `http://proxy:8080`
- Check startup log for `HTTP proxy configured: host:port` message
- For Spring AI providers (Anthropic, OpenAI, etc.), proxy is applied via `RestClientCustomizer` with `JdkClientHttpRequestFactory`
- For built-in tools (WebFetch, WebSearch) and MCP clients, proxy is applied via `ProxyAwareHttpClientFactory`
- If using explicit YAML config, ensure both `host` and `port` are set (port > 0)

### Spring Shell Multi-Word Commands Fail from CLI
Use hyphenated aliases: `pplx-search` instead of `pplx search`. See [design-patterns.md](design-patterns.md#dual-mode-cli-build).
