# Security Handshake Server Example

Standalone MCP server implementing the full security handshake protocol — ECDH key exchange, API key bootstrap trust, nonce challenge/response, and JWT session token issuance. Includes an embedded programmatic client that runs the complete 7-step handshake on startup.

## What This Demonstrates

- **Security handshake protocol** (5 REST endpoints) from `jaiclaw-tools-security`
- **ECDH P-256 key exchange** with forward secrecy
- **API_KEY bootstrap trust** — client proves identity with a pre-shared API key
- **HMAC-SHA256 challenge/response** — mutual authentication via signed nonces
- **JWT session tokens** — issued after successful handshake, validated on protected tools
- **Bearer token protection** on MCP tool endpoints
- **Programmatic client** (`HandshakeClientRunner`) demonstrating all 7 steps

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│              SECURITY HANDSHAKE SERVER APP                  │
│                (standalone Spring Boot)                    │
├──────────────────┬───────────────────────────────────────┤
│ REST Endpoints   │  HandshakeController                     │
│                  │  POST /mcp/security/capabilities          │
│                  │  POST /mcp/security/negotiate             │
│                  │  POST /mcp/security/challenge             │
│                  │  POST /mcp/security/verify                │
│                  │  POST /mcp/security/establish             │
├──────────────────┼───────────────────────────────────────┤
│ Protected Tools  │  McpToolController                       │
│                  │  POST /mcp/data/tools/{name}              │
│                  │  → validates Bearer token first            │
├──────────────────┼───────────────────────────────────────┤
│ Security         │  CryptoService (ECDH, HMAC)               │
│                  │  HandshakeSessionStore (in-memory)         │
│                  │  SecurityHandshakeMcpProvider              │
├──────────────────┼───────────────────────────────────────┤
│ Client (startup) │  HandshakeClientRunner                    │
│                  │  → runs 7-step handshake automatically     │
└──────────────────┴───────────────────────────────────────┘

Handshake flow:
  Client                              Server
    │                                    │
    ├──POST /capabilities───────────────►│  1. Query supported ciphers
    │◄──cipherSuites, authMethods────────┤
    │                                    │
    ├──POST /negotiate──────────────────►│  2-3. ECDH key exchange + API key
    │  (clientPublicKey, apiKey)          │
    │◄──(serverPublicKey, handshakeId)───┤
    │                                    │
    │  [Both derive shared secret]       │  4. ECDH key agreement
    │                                    │
    ├──POST /challenge──────────────────►│  5. Request nonce
    │◄──(challenge nonce)────────────────┤
    │                                    │
    ├──POST /verify─────────────────────►│  6. Sign nonce with HMAC
    │  (HMAC-SHA256 signature)           │
    │◄──(verified: true)─────────────────┤
    │                                    │
    ├──POST /establish──────────────────►│  7. Get session token
    │◄──(JWT sessionToken)───────────────┤
    │                                    │
    ├──POST /mcp/data/tools/...─────────►│  8. Call protected tool
    │  Authorization: Bearer <token>     │
    │◄──(secret data)────────────────────┤
```

## Prerequisites

- Java 21+
- JaiClaw built and installed (`./mvnw install -DskipTests` from project root)

## Build & Run

```bash
cd jaiclaw-examples/security-handshake-server
export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.9-oracle
../../mvnw spring-boot:run
```

The embedded client runs automatically on startup and logs the complete handshake flow.

## Testing It

```bash
# Step 1: Query capabilities
curl -s -X POST http://localhost:8080/mcp/security/capabilities | jq

# Step 2: Negotiate (provide API key + client public key)
# The embedded HandshakeClientRunner does this automatically on startup.
# See the console logs for the full 7-step flow.

# After the client completes, you can also call the protected tool manually:
# (Use the session token printed in the startup logs)
curl -X POST http://localhost:8080/mcp/data/tools/get_secret_data \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <session-token-from-logs>" \
  -d '{}'
```

## Related

- [security-handshake](../security-handshake/) — LLM-driven client that uses Spring AI to autonomously perform the handshake
- `jaiclaw-tools-security` module — reusable handshake protocol implementation
