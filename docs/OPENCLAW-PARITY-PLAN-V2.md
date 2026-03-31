# OpenClaw Feature Parity Plan v2

> **Status:** Draft
> **Date:** 2026-03-30
> **Predecessor:** `docs/FEATURE-COMPARISON.md` (three-way comparison)
> **Scope:** Features present in OpenClaw but missing or incomplete in JaiClaw

This plan is organized into **tiers** by strategic value and implementation complexity. Each feature section includes the gap description, proposed JaiClaw module placement, implementation approach, dependencies, and estimated scope.

---

## Tier 1: High-Impact Channel & Routing Gaps

These are the features that most directly expand JaiClaw's reach and differentiate the platform.

### 1.1 Additional Messaging Channels (17+ missing)

**Gap:** OpenClaw supports 25+ channels. JaiClaw has 7 (Telegram, Slack, Discord, Email, SMS, Signal, Teams). Missing channels include WhatsApp, iMessage, Matrix, LINE, IRC, Twitch, Nostr, Google Chat, Feishu, Zalo, WeChat, Mattermost, Nextcloud Talk, Synology Chat, BlueBubbles, Tlon/Landscape.

**Priority order** (by user demand and ecosystem value):
1. **WhatsApp** — highest demand; OpenClaw uses whatsapp-web.js (Puppeteer)
2. **Matrix** — open protocol, enterprise appeal; OpenClaw uses matrix-js-sdk
3. **iMessage** — macOS-only; OpenClaw uses AppleScript bridge
4. **LINE** — large APAC user base
5. **Google Chat** — enterprise (Google Workspace)
6. **Mattermost** — self-hosted Slack alternative; REST API
7. **IRC** — lightweight, hobbyist/dev community
8. **Twitch** — streaming/entertainment vertical
9. **Remaining** — Nostr, Feishu, Zalo, WeChat, Synology Chat, Nextcloud Talk, Tlon, BlueBubbles

**Module placement:** New modules under `channels/` following the existing pattern:
- `jaiclaw-channel-whatsapp`
- `jaiclaw-channel-matrix`
- `jaiclaw-channel-imessage`
- etc.

**Implementation approach:**
- Each channel implements `ChannelAdapter` SPI from `jaiclaw-channel-api`
- Port the protocol-specific logic from OpenClaw's TypeScript to Java equivalents
- WhatsApp: evaluate whatsapp-web.js via GraalJS or use the unofficial REST bridge pattern (Baileys for Node, called from Java via subprocess or HTTP sidecar)
- Matrix: use the matrix-java-sdk or implement REST API client directly
- iMessage: JNI/ProcessBuilder bridge to AppleScript (macOS only, conditional bean)
- Each channel gets a Spring Boot auto-configuration conditional on classpath + config

**Dependencies:** `jaiclaw-channel-api` (exists), per-channel client libraries

**Estimated scope per channel:** ~10-20 files (adapter, config, message mapper, auto-config, tests)

---

### 1.2 Group Chat Routing with @Mention Activation

**Gap:** OpenClaw routes messages in group chats using @mention-based agent selection, with configurable activation modes (always-on, explicit mention, reply-back). JaiClaw has no group chat routing — agents only respond in DMs.

**Module placement:** New module `extensions/jaiclaw-group-routing` or extend `core/jaiclaw-gateway`

**Implementation approach:**
- **MentionParser** (exists in JaiClaw) — extend to extract agent target from @mentions
- **ActivationMode enum**: `ALWAYS`, `MENTION_ONLY`, `REPLY_BACK`
- **GroupRoutingPolicy**: Per-channel, per-group configuration of activation mode
- **GroupSessionManager**: Isolate sessions per group (key: `{agentId}:{channel}:{groupId}`)
- Wire into `GatewayService` message dispatch — check activation mode before routing to agent

**Key design decisions:**
- How to identify groups vs DMs per channel (channel adapter must expose this)
- Thread binding — map channel threads (Discord threads, Slack threads, Telegram topics) to sessions

**Dependencies:** `jaiclaw-channel-api` (add `isGroup()` to `ChannelMessage`), `jaiclaw-gateway`

**Estimated scope:** ~15-20 files

---

### 1.3 Multi-Agent Routing

**Gap:** OpenClaw supports multiple isolated agents per gateway, each with its own workspace, sessions, and routing rules. JaiClaw runs a single agent per runtime (multi-tenancy is per-organization, not per-agent within a tenant).

**Module placement:** Extend `core/jaiclaw-agent` + `core/jaiclaw-gateway`

**Implementation approach:**
- **AgentRegistry**: Register multiple named agents, each with its own config, skills, tools, and workspace directory
- **AgentRoutingRule**: Map inbound channel/account/peer patterns to specific agents
- **Per-agent SessionManager**: Isolate sessions by agent ID (already in session key convention)
- **Per-agent WorkspaceMemory**: Each agent gets its own memory directory
- **Agent lifecycle**: add/bind/delete/list agents at runtime via gateway API + shell commands
- **Default agent**: Fallback when no routing rule matches

