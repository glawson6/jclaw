# OpenClaw vs JaiClaw: Startup & Quickstart Script Comparison Report

**Date:** 2026-03-30

---

## Executive Summary

JaiClaw's startup scripts are solid for basic use cases (API key-based auth, Docker/local modes, Telegram setup), but OpenClaw has significantly richer **auth lifecycle management** including OAuth integration in scripts, proactive auth monitoring, mobile re-auth flows, and credential status tracking. This report identifies the key gaps and proposes improvements.

---

## 1. Feature Comparison Matrix

| Feature | OpenClaw | JaiClaw | Gap |
|---------|----------|---------|-----|
| **Basic startup (local/Docker)** | Yes | Yes | Parity |
| **Interactive provider selection** | Yes (gum TUI) | Yes (read prompts) | Minor — gum is nicer |
| **API key input during setup** | Yes | Yes | Parity |
| **OAuth login in startup scripts** | Yes — `claude setup-token` + profiles | **No** — OAuth only in shell REPL | **Major gap** |
| **Auth status check (CLI)** | `claude-auth-status.sh` — full/json/simple | `auth status` shell cmd only | **Major gap** |
| **Auth monitoring (cron)** | `auth-monitor.sh` + systemd timer | None | **Major gap** |
| **Proactive expiry notifications** | ntfy.sh + SMS + OpenClaw message | None | **Gap** |
| **Mobile re-auth flow** | `mobile-reauth.sh` + Termux widgets | None | **Gap** |
| **Docker auth dir mounting** | `live-docker-auth.sh` — mounts `~/.claude`, `~/.codex`, etc. | None — API keys passed as env vars | **Gap** |
| **Credential file management** | `~/.openclaw/agents/main/agent/auth-profiles.json` | `~/.jaiclaw/agents/default/profiles.json` | Parity (structure) |
| **Multi-provider OAuth** | Claude, Codex, Qwen, MiniMax | Chutes, Codex, Gemini, Qwen, MiniMax | Parity |
| **Token refresh** | Yes (automatic) | Yes (automatic with file lock) | Parity |
| **Secret ref resolution** | ENV, FILE, EXEC | ENV, FILE, EXEC | Parity |
| **E2E onboarding tests** | `onboard-docker.sh` — 578 lines | None | **Gap** |
| **Podman support** | Full (`setup-podman.sh` + Quadlet) | None | **Gap** (lower priority) |
| **Non-interactive/CI mode** | `NO_PROMPT=1` | None | **Gap** |
| **Ollama fallback** | N/A (different arch) | Yes — auto-pulls llama3.2 | JaiClaw ahead |
| **Multi-tenant startup** | N/A | `start-multitenant.sh` | JaiClaw ahead |
| **JBang bootstrap** | N/A | `bootstrap.sh` + JBang | JaiClaw ahead |

---

## 2. The Big Gap: OAuth in Startup Scripts

### What OpenClaw Does

OpenClaw's scripts integrate OAuth throughout the **startup lifecycle**, not just at runtime:

1. **`docker/setup.sh`** reads `~/.openclaw/openclaw.json` for gateway tokens and mounts auth credential directories (`~/.claude`, `~/.codex`, `~/.minimax`) into Docker containers via `live-docker-auth.sh`

2. **`setup-auth-system.sh`** is a dedicated interactive wizard that:
   - Checks current auth status
   - Guides users through `claude setup-token` for long-lived tokens
   - Sets up **ntfy.sh push notifications** for token expiry warnings
   - Collects phone number for SMS alerts
   - Installs a **systemd timer** (`openclaw-auth-monitor.timer`) that runs every 30 min

3. **`auth-monitor.sh`** is a cron-compatible script that:
   - Reads `~/.openclaw/agents/main/agent/auth-profiles.json`
   - Checks credential expiry timestamps
   - Sends proactive alerts before tokens expire
   - Has cooldown tracking (min 1 hour between notifications)
   - Supports ntfy.sh, SMS, and in-app message channels

4. **`claude-auth-status.sh`** provides three output modes:
   - `full` — colored terminal display with expiry countdown
   - `json` — machine-readable for tooling integration
   - `simple` — one-line status code (OK:12h, EXPIRING:30m, EXPIRED, MISSING)

