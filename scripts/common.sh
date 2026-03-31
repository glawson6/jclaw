#!/usr/bin/env bash
#
# JaiClaw shared helpers — sourced by start.sh, quickstart.sh, setup.sh
#
# Provides:
#   Colors (RED, GREEN, YELLOW, CYAN, DIM, BOLD, NC)
#   Logging (info, ok, warn, err, header)
#   resolve_api_key    — mirrors ApiKeyProvider logic; sets RESOLVED_API_KEY
#   print_api_curl_example <port>  — prints a curl snippet with the resolved key
#   print_api_httpie_example <port> — prints an httpie snippet with the resolved key
#   print_security_info            — prints current security mode + key info
#

# ─── Colors ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
DIM='\033[2m'
BOLD='\033[1m'
NC='\033[0m'

# ─── Logging ──────────────────────────────────────────────────────────────────
info()   { printf "${CYAN}▸${NC} %s\n" "$*"; }
ok()     { printf "${GREEN}✓${NC} %s\n" "$*"; }
warn()   { printf "${YELLOW}!${NC} %s\n" "$*"; }
err()    { printf "${RED}✗${NC} %s\n" "$*" >&2; }
header() { printf "\n${BOLD}${CYAN}── %s ──${NC}\n\n" "$*"; }

# ─── API Key Resolution ──────────────────────────────────────────────────────
#
# Mirrors the Java ApiKeyProvider lookup order:
#   1. JAICLAW_API_KEY env var
#   2. Key file (JAICLAW_API_KEY_FILE or ~/.jaiclaw/api-key)
#   3. Generate + write to key file
#
# After this function returns, RESOLVED_API_KEY is set.
#
resolve_api_key() {
    local key_file="${JAICLAW_API_KEY_FILE:-$HOME/.jaiclaw/api-key}"

    # 1. Explicit env var
    if [ -n "${JAICLAW_API_KEY:-}" ]; then
        RESOLVED_API_KEY="$JAICLAW_API_KEY"
        return
    fi

    # 2. Read from file
    if [ -f "$key_file" ]; then
        RESOLVED_API_KEY="$(tr -d '[:space:]' < "$key_file")"
        if [ -n "$RESOLVED_API_KEY" ]; then
            return
        fi
    fi

    # 3. Generate + write
    RESOLVED_API_KEY="jaiclaw_ak_$(openssl rand -hex 16)"
    mkdir -p "$(dirname "$key_file")"
    printf '%s' "$RESOLVED_API_KEY" > "$key_file"
    chmod 600 "$key_file"
}

# ─── Curl Example ─────────────────────────────────────────────────────────────
#
# Prints a ready-to-paste curl command with the API key header.
# Usage: print_api_curl_example [port]
#
print_api_curl_example() {
    local port="${1:-8080}"
    local key="${RESOLVED_API_KEY:-<your-api-key>}"
    printf "  ${BOLD}curl -X POST http://localhost:${port}/api/chat \\\\${NC}\n"
    printf "  ${BOLD}  -H \"Content-Type: application/json\" \\\\${NC}\n"
    printf "  ${BOLD}  -H \"X-API-Key: ${key}\" \\\\${NC}\n"
    printf "  ${BOLD}  -d '{\"content\": \"hello\"}'${NC}\n"
}

# ─── HTTPie Example ──────────────────────────────────────────────────────────
#
# Prints a ready-to-paste httpie command with the API key header.
# Usage: print_api_httpie_example [port]
#
print_api_httpie_example() {
    local port="${1:-8080}"
    local key="${RESOLVED_API_KEY:-<your-api-key>}"
    printf "  ${BOLD}http POST http://localhost:${port}/api/chat \\\\${NC}\n"
    printf "  ${BOLD}  X-API-Key:${key} \\\\${NC}\n"
    printf "  ${BOLD}  content=hello${NC}\n"
}

# ─── Security Info ────────────────────────────────────────────────────────────
#
# Prints the active security mode and key (if applicable).
#
# ─── API Key Sync ────────────────────────────────────────────────────────────
#
# Syncs the resolved API key to a specific env file.
# Usage: sync_api_key_to_env <target_file>
#
sync_api_key_to_env() {
    local target_file="${1:-}"
    [ -z "$target_file" ] || [ -z "${RESOLVED_API_KEY:-}" ] && return
    [ ! -f "$target_file" ] && return
    local current
    current=$(grep "^JAICLAW_API_KEY=" "$target_file" 2>/dev/null | cut -d= -f2)
    if [ "$current" != "$RESOLVED_API_KEY" ]; then
        sed -i.bak "s|^JAICLAW_API_KEY=.*|JAICLAW_API_KEY=${RESOLVED_API_KEY}|" "$target_file"
        rm -f "$target_file.bak"
    fi
}

# Syncs the resolved API key to all known env files in a compose directory.
# Usage: sync_api_key_to_all_envs [compose_dir]
#
sync_api_key_to_all_envs() {
    local compose_dir="${1:-.}"
    sync_api_key_to_env "$compose_dir/.env"
    sync_api_key_to_env "$compose_dir/.env.multitenant"
}

# ─── Security Info ────────────────────────────────────────────────────────────
#
print_security_info() {
    local mode="${JAICLAW_SECURITY_MODE:-api-key}"
    case "$mode" in
        api-key)
            ok "Security: API key mode"
            info "API Key: ${RESOLVED_API_KEY:-<not resolved>}"
            ;;
        jwt)
            ok "Security: JWT mode"
            ;;
        none)
            warn "Security: DISABLED (mode=none)"
            ;;
    esac
}

# ─── Auth Status Check ───────────────────────────────────────────────────────
#
# Checks OAuth token status and prints a warning if tokens are expiring/expired.
# Non-blocking — returns 0 even on auth issues (it's just a heads-up).
# Usage: check_auth_status [path_to_auth_status_script]
#
check_auth_status() {
    local auth_script="${1:-}"
    if [ -z "$auth_script" ]; then
        auth_script="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/auth-status.sh"
    fi
    [ ! -x "$auth_script" ] && return 0
    local status
    status=$("$auth_script" simple 2>/dev/null) || return 0
    case "$status" in
        OK)       return 0 ;;
        EXPIRING) warn "OAuth tokens expiring soon. Run './start.sh auth' to check or './start.sh login' to re-authenticate." ;;
        EXPIRED)  warn "OAuth tokens EXPIRED! Run './start.sh login <provider>' to re-authenticate." ;;
    esac
    return 0
}