**Key design decisions:**
- Agent definition format (YAML config vs database)
- Hot-add agents without gateway restart
- Resource limits per agent (max concurrent sessions, token budget)

**Dependencies:** Multi-tenancy (complete), `AgentRuntime`, `SessionManager`, `GatewayService`

**Estimated scope:** ~25-30 files

---

## Tier 2: Security, Approval & Session Gaps

### 2.1 DM Security & Pairing Workflows

**Gap:** OpenClaw has a sophisticated unknown-sender approval model: DM pairing codes, per-channel DM policies (pairing/open/blocked), account allowlists, group-scoped allowlists, mention gating, and command gating. JaiClaw has JWT auth and rate limiting but no sender-level approval flow.

**Module placement:** Extend `core/jaiclaw-security` + new `extensions/jaiclaw-dm-security`

**Implementation approach:**
- **DmPolicy enum**: `OPEN`, `PAIRING`, `BLOCKED`
- **DmPolicyConfig**: Per-channel DM policy configuration in YAML
- **PairingCodeManager**: Generate and validate pairing codes (time-limited, single-use)
- **AllowlistStore**: Persistent approved sender list per channel (JSON file or Redis)
- **DmSecurityFilter**: Intercept inbound messages in `GatewayService`, apply DM policy before routing to agent
- **PairingFlow**: Agent-driven approval flow — unknown sender sends pairing code, agent validates and adds to allowlist
- **MentionGating**: Allow/deny list for who can @mention the agent in groups
- **CommandGating**: Restrict native command execution to allowlisted senders

**Dependencies:** `jaiclaw-gateway`, `jaiclaw-security`, channel adapter changes

**Estimated scope:** ~20-25 files

---

### 2.2 Execution Approval Workflows

**Gap:** OpenClaw agents can request human approval before running sensitive operations (shell commands, file writes, API calls). Approval requests are delivered through the messaging channel with custom UI renderers. JaiClaw's `ExplicitToolLoop` has a basic approval gate but no channel-delivered approval UX.

**Module placement:** Extend `core/jaiclaw-agent` (`ExplicitToolLoop`) + new approval rendering in `extensions/jaiclaw-messaging`

**Implementation approach:**
- **ApprovalRequest record**: Tool name, arguments, risk level, requester context
- **ApprovalRenderer SPI**: Per-channel rendering of approval prompts (Slack buttons, Discord components, Telegram inline keyboards, etc.)
- **ApprovalDeliveryService**: Send approval request to the user's channel, await response
- **ApprovalPolicy**: Configurable rules — which tools require approval, auto-approve patterns, timeout behavior
- **ExplicitToolLoop enhancement**: When approval required, pause loop → deliver approval request → resume on approve/deny
- **Timeout handling**: Auto-deny after configurable period

**Dependencies:** `jaiclaw-agent` (ExplicitToolLoop exists), channel adapters (need interactive component support)

**Estimated scope:** ~15-20 files

---

### 2.3 Session Spawning & Sub-Agents

**Gap:** OpenClaw agents can spawn sub-sessions for parallel work, yield control for interactive mode, and manage session types (main, group, cron, hook). JaiClaw has flat sessions with no spawning.

**Module placement:** Extend `core/jaiclaw-agent`

**Implementation approach:**
- **SessionType enum**: `MAIN`, `GROUP`, `CRON`, `HOOK`, `SPAWNED`
- **SessionSpawnTool**: Agent-callable tool that creates a child session with its own context
- **SessionYieldTool**: Pause agent execution and await user input (interactive mode)
- **ParentChildSession tracking**: Parent session references spawned children, children inherit context
- **Lifecycle**: Spawned sessions auto-expire, results bubble up to parent
- **Concurrency**: Spawned sessions run in parallel (virtual threads)

**Dependencies:** `SessionManager`, `AgentRuntime`

**Estimated scope:** ~15 files

---

## Tier 3: Provider & Media Gaps

### 3.1 Additional LLM Providers (16+ missing)

**Gap:** OpenClaw supports 20+ providers. JaiClaw has 4 (Anthropic, OpenAI, Gemini, Ollama). Missing: Mistral, Groq, Hugging Face, vLLM, SGLang, BytePlus/Doubao, Qianfan/Baidu, Kimi/Moonshot, xAI/Grok, Perplexity, Together, Venice, DeepSeek, OpenRouter, LiteLLM, Cloudflare AI Gateway, Vercel AI Gateway.

**Module placement:** New starter modules under `jaiclaw-starters/`:
- `jaiclaw-starter-mistral`
- `jaiclaw-starter-groq`
- `jaiclaw-starter-deepseek`
- `jaiclaw-starter-xai`
- `jaiclaw-starter-openrouter`
- etc.

