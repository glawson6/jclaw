# OAuth Provider Demo

Authenticate with an AI provider via OAuth before the agent can operate — the LLM API key comes from a browser-based PKCE or device code flow, not from an environment variable.

## What This Demonstrates

- **OAuth-gated LLM access** — the agent cannot chat until the user completes an OAuth login
- **jaiclaw-identity** — OAuth credential management, token storage, automatic token refresh
- **PKCE authorization code flow** — browser-based login for OpenAI Codex, Google Gemini, and Chutes
- **RFC 8628 device code flow** — headless login for Qwen and MiniMax
- **Custom TenantChatModelFactory** — overrides the default to create ChatModel instances with the OAuth access token
- **VPS/remote detection** — falls back to manual URL paste when no browser is available

## Architecture

```
  User                    App (port 8080)                  OAuth Provider
  ────                    ───────────────                  ──────────────

  1. POST /api/oauth/login/openai-codex
       ────────────────────►
                            Start PKCE flow
                            Open browser ──────────────────► Authorization URL
  2. User authorizes in browser
                            ◄────────────────────────────── Callback with code
                            Exchange code for token ────────► Token endpoint
                            ◄────────────────────────────── Access + refresh token
                            Store in auth-profiles.json
                            Activate as LLM API key
       ◄────────────────────
       {"status":"authenticated","email":"..."}

  3. POST /api/chat  {"content":"Hello!"}
       ────────────────────►
                            TenantChatModelFactory creates
                            OpenAiChatModel with OAuth token
                            ────────────────────────────────► OpenAI API
                            ◄────────────────────────────── Response
       ◄────────────────────
       {"response":"Hi! How can I help?"}
```

## Prerequisites

- Java 21+
- JaiClaw built (`./mvnw install -DskipTests` from repo root)
- OAuth client credentials for at least one provider (see table below)

**No `OPENAI_API_KEY` environment variable needed** — that is the whole point of this demo.

## Quick Start

### 1. Build and run

```bash
cd jaiclaw-examples/oauth-provider-demo
JAVA_HOME=$HOME/.sdkman/candidates/java/current \
  ../../mvnw spring-boot:run
```

### 2. Authenticate via OAuth

```bash
# List available providers
curl http://localhost:8080/api/oauth/providers

# Log in (opens browser for PKCE flow)
curl -X POST http://localhost:8080/api/oauth/login/openai-codex

# Check status
curl http://localhost:8080/api/oauth/status
```

### 3. Chat with the agent

```bash
# This only works AFTER successful OAuth login
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello! How were you authenticated?"}'
```

**Before login**, the agent will return an error explaining that no OAuth credential is available.

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `OPENAI_CODEX_CLIENT_ID` | No | OpenAI Codex OAuth client ID (for Codex login) |
| `GEMINI_CLI_OAUTH_CLIENT_ID` | No | Google OAuth client ID (for Gemini login) |
| `GEMINI_CLI_OAUTH_CLIENT_SECRET` | No | Google OAuth client secret |
| `CHUTES_CLIENT_ID` | No | Chutes OAuth client ID |
| `CHUTES_CLIENT_SECRET` | No | Chutes OAuth client secret |

## OAuth Providers

| Provider | ID | Flow | What Happens |
|----------|----|------|--------------|
| OpenAI Codex | `openai-codex` | PKCE (browser) | Opens browser to ChatGPT auth, catches callback on port 1455 |
| Google Gemini | `google-gemini-cli` | PKCE (browser) | Opens browser to Google login, catches callback on port 8085 |
| Chutes | `chutes` | PKCE (browser) | Opens browser to Chutes auth, catches callback on port 1456 |
| Qwen | `qwen-portal` | Device Code | Shows verification URL + code, polls for completion |
| MiniMax | `minimax-portal` | Device Code | Shows verification URL + code, polls for completion |

## How It Works

1. **No API key at startup** — `application.yml` sets a placeholder; the custom `TenantChatModelFactory` ignores it
2. **OAuth login** — `OAuthLoginController` triggers `OAuthFlowManager.login()` which runs the full PKCE or device code flow
3. **Credential stored** — the access token, refresh token, and expiry are written to `~/.jaiclaw/agents/default/agent/auth-profiles.json`
4. **Credential activated** — the resolved token is placed in an `AtomicReference<ResolvedCredential>` that the model factory reads
5. **ChatModel created** — when the agent processes a message, `TenantChatModelFactory` creates an `OpenAiChatModel` using the OAuth token as the API key
6. **Token refresh** — if the token expires, `AuthProfileResolver` auto-refreshes it using the stored refresh token

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/oauth/providers` | List available OAuth providers |
| `POST` | `/api/oauth/login/{providerId}` | Trigger OAuth login flow |
| `GET` | `/api/oauth/status` | Check current authentication status |
| `POST` | `/api/chat` | Chat with the agent (requires prior OAuth login) |

## Credential Storage

Credentials are stored at `~/.jaiclaw/agents/default/agent/auth-profiles.json`:

```json
{
  "version": 1,
  "profiles": {
    "openai-codex:user@example.com": {
      "type": "oauth",
      "provider": "openai-codex",
      "access": "eyJ...",
      "refresh": "rt_...",
      "expires": 1711929600000,
      "email": "user@example.com",
      "clientId": "..."
    }
  }
}
```

## Related

- [jaiclaw-identity module](../../extensions/jaiclaw-identity/) — full OAuth + identity linking library
- [Skill file](src/main/resources/skills/oauth-provider-demo.md) — agent behavior instructions