5. **`mobile-reauth.sh`** + Termux widgets enable re-auth from a phone when tokens expire on a remote server

### What JaiClaw Has

JaiClaw's OAuth is fully implemented in the **Java identity module** but is only accessible:
- Via the Spring Shell `login`/`logout`/`auth status` commands (interactive REPL only)
- At runtime through `OAuthFlowManager` auto-configuration

**The startup scripts (`start.sh`, `quickstart.sh`) have zero OAuth awareness.** They only handle static API keys via environment variables.

---

## 3. Recommended Improvements

### Priority 1: Auth Status Script (High Impact, Low Effort)

Create `scripts/auth-status.sh` — a standalone script (no JVM required) that:

```
# Usage:
#   ./scripts/auth-status.sh              # full colored output
#   ./scripts/auth-status.sh --json       # machine-readable JSON
#   ./scripts/auth-status.sh --simple     # one-line: OK:12h / EXPIRING:30m / EXPIRED / MISSING
```

**Implementation:**
- Read `~/.jaiclaw/agents/default/profiles.json` with `jq` (or Python fallback)
- Check `expires` field against current epoch
- Report per-profile: PROFILE_ID | TYPE | STATE | EXPIRES_IN
- Exit code: 0=OK, 1=EXPIRING_SOON, 2=EXPIRED, 3=MISSING

**Why:** This enables all downstream integrations (cron monitoring, startup checks, CI validation).

### Priority 2: OAuth Integration in Startup Scripts (High Impact, Medium Effort)

Modify `quickstart.sh` and `start.sh` to:

1. **Check auth status on startup** — before launching, verify credentials are valid:
   ```bash
   # In start.sh, after loading .env:
   if command -v jq &>/dev/null && [ -f "$HOME/.jaiclaw/agents/default/profiles.json" ]; then
       auth_state=$(./scripts/auth-status.sh --simple 2>/dev/null || echo "UNKNOWN")
       case "$auth_state" in
           EXPIRED*)  warn "OAuth credentials expired. Run: ./start.sh login" ;;
           EXPIRING*) warn "OAuth credentials expiring soon ($auth_state)" ;;
       esac
   fi
   ```

2. **Add `login` command to start.sh** — delegate to the shell's OAuth flow or a lightweight JBang script:
   ```bash
   # ./start.sh login [provider]
   cmd_login() {
       local provider="${1:-}"
       # Launch JBang script or lightweight Java class that runs OAuthFlowManager
       jbang JaiClawAuth.java login "$provider"
   }
   ```

3. **Add provider selection with OAuth option in quickstart.sh**:
   ```
   Select your LLM provider:
     1) Anthropic (API key)
     2) OpenAI (API key)
     3) Google Gemini (API key)
     4) Ollama (local, no API key)
     5) Chutes (OAuth login)          ← NEW
     6) OpenAI Codex (OAuth login)    ← NEW
   ```

### Priority 3: Auth Monitoring Script (Medium Impact, Low Effort)

Create `scripts/auth-monitor.sh` — cron-compatible monitoring:

```bash
#!/usr/bin/env bash
# Run via: crontab -e → */30 * * * * /path/to/jaiclaw/scripts/auth-monitor.sh

WARN_HOURS="${JAICLAW_AUTH_WARN_HOURS:-2}"
STATE_FILE="$HOME/.jaiclaw/auth-monitor-state"
COOLDOWN_SECONDS=3600  # 1 hour between notifications

status=$(./scripts/auth-status.sh --simple)
case "$status" in
    EXPIRING*|EXPIRED*)
        last_notify=$(cat "$STATE_FILE" 2>/dev/null || echo 0)
        now=$(date +%s)
        if (( now - last_notify > COOLDOWN_SECONDS )); then
            # Send notification (ntfy.sh, email, etc.)
            echo "$now" > "$STATE_FILE"
        fi
        ;;
esac
```

### Priority 4: Docker Auth Directory Mounting (Medium Impact, Medium Effort)

Create `scripts/lib/docker-auth-dirs.sh` — mirrors OpenClaw's `live-docker-auth.sh`:

- Map provider names to credential directories:
  - `anthropic` → `~/.claude`
  - `openai-codex` → `~/.codex`
  - `chutes` → `~/.chutes` (or wherever stored)
  - `qwen` → `~/.qwen`
  - `minimax` → `~/.minimax`
- Mount as read-only volumes in `docker-compose.yml`
- Configurable via `JAICLAW_DOCKER_AUTH_DIRS` (csv, "all", or "none")

Update `docker-compose.yml` to optionally mount these directories so OAuth credentials persist across container restarts.

### Priority 5: JBang OAuth Launcher (Medium Impact, Medium Effort)

Create `JaiClawAuth.java` — a JBang script for OAuth outside the REPL:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS io.jaiclaw:jaiclaw-identity:0.1.0-SNAPSHOT

// Commands:
//   jbang JaiClawAuth.java login [provider]     — interactive OAuth
//   jbang JaiClawAuth.java status               — print profile statuses
//   jbang JaiClawAuth.java refresh [profileId]  — force token refresh
```

This gives OAuth capabilities without requiring a running Spring Boot app.

### Priority 6: Non-Interactive / CI Mode (Low Impact, Low Effort)

Add `--non-interactive` flag to `quickstart.sh`:
- Skip all prompts
- Use env vars or defaults for everything
- Exit with error if required config is missing
- Useful for CI/CD and automation

---

## 4. Implementation Roadmap

| Phase | Scripts | Effort | Depends On |
|-------|---------|--------|------------|
| **Phase 1** | `scripts/auth-status.sh` | ~2 hours | Nothing |
| **Phase 2** | Startup auth check in `start.sh` | ~1 hour | Phase 1 |
| **Phase 3** | `login` command in `start.sh` | ~3 hours | Identity module (already exists) |
| **Phase 4** | `scripts/auth-monitor.sh` + cron setup | ~2 hours | Phase 1 |
| **Phase 5** | OAuth provider option in `quickstart.sh` | ~2 hours | Phase 3 |
| **Phase 6** | Docker auth dir mounting | ~2 hours | Nothing |
| **Phase 7** | `JaiClawAuth.java` JBang script | ~4 hours | Identity module |
| **Phase 8** | `--non-interactive` mode | ~1 hour | Nothing |

---

## 5. Files to Create / Modify

### New Files
| File | Purpose |
|------|---------|
| `scripts/auth-status.sh` | Standalone auth status checker (no JVM) |
| `scripts/auth-monitor.sh` | Cron-compatible auth monitoring |
| `scripts/lib/docker-auth-dirs.sh` | Docker credential directory mapping |
| `JaiClawAuth.java` | JBang OAuth launcher |

### Modified Files
| File | Changes |
|------|---------|
| `start.sh` | Add auth status check on startup, add `login` command |
| `quickstart.sh` | Add OAuth providers to interactive wizard, add `--non-interactive` flag |
| `docker-compose/docker-compose.yml` | Optional auth dir volume mounts |
| `scripts/common.sh` | Add `check_auth_status()` helper function |

---

## 6. Key Architectural Differences

| Aspect | OpenClaw | JaiClaw |
|--------|----------|---------|
| **Runtime** | Node.js — can run JS in scripts | JVM — heavier startup, but JBang helps |
| **Auth store format** | Same JSON structure | Same JSON structure (compatible) |
| **Profile location** | `~/.openclaw/agents/main/agent/` | `~/.jaiclaw/agents/default/` |
| **Token management** | `claude setup-token` CLI | `OAuthFlowManager` Java class |
| **Script tooling** | `gum` for TUI, `jq` for JSON | `read` prompts, `jq` optional |
| **Container runtime** | Docker + Podman | Docker only |
| **Mobile support** | Termux widgets + SSH | None |

---

## 7. Quick Wins (Can Implement Now)

1. **`auth-status.sh`** — Pure bash + jq, reads existing profiles.json, provides foundation for everything else
2. **Startup auth warning** — 10-line addition to `start.sh` that warns if OAuth tokens are expired
3. **`--non-interactive`** flag — Simple guard around existing `read` prompts in `quickstart.sh`

These three changes would bring the biggest functional improvement with minimal code.