**Implementation approach:**
- Most providers expose OpenAI-compatible APIs — reuse `jaiclaw-starter-openai` with custom base URL and model mappings
- Spring AI already supports many providers via community starters — evaluate what's available before building custom
- **OpenRouter / LiteLLM gateway pattern**: Single adapter that proxies to 100+ models via one endpoint
- **Priority**: OpenRouter (meta-gateway, covers many), DeepSeek (cost-effective), Groq (speed), Mistral (EU compliance)

**Key design decisions:**
- Use Spring AI community starters where available vs custom adapters
- OpenRouter as a meta-provider could cover 80% of the gap with one integration

**Estimated scope per provider:** ~5-10 files (config, model mappings, auto-config, tests)
- OpenRouter alone: ~10 files, covers dozens of models

---

### 3.2 Image Generation

**Gap:** OpenClaw integrates DALL-E and Stable Diffusion (via FAL) as agent-callable tools. JaiClaw has no image generation.

**Module placement:** New `extensions/jaiclaw-image-gen`

**Implementation approach:**
- **ImageGenerationTool**: Agent-callable tool wrapping Spring AI's `ImageModel` interface
- **Provider adapters**: OpenAI DALL-E (Spring AI has this), FAL/Stable Diffusion (custom HTTP client)
- **Image delivery**: Generated images routed back through channel (attachment on `ChannelMessage`)
- **Configuration**: Model selection, size, quality, style parameters

**Dependencies:** Spring AI `spring-ai-openai` (DALL-E support exists), FAL REST API client

**Estimated scope:** ~10-15 files

---

### 3.3 Full Media Pipeline

**Gap:** OpenClaw has FFmpeg transcoding, size caps, temp file lifecycle management, media sanitization, and image processing. JaiClaw has `MediaAnalysisProvider` SPI and `CompositeMediaAnalyzer` but no transcoding, size management, or lifecycle.

**Module placement:** Extend `extensions/jaiclaw-media`

**Implementation approach:**
- **MediaTranscoder**: FFmpeg wrapper via ProcessBuilder (audio/video format conversion)
- **MediaSizeManager**: Enforce per-channel size limits, compress/resize as needed
- **TempFileManager**: Track temp media files, auto-cleanup after TTL
- **ImageProcessor**: Resize, sanitize (strip EXIF), convert formats
- **MediaPipeline orchestrator**: Inbound attachment → analyze → transcode if needed → store → deliver

**Dependencies:** FFmpeg (system binary), `jaiclaw-media` (exists)

**Estimated scope:** ~15-20 files

---

### 3.4 Web Search Provider Diversity

**Gap:** OpenClaw integrates 7+ search providers (Brave, DuckDuckGo, Tavily, Exa, Firecrawl, Perplexity search, Grok search, Kimi search). JaiClaw has a generic `WebSearchTool` and a standalone Perplexity CLI but no multi-provider search routing.

**Module placement:** New `extensions/jaiclaw-search-providers`

**Implementation approach:**
- **SearchProvider SPI**: `search(query, options) -> List<SearchResult>`
- **Provider implementations**: Brave, DuckDuckGo, Tavily, Exa (each wraps their REST API)
- **SearchRouter**: Configurable provider selection (primary + fallback chain)
- **Firecrawl integration**: Structured web scraping as a separate tool
- **WebSearchTool enhancement**: Delegate to configured `SearchProvider`

**Dependencies:** HTTP client (JDK), API keys for each provider

**Estimated scope:** ~20-25 files (SPI + 5-6 provider implementations)

---

## Tier 4: Voice, Apps & Device Gaps

### 4.1 Voice Wake & Talk Mode

**Gap:** OpenClaw has Voice Wake (macOS/iOS wake-word activation), Talk Mode (continuous voice on Android), and a voice overlay on macOS. JaiClaw has TTS/STT SPI + OpenAI provider but no wake-word, continuous voice, or native voice UX.

**Module placement:** Extend `extensions/jaiclaw-voice` + native companion apps (new repos)

**Implementation approach:**
- **Voice Wake (macOS)**: Requires native app (Swift/macOS) — separate repo/module
- **Talk Mode (Android)**: Requires native app — separate repo/module
- **Server-side voice session**: Extend `jaiclaw-voice` to support continuous streaming STT → agent → TTS loop
- **WebSocket voice transport**: Real-time audio streaming over WebSocket
- **Wake word detection**: Consider Porcupine (Picovoice) or Snowboy for server-side wake-word, but OpenClaw does this on-device

**Reality check:** Voice Wake and Talk Mode are native app features. JaiClaw can build the server-side voice session infrastructure, but the native UX requires companion apps (see 4.2).

**Estimated scope:** Server-side: ~15-20 files. Native apps: separate projects.

---

### 4.2 Native Companion Apps (macOS, iOS, Android)

**Gap:** OpenClaw ships macOS (menu bar), iOS, and Android apps with push notifications, device control, voice overlay, canvas host, and auto-updates. JaiClaw is server-side only.

**Module placement:** New top-level repos or `apps/` directory:
- `apps/jaiclaw-macos` (Swift/SwiftUI)
- `apps/jaiclaw-ios` (Swift/SwiftUI)
- `apps/jaiclaw-android` (Kotlin/Jetpack Compose)

**Implementation approach:**
- **Phase 1 — macOS menu bar app**: Status indicator, quick chat, gateway control. Connect to JaiClaw gateway via WebSocket. SwiftUI + Observation framework.
- **Phase 2 — iOS app**: Push notifications (APNs), chat UI, basic voice. Connect to gateway.
- **Phase 3 — Android app**: Chat UI, Talk Mode (continuous voice), push (FCM). Connect to gateway.
- **Shared**: All apps use the existing gateway WebSocket API for real-time messaging

**Reality check:** This is a major investment (separate codebases, platform expertise, CI/CD, signing, distribution). Consider whether a progressive web app (PWA) could cover 80% of the value at 20% of the cost.

**Alternative — PWA approach:**
- Build a responsive web client as a new JaiClaw extension or standalone SPA
- Use Web Push for notifications, Web Audio for voice, Service Worker for offline
- Deploy as installable PWA on all platforms
- Much lower maintenance burden than three native codebases

**Estimated scope:** PWA: ~30-40 files. Native apps: 3 separate projects, thousands of files each.

---

### 4.3 Device Control (Nodes)

**Gap:** OpenClaw can control paired iOS, Android, macOS, and Linux devices remotely — invoke commands, capture camera/screen, read location, access contacts. JaiClaw has no device control.

**Module placement:** New `extensions/jaiclaw-nodes`

**Implementation approach:**
- **NodeRegistry**: Register paired devices with connection details
- **NodeAdapter SPI**: Per-platform command execution (iOS/Shortcuts, Android/ADB, macOS/osascript, Linux/SSH)
- **NodeTools**: Agent-callable tools — `node_exec`, `node_screenshot`, `node_camera`, `node_location`
- **Pairing flow**: Device sends pairing request to gateway, user approves
- **Transport**: WebSocket or HTTP for command dispatch and result return

**Dependencies:** Requires companion apps or agents running on target devices. Closely tied to 4.2.

**Estimated scope:** Server-side registry + tools: ~15-20 files. Device-side agents: separate projects.

---

## Tier 5: UX, Config & Tooling Gaps

### 5.1 Config Hot-Reload

**Gap:** OpenClaw supports config hot-reload with file watchers — no restart needed for most configuration changes. JaiClaw requires a restart.

**Module placement:** Extend `core/jaiclaw-config`

**Implementation approach:**
- **FileWatcherService**: Java `WatchService` on config directory
- **ConfigChangeEvent**: Published via Spring `ApplicationEventPublisher`
- **Hot-reloadable scopes**: Channel credentials, skill definitions, tool availability, agent routing rules, DM policies
- **Non-reloadable**: Port, security mode, tenant mode (require restart)
- **ConfigSnapshot**: Immutable config versions, components read latest snapshot

**Dependencies:** `jaiclaw-config` (exists)

**Estimated scope:** ~10-15 files

---

### 5.2 Extended CLI Commands (40+ vs ~12)

**Gap:** OpenClaw has 40+ CLI subcommands including doctor diagnostics, backup/restore, channel status probing, model management, dashboard launcher. JaiClaw's Spring Shell has ~12 commands.

**Priority additions:**
1. **`doctor`** — Validate config, probe channels, check auth, report recommendations
2. **`channels status --probe`** — Send test message to each channel, report connectivity
3. **`backup` / `restore`** — Export/import config, credentials, sessions
4. **`dashboard`** — Launch web control UI (open browser to gateway dashboard)
5. **`models list`** — Show available models across configured providers
6. **`sessions inspect`** — Detailed session view with message history
7. **`context list/detail`** — Show token usage and compaction state

**Module placement:** Extend `apps/jaiclaw-shell`

**Estimated scope:** ~5-8 files per command group

---

### 5.3 Extended Thinking (Chain-of-Thought)

**Gap:** OpenClaw has explicit extended thinking support with per-session thinking configuration, streaming thought output, and thinking budgets. JaiClaw doesn't expose this.

**Module placement:** Extend `core/jaiclaw-agent` + `core/jaiclaw-config`

**Implementation approach:**
- **ThinkingConfig**: `mode` (off, low, medium, high), `budget` (max tokens for thinking)
- **Per-session override**: Agent or user can adjust thinking level per conversation
- **Streaming thought output**: Surface thinking tokens in WebSocket stream (separate from response)
- **Provider support**: Anthropic Claude supports this natively; map to provider-specific parameters

**Dependencies:** Spring AI Anthropic adapter (must support `thinking` parameter)

**Estimated scope:** ~8-10 files

---

### 5.4 Richer Onboarding Wizard

**Gap:** OpenClaw has a multi-step interactive onboarding wizard with search-and-select UI patterns, channel discovery flows, provider selection with model pickers, and progressive disclosure. JaiClaw's `onboard` command is simpler.

**Module placement:** Extend `apps/jaiclaw-shell` onboarding steps

**Implementation approach:**
- **ChannelDiscoveryStep**: Auto-detect available channel credentials in env, suggest configuration
- **ProviderSelectionStep**: Show available providers, test API key validity, recommend models
- **ModelPickerStep**: List models per provider with capability tags (vision, tools, streaming)
- **SkillBrowserStep**: Browse and enable/disable skills interactively
- **HealthCheckStep**: Run `doctor` at end of onboarding, surface issues

**Dependencies:** Spring Shell `ComponentFlow`, existing onboarding framework

**Estimated scope:** ~10-15 files

---

## Tier 6: Plugin & Skill Ecosystem Gaps

### 6.1 Skill Library Expansion (58 vs 51) — LARGELY COMPLETE

**Gap:** ~~OpenClaw has 51 bundled skills. JaiClaw has 29.~~ **Updated 2026-03-30:** JaiClaw now has **58 skills** after porting 26 trivial/borderline skills and implementing canvas, Discord, and Slack tool providers with matching skills. The remaining gap is only 2 OpenClaw-specific skills (bluebubbles, voice-call) that require backing channel/plugin infrastructure.

**Portability analysis:** Both systems use the same fundamental pattern — SKILL.md files (Markdown + YAML frontmatter) injected into the agent's system prompt at runtime. Skills are *not* code. This makes porting straightforward for most skills.

**Three portability categories:**

| Category | Description | Count | Effort |
|----------|-------------|-------|--------|
| **Pure prompt** | Markdown guidance referencing CLI tools (gh, op, etc.) that the user installs independently. No backing code. | ~40-45 | **Minutes per skill** — copy body, reformat frontmatter |
| **Built-in tool reference** | References tools both systems already have (web search, file ops, browser, canvas). | ~5-6 | **Minutes per skill** — copy as-is |
| **Plugin-backed** | Documents tools that only exist in OpenClaw plugins (tavily_search, firecrawl_scrape, etc.). The skill text is portable but useless without the Java tool implementation. | ~5-6 | **Days per skill** — need to implement the ToolCallback in Java |

**Frontmatter translation** (the only mechanical change needed):

```yaml
# OpenClaw format                    # JaiClaw format
---                                   ---
name: github                          name: github
description: "GitHub ops"             description: GitHub ops
metadata:                             requiredBins: [gh]
  openclaw:                           platforms: [darwin, linux]
    requires:                         version: 1.0.0
      bins: ["gh"]                    tenantIds: []
---                                   ---
```

**Ported skills** (26 trivial + 3 with backing tools, added 2026-03-30):
- Trivial: apple-notes, apple-reminders, bear-notes, blucli, camsnap, clawhub, coding-agent, eightctl, gemini, himalaya, imsg, model-usage, node-connect, obsidian, openai-whisper, openhue, oracle, ordercli, peekaboo, sag, session-logs, sherpa-onnx-tts, songsee, sonoscli, spotify-player, things-mac
- With new MCP tool providers: **canvas** (uses existing `jaiclaw-canvas` tools), **discord** (new `jaiclaw-discord-tools` extension with `DiscordMcpToolProvider` — 9 tools), **slack** (new `jaiclaw-slack-tools` extension with `SlackMcpToolProvider` — 10 tools)

**Remaining** (2 OpenClaw-specific, need backing infrastructure):
bluebubbles, voice-call — park until the respective channels/plugins are implemented in JaiClaw.

---

### 6.2 Plugin Ecosystem Maturity

**Gap:** OpenClaw has 80+ bundled plugins with a mature plugin SDK, published npm packages, third-party plugin ecosystem, and a plugin marketplace (ClawHub). JaiClaw has a plugin SPI with ServiceLoader + Spring scanning but few concrete plugins and no marketplace.

**Implementation approach:**
- **Plugin SDK documentation**: Comprehensive guide for third-party plugin authors
- **Plugin archetype**: Maven archetype for bootstrapping new plugins
- **Plugin repository**: Central Maven repository or GitHub-based discovery for community plugins
- **Plugin versioning**: Semantic versioning with compatibility matrix
- **Plugin isolation**: ClassLoader isolation for untrusted plugins (future)

**Estimated scope:** Documentation + archetype: ~20 files. Registry/marketplace: separate project.

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-4)
- **1.2** Group Chat Routing — unlocks multi-user scenarios
- **2.1** DM Security & Pairing — prerequisite for public-facing deployments
- **5.1** Config Hot-Reload — developer experience improvement
- **5.3** Extended Thinking — quick win, high value for reasoning quality

### Phase 2: Channels & Providers (Weeks 5-12)
- **1.1** WhatsApp channel (highest demand)
- **1.1** Matrix channel (open protocol, enterprise)
- **3.1** OpenRouter provider (meta-gateway, covers dozens of models)
- **3.1** DeepSeek, Groq, Mistral providers
- **3.4** Search provider diversity (Brave, Tavily)

### Phase 3: Agent Capabilities (Weeks 13-20)
- **1.3** Multi-Agent Routing
- **2.2** Execution Approval Workflows
- **2.3** Session Spawning & Sub-Agents
- **3.2** Image Generation
- **6.1** First 10 skill ports

### Phase 4: Media & Voice (Weeks 21-28)
- **3.3** Full Media Pipeline
- **4.1** Server-side voice session infrastructure
- **5.2** Extended CLI commands (doctor, backup, channels status)
- **5.4** Richer onboarding wizard

### Phase 5: Native Apps & Devices (Weeks 29+)
- **4.2** PWA or native companion app (evaluate PWA-first)
- **4.3** Device Control (Nodes)
- **1.1** Remaining channels (LINE, Google Chat, IRC, Twitch, etc.)
- **6.2** Plugin ecosystem maturity

---

## What JaiClaw Already Has (Not In This Plan)

These features are already ported or are JaiClaw-original. No action needed:

| Feature | Status |
|---------|--------|
| Memory (workspace + daily logs + hybrid search + transcripts) | Ported |
| Context compaction (LLM summarization) | Ported |
| Browser automation (8 Playwright tools) | Ported |
| Canvas/A2UI (3 tools) | Ported |
| Cron scheduling | Ported |
| Identity linking (cross-channel) | Ported |
| Voice TTS/STT (OpenAI provider) | Ported (API layer) |
| OAuth credential management | Ported (per OAUTH-IMPLEMENTATION-PLAN.md) |
| Multi-tenancy | Ported (per MULTI-TENANCY-REMEDIATION-PLAN.md) |
| Code tools (file_edit, glob, grep) | JaiClaw-original |
| DocStore (8 tools + LLM analysis) | JaiClaw-original |
| K8s monitoring tools | JaiClaw-original |
| Agent-to-agent security (ECDH handshake) | JaiClaw-original |
| Explicit tool loop with HITL gates | JaiClaw-original |
| MCP server hosting (REST + SSE + stdio) | JaiClaw-original |
| Subscription/payment system | JaiClaw-original |

---

## Task Checklist

> Mark tasks `[x]` as they are completed. Update dates in the "Done" column.

### Phase 1: Foundation (Weeks 1-4)

| # | Task | Feature | Status | Done |
|---|------|---------|--------|------|
| 1 | Add `isGroup()` / `groupId()` to `ChannelMessage` in `jaiclaw-channel-api` | 1.2 | [ ] | |
| 2 | Create `ActivationMode` enum (ALWAYS, MENTION_ONLY, REPLY_BACK) | 1.2 | [ ] | |
| 3 | Create `GroupRoutingPolicy` config (per-channel activation mode) | 1.2 | [ ] | |
| 4 | Implement `GroupSessionManager` (session-per-group isolation) | 1.2 | [ ] | |
| 5 | Wire group routing into `GatewayService` message dispatch | 1.2 | [ ] | |
| 6 | Implement thread binding (Discord threads, Slack threads, Telegram topics) | 1.2 | [ ] | |
| 7 | Write Spock specs for group routing | 1.2 | [ ] | |
| 8 | Create `DmPolicy` enum (OPEN, PAIRING, BLOCKED) | 2.1 | [ ] | |
| 9 | Create `DmPolicyConfig` (per-channel YAML configuration) | 2.1 | [ ] | |
| 10 | Implement `PairingCodeManager` (generate/validate time-limited codes) | 2.1 | [ ] | |
| 11 | Implement `AllowlistStore` (persistent approved sender list) | 2.1 | [ ] | |
| 12 | Implement `DmSecurityFilter` in `GatewayService` | 2.1 | [ ] | |
| 13 | Implement mention gating and command gating | 2.1 | [ ] | |
| 14 | Write Spock specs for DM security | 2.1 | [ ] | |
| 15 | Implement `FileWatcherService` (Java WatchService on config dir) | 5.1 | [ ] | |
| 16 | Create `ConfigChangeEvent` (Spring ApplicationEvent) | 5.1 | [ ] | |
| 17 | Define hot-reloadable vs restart-required config scopes | 5.1 | [ ] | |
| 18 | Wire config reload into channel credentials, skills, tools, DM policies | 5.1 | [ ] | |
| 19 | Write Spock specs for config hot-reload | 5.1 | [ ] | |
| 20 | Create `ThinkingConfig` (mode: off/low/medium/high, budget) | 5.3 | [ ] | |
| 21 | Add per-session thinking override to `Session` model | 5.3 | [ ] | |
| 22 | Surface thinking tokens in WebSocket stream (separate from response) | 5.3 | [ ] | |
| 23 | Map thinking config to Anthropic API `thinking` parameter | 5.3 | [ ] | |
| 24 | Write Spock specs for extended thinking | 5.3 | [ ] | |

### Phase 2: Channels & Providers (Weeks 5-12)

| # | Task | Feature | Status | Done |
|---|------|---------|--------|------|
| 25 | Evaluate WhatsApp approach: GraalJS + whatsapp-web.js vs HTTP sidecar vs Baileys | 1.1 | [ ] | |
| 26 | Create `jaiclaw-channel-whatsapp` module (pom.xml, adapter, config) | 1.1 | [ ] | |
| 27 | Implement WhatsApp `ChannelAdapter` (send/receive/attachments) | 1.1 | [ ] | |
| 28 | Write WhatsApp auto-configuration + tests | 1.1 | [ ] | |
| 29 | Create `jaiclaw-channel-matrix` module | 1.1 | [ ] | |
| 30 | Implement Matrix `ChannelAdapter` (matrix-java-sdk or REST) | 1.1 | [ ] | |
| 31 | Write Matrix auto-configuration + tests | 1.1 | [ ] | |
| 32 | Create `jaiclaw-starter-openrouter` (OpenAI-compatible with model mappings) | 3.1 | [ ] | |
| 33 | Create `jaiclaw-starter-deepseek` | 3.1 | [ ] | |
| 34 | Create `jaiclaw-starter-groq` | 3.1 | [ ] | |
| 35 | Create `jaiclaw-starter-mistral` | 3.1 | [ ] | |
| 36 | Write provider integration tests | 3.1 | [ ] | |
| 37 | Create `SearchProvider` SPI interface | 3.4 | [ ] | |
| 38 | Implement `BraveSearchProvider` | 3.4 | [ ] | |
| 39 | Implement `TavilySearchProvider` | 3.4 | [ ] | |
| 40 | Implement `DuckDuckGoSearchProvider` | 3.4 | [ ] | |
| 41 | Create `SearchRouter` (configurable primary + fallback chain) | 3.4 | [ ] | |
| 42 | Update `WebSearchTool` to delegate to `SearchProvider` | 3.4 | [ ] | |
| 43 | Write search provider Spock specs | 3.4 | [ ] | |

### Phase 3: Agent Capabilities (Weeks 13-20)

| # | Task | Feature | Status | Done |
|---|------|---------|--------|------|
| 44 | Create `AgentRegistry` (register multiple named agents) | 1.3 | [ ] | |
| 45 | Create `AgentRoutingRule` (map channel/account/peer to agent) | 1.3 | [ ] | |
| 46 | Implement per-agent `SessionManager` and `WorkspaceMemory` isolation | 1.3 | [ ] | |
| 47 | Add agent CRUD to gateway API (add/bind/delete/list) | 1.3 | [ ] | |
| 48 | Add agent shell commands | 1.3 | [ ] | |
| 49 | Write multi-agent Spock specs | 1.3 | [ ] | |
| 50 | Create `ApprovalRequest` record (tool, args, risk level) | 2.2 | [ ] | |
| 51 | Create `ApprovalRenderer` SPI (per-channel rendering) | 2.2 | [ ] | |
| 52 | Implement `ApprovalDeliveryService` (send request, await response) | 2.2 | [ ] | |
| 53 | Implement Telegram inline keyboard approval renderer | 2.2 | [ ] | |
| 54 | Implement Slack button approval renderer | 2.2 | [ ] | |
| 55 | Implement Discord component approval renderer | 2.2 | [ ] | |
| 56 | Create `ApprovalPolicy` config (which tools, auto-approve, timeout) | 2.2 | [ ] | |
| 57 | Enhance `ExplicitToolLoop` to pause/resume on approval | 2.2 | [ ] | |
| 58 | Write approval workflow Spock specs | 2.2 | [ ] | |
| 59 | Create `SessionType` enum (MAIN, GROUP, CRON, HOOK, SPAWNED) | 2.3 | [ ] | |
| 60 | Implement `SessionSpawnTool` (create child session) | 2.3 | [ ] | |
| 61 | Implement `SessionYieldTool` (pause and await input) | 2.3 | [ ] | |
| 62 | Add parent-child session tracking and lifecycle | 2.3 | [ ] | |
| 63 | Write session spawning Spock specs | 2.3 | [ ] | |
| 64 | Create `jaiclaw-image-gen` module | 3.2 | [ ] | |
| 65 | Implement `ImageGenerationTool` wrapping Spring AI `ImageModel` | 3.2 | [ ] | |
| 66 | Add DALL-E provider adapter | 3.2 | [ ] | |
| 67 | Add FAL/Stable Diffusion provider adapter | 3.2 | [ ] | |
| 68 | Wire generated images as channel attachments | 3.2 | [ ] | |
| 69 | Write image generation Spock specs | 3.2 | [ ] | |
| 70 | Audit OpenClaw skills vs JaiClaw's 29 — identify remaining unported skills | 6.1 | [x] | 2026-03-30 |
| 71 | Copy + reformat frontmatter for all 26 remaining pure-prompt skills | 6.1 | [x] | 2026-03-30 |
| 72 | Port plugin-backed skill tools to Java (tavily, firecrawl, etc.) | 6.1 | [ ] | |

### Phase 4: Media & Voice (Weeks 21-28)

| # | Task | Feature | Status | Done |
|---|------|---------|--------|------|
| 73 | Implement `MediaTranscoder` (FFmpeg wrapper via ProcessBuilder) | 3.3 | [ ] | |
| 74 | Implement `MediaSizeManager` (per-channel size limits, compression) | 3.3 | [ ] | |
| 75 | Implement `TempFileManager` (auto-cleanup after TTL) | 3.3 | [ ] | |
| 76 | Implement `ImageProcessor` (resize, EXIF strip, format convert) | 3.3 | [ ] | |
| 77 | Create `MediaPipeline` orchestrator (inbound → analyze → transcode → deliver) | 3.3 | [ ] | |
| 78 | Write media pipeline Spock specs | 3.3 | [ ] | |
| 79 | Extend `jaiclaw-voice` for continuous streaming STT → agent → TTS loop | 4.1 | [ ] | |
| 80 | Add WebSocket voice transport for real-time audio streaming | 4.1 | [ ] | |
| 81 | Evaluate server-side wake-word detection (Porcupine/Picovoice) | 4.1 | [ ] | |
| 82 | Write voice session Spock specs | 4.1 | [ ] | |
| 83 | Implement `doctor` CLI command (config validation, channel probing, auth check) | 5.2 | [ ] | |
| 84 | Implement `channels status --probe` CLI command | 5.2 | [ ] | |
| 85 | Implement `backup` / `restore` CLI commands | 5.2 | [ ] | |
| 86 | Implement `models list` CLI command | 5.2 | [ ] | |
| 87 | Implement `sessions inspect` CLI command | 5.2 | [ ] | |
| 88 | Implement `context list/detail` CLI command | 5.2 | [ ] | |
| 89 | Add `ChannelDiscoveryStep` to onboarding wizard | 5.4 | [ ] | |
| 90 | Add `ProviderSelectionStep` with API key validation | 5.4 | [ ] | |
| 91 | Add `ModelPickerStep` with capability tags | 5.4 | [ ] | |
| 92 | Add `HealthCheckStep` (run doctor at end of onboard) | 5.4 | [ ] | |

### Phase 5: Native Apps & Devices (Weeks 29+)

| # | Task | Feature | Status | Done |
|---|------|---------|--------|------|
| 93 | Evaluate PWA vs native — write decision doc | 4.2 | [ ] | |
| 94 | Build PWA or macOS menu bar app (Phase 1 companion) | 4.2 | [ ] | |
| 95 | Build iOS companion app or PWA install (Phase 2 companion) | 4.2 | [ ] | |
| 96 | Build Android companion app or PWA install (Phase 3 companion) | 4.2 | [ ] | |
| 97 | Create `jaiclaw-nodes` module with `NodeRegistry` | 4.3 | [ ] | |
| 98 | Create `NodeAdapter` SPI (per-platform command execution) | 4.3 | [ ] | |
| 99 | Implement `NodeTools` (node_exec, node_screenshot, node_camera, node_location) | 4.3 | [ ] | |
| 100 | Implement device pairing flow | 4.3 | [ ] | |
| 101 | Create `jaiclaw-channel-imessage` (macOS-only, AppleScript bridge) | 1.1 | [ ] | |
| 102 | Create `jaiclaw-channel-line` | 1.1 | [ ] | |
| 103 | Create `jaiclaw-channel-googlechat` | 1.1 | [ ] | |
| 104 | Create `jaiclaw-channel-irc` | 1.1 | [ ] | |
| 105 | Create `jaiclaw-channel-twitch` | 1.1 | [ ] | |
| 106 | Create `jaiclaw-channel-mattermost` | 1.1 | [ ] | |
| 107 | Write plugin SDK documentation for third-party authors | 6.2 | [ ] | |
| 108 | Create Maven archetype for bootstrapping new plugins | 6.2 | [ ] | |
| 109 | Set up plugin discovery registry (GitHub-based or Maven repo) | 6.2 | [ ] | |

---

## Summary

| Phase | Tasks | Features Covered |
|-------|-------|-----------------|
| Phase 1: Foundation | #1-24 (24 tasks) | Group routing, DM security, config hot-reload, extended thinking |
| Phase 2: Channels & Providers | #25-43 (19 tasks) | WhatsApp, Matrix, OpenRouter/DeepSeek/Groq/Mistral, search providers |
| Phase 3: Agent Capabilities | #44-72 (29 tasks) | Multi-agent, approval workflows, session spawning, image gen, skills |
| Phase 4: Media & Voice | #73-92 (20 tasks) | Media pipeline, voice sessions, CLI commands, onboarding |
| Phase 5: Native & Devices | #93-109 (17 tasks) | Companion apps, device nodes, remaining channels, plugin ecosystem |
| **Total** | **109 tasks** | **19 feature areas** |
