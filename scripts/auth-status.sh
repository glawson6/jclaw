#!/usr/bin/env bash
#
# JaiClaw Auth Status — standalone auth profile status checker
#
# Reads ~/.jaiclaw/agents/default/agent/auth-profiles.json and external CLI
# credential files to report OAuth token status.
#
# Usage:
#   ./scripts/auth-status.sh              # colored terminal table (default)
#   ./scripts/auth-status.sh full         # same as above
#   ./scripts/auth-status.sh json         # machine-readable JSON
#   ./scripts/auth-status.sh simple       # one-line: OK|EXPIRING|EXPIRED|MISSING
#
# Exit codes:
#   0 = OK (all tokens valid)
#   1 = EXPIRED (at least one token expired)
#   2 = EXPIRING (at least one token expiring within 1 hour)
#   3 = MISSING (no auth-profiles.json found)
#
set -euo pipefail

# ─── Config ─────────────────────────────────────────────────────────────────

JAICLAW_STATE_DIR="${JAICLAW_STATE_DIR:-$HOME/.jaiclaw}"
JAICLAW_AGENT_DIR="${JAICLAW_AGENT_DIR:-$JAICLAW_STATE_DIR/agents/default/agent}"
AUTH_PROFILES="$JAICLAW_AGENT_DIR/auth-profiles.json"

EXPIRY_WARN_MS=3600000  # 1 hour in milliseconds

# External CLI credential paths
CLAUDE_CLI_CREDS="$HOME/.claude/.credentials.json"
CODEX_CLI_AUTH="$HOME/.codex/auth.json"
QWEN_CLI_CREDS="$HOME/.qwen/oauth_creds.json"
MINIMAX_CLI_CREDS="$HOME/.minimax/oauth_creds.json"

# ─── Colors ─────────────────────────────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
DIM='\033[2m'
BOLD='\033[1m'
NC='\033[0m'

# ─── Time helpers ───────────────────────────────────────────────────────────

now_ms() {
    if command -v python3 &>/dev/null; then
        python3 -c 'import time; print(int(time.time() * 1000))'
    elif command -v perl &>/dev/null; then
        perl -e 'use Time::HiRes qw(time); printf "%d\n", time() * 1000'
    else
        echo $(( $(date +%s) * 1000 ))
    fi
}

# Format epoch millis to human-readable date
format_date() {
    local epoch_ms="$1"
    local epoch_s=$(( epoch_ms / 1000 ))
    if date -r "$epoch_s" "+%Y-%m-%d %H:%M" 2>/dev/null; then
        return
    fi
    # Linux fallback
    date -d "@$epoch_s" "+%Y-%m-%d %H:%M" 2>/dev/null || echo "unknown"
}

# Format remaining time as human-readable
format_remaining() {
    local remaining_ms="$1"
    if [ "$remaining_ms" -le 0 ]; then
        echo "expired"
        return
    fi
    local remaining_s=$(( remaining_ms / 1000 ))
    local hours=$(( remaining_s / 3600 ))
    local mins=$(( (remaining_s % 3600) / 60 ))
    if [ "$hours" -gt 0 ]; then
        echo "${hours}h ${mins}m"
    else
        echo "${mins}m"
    fi
}

# ─── JSON parsing ──────────────────────────────────────────────────────────

# Detect best JSON parser available
JSON_PARSER=""
detect_json_parser() {
    if command -v jq &>/dev/null; then
        JSON_PARSER="jq"
    elif command -v python3 &>/dev/null; then
        JSON_PARSER="python3"
    else
        JSON_PARSER="grep"
    fi
}

# Extract a field from JSON using best available parser
# Usage: json_field <file> <jq_expression> [python_expression]
json_field() {
    local file="$1"
    local jq_expr="$2"
    local py_expr="${3:-}"

    case "$JSON_PARSER" in
        jq)
            jq -r "$jq_expr // empty" "$file" 2>/dev/null
            ;;
        python3)
            if [ -n "$py_expr" ]; then
                python3 -c "
import json, sys
try:
    data = json.load(open('$file'))
    result = $py_expr
    if result is not None:
        print(result)
except Exception:
    pass
" 2>/dev/null
            fi
            ;;
        grep)
            # Last resort — basic grep for simple fields
            return 1
            ;;
    esac
}

# ─── Profile parsing ──────────────────────────────────────────────────────

# Parse auth-profiles.json and output profile status lines
# Format: profileId|type|state|expires_ms|email
parse_profiles() {
    local file="$1"
    local now="$2"

    case "$JSON_PARSER" in
        jq)
            jq -r --argjson now "$now" --argjson warn "$EXPIRY_WARN_MS" '
                .profiles // {} | to_entries[] |
                .key as $id |
                .value |
                (
                    if .type == "oauth" then
                        if (.expires // 0) <= 0 then "MISSING"
                        elif .expires <= $now then "EXPIRED"
                        elif (.expires - $now) < $warn then "EXPIRING"
                        else "OK"
                        end
                    elif .type == "api_key" then "ok"
                    elif .type == "token" then
                        if (.expires // 0) <= 0 then "ok"
                        elif .expires <= $now then "EXPIRED"
                        elif (.expires - $now) < $warn then "EXPIRING"
                        else "ok"
                        end
                    else "unknown"
                    end
                ) as $state |
                "\($id)|\(.type // "unknown")|\($state)|\(.expires // 0)|\(.email // "")"
            ' "$file" 2>/dev/null
            ;;
        python3)
            python3 -c "
import json, sys
now = $now
warn = $EXPIRY_WARN_MS
try:
    data = json.load(open('$file'))
    profiles = data.get('profiles', {})
    for pid, cred in profiles.items():
        ctype = cred.get('type', 'unknown')
        expires = cred.get('expires', 0) or 0
        email = cred.get('email', '') or ''
        if ctype == 'oauth':
            if expires <= 0:
                state = 'MISSING'
            elif expires <= now:
                state = 'EXPIRED'
            elif (expires - now) < warn:
                state = 'EXPIRING'
            else:
                state = 'OK'
        elif ctype == 'api_key':
            state = 'ok'
        elif ctype == 'token':
            if expires <= 0:
                state = 'ok'
            elif expires <= now:
                state = 'EXPIRED'
            elif (expires - now) < warn:
                state = 'EXPIRING'
            else:
                state = 'ok'
        else:
            state = 'unknown'
        print(f'{pid}|{ctype}|{state}|{expires}|{email}')
except Exception:
    pass
" 2>/dev/null
            ;;
        *)
            return 1
            ;;
    esac
}

# ─── External CLI checks ──────────────────────────────────────────────────

# Check an external CLI credential file, return "state|remaining_desc"
check_external_cli() {
    local name="$1"
    local file="$2"
    local expires_field="$3"
    local now="$4"

    if [ ! -f "$file" ]; then
        echo "MISSING|"
        return
    fi

    local expires_ms=""
    case "$JSON_PARSER" in
        jq)
            expires_ms=$(jq -r "$expires_field // empty" "$file" 2>/dev/null)
            ;;
        python3)
            # Convert jq-style path to python — handle simple cases
            local py_path
            py_path=$(echo "$expires_field" | sed 's/^\./data/' | sed 's/\.\([a-zA-Z_][a-zA-Z0-9_]*\)/.get("\1", 0)/g')
            expires_ms=$(python3 -c "
import json
try:
    data = json.load(open('$file'))
    v = $py_path
    if v: print(int(v))
except Exception:
    pass
" 2>/dev/null)
            ;;
    esac

    if [ -z "$expires_ms" ] || [ "$expires_ms" = "null" ] || [ "$expires_ms" = "0" ]; then
        echo "MISSING|"
        return
    fi

    # Some files store seconds, some milliseconds — normalize to ms
    # If the value is less than 10 billion, it's probably seconds
    if [ "$expires_ms" -lt 10000000000 ] 2>/dev/null; then
        expires_ms=$(( expires_ms * 1000 ))
    fi

    local remaining=$(( expires_ms - now ))
    if [ "$remaining" -le 0 ]; then
        echo "EXPIRED|"
    elif [ "$remaining" -lt "$EXPIRY_WARN_MS" ]; then
        echo "EXPIRING|$(format_remaining "$remaining")"
    else
        echo "OK|$(format_remaining "$remaining")"
    fi
}

# ─── Output modes ─────────────────────────────────────────────────────────

output_full() {
    local now
    now=$(now_ms)

    printf "\n${BOLD}${CYAN}── JaiClaw Auth Status ──${NC}\n\n"

    # Profiles
    if [ ! -f "$AUTH_PROFILES" ]; then
        printf "${DIM}Profiles ($AUTH_PROFILES):${NC}\n"
        printf "  ${YELLOW}No auth-profiles.json found${NC}\n"
    else
        printf "${DIM}Profiles ($AUTH_PROFILES):${NC}\n"
        printf "  ${BOLD}%-35s %-10s %-12s %s${NC}\n" "PROFILE ID" "TYPE" "STATE" "EXPIRES"
        printf "  ${DIM}%s${NC}\n" "$(printf '%0.s─' {1..75})"

        local has_profiles=false
        while IFS='|' read -r pid ptype pstate pexpires pemail; do
            has_profiles=true
            local expires_str="-"
            local remaining_str=""
            if [ "$pexpires" -gt 0 ] 2>/dev/null; then
                expires_str=$(format_date "$pexpires")
                local remaining=$(( pexpires - now ))
                remaining_str=" ($(format_remaining "$remaining"))"
            fi

            local state_color="$NC"
            case "$pstate" in
                OK|ok)      state_color="$GREEN" ;;
                EXPIRING)   state_color="$YELLOW" ;;
                EXPIRED)    state_color="$RED" ;;
                MISSING)    state_color="$YELLOW" ;;
            esac

            printf "  %-35s %-10s ${state_color}%-12s${NC} %s%s\n" \
                "$pid" "$ptype" "$pstate" "$expires_str" "$remaining_str"
        done < <(parse_profiles "$AUTH_PROFILES" "$now")

        if [ "$has_profiles" = false ]; then
            printf "  ${DIM}(no profiles)${NC}\n"
        fi
    fi

    # External CLIs
    echo ""
    printf "${DIM}External CLI:${NC}\n"

    local externals=(
        "Claude Code|$CLAUDE_CLI_CREDS|.claudeAiOauth.expiresAt"
        "Codex|$CODEX_CLI_AUTH|.expiresAt"
        "Qwen|$QWEN_CLI_CREDS|.expiresAt"
        "MiniMax|$MINIMAX_CLI_CREDS|.expiresAt"
    )

    for entry in "${externals[@]}"; do
        IFS='|' read -r ext_name ext_file ext_field <<< "$entry"
        local result
        result=$(check_external_cli "$ext_name" "$ext_file" "$ext_field" "$now")
        IFS='|' read -r ext_state ext_remaining <<< "$result"

        local state_color="$NC"
        local display_remaining=""
        case "$ext_state" in
            OK)       state_color="$GREEN"; display_remaining=" ($ext_remaining)" ;;
            EXPIRING) state_color="$YELLOW"; display_remaining=" ($ext_remaining)" ;;
            EXPIRED)  state_color="$RED" ;;
            MISSING)  state_color="$DIM" ;;
        esac

        printf "  %-15s ${state_color}%s${NC}%s\n" "$ext_name:" "$ext_state" "$display_remaining"
    done
    echo ""
}

output_json() {
    local now
    now=$(now_ms)

    if [ "$JSON_PARSER" = "jq" ]; then
        # Build JSON with jq
        local profiles_json="[]"
        if [ -f "$AUTH_PROFILES" ]; then
            profiles_json=$(jq -c --argjson now "$now" --argjson warn "$EXPIRY_WARN_MS" '[
                .profiles // {} | to_entries[] |
                {
                    id: .key,
                    type: (.value.type // "unknown"),
                    state: (
                        if .value.type == "oauth" then
                            if (.value.expires // 0) <= 0 then "MISSING"
                            elif .value.expires <= $now then "EXPIRED"
                            elif (.value.expires - $now) < $warn then "EXPIRING"
                            else "OK"
                            end
                        elif .value.type == "api_key" then "ok"
                        elif .value.type == "token" then
                            if (.value.expires // 0) <= 0 then "ok"
                            elif .value.expires <= $now then "EXPIRED"
                            elif (.value.expires - $now) < $warn then "EXPIRING"
                            else "ok"
                            end
                        else "unknown"
                        end
                    ),
                    expires: (.value.expires // 0),
                    email: (.value.email // null)
                }
            ]' "$AUTH_PROFILES" 2>/dev/null || echo "[]")
        fi

        # External CLIs
        local externals_json="{"
        local first=true
        for entry in "claude|$CLAUDE_CLI_CREDS|.claudeAiOauth.expiresAt" \
                     "codex|$CODEX_CLI_AUTH|.expiresAt" \
                     "qwen|$QWEN_CLI_CREDS|.expiresAt" \
                     "minimax|$MINIMAX_CLI_CREDS|.expiresAt"; do
            IFS='|' read -r ext_key ext_file ext_field <<< "$entry"
            local result
            result=$(check_external_cli "$ext_key" "$ext_file" "$ext_field" "$now")
            IFS='|' read -r ext_state ext_remaining <<< "$result"
            [ "$first" = true ] && first=false || externals_json+=","
            externals_json+="\"$ext_key\":\"$ext_state\""
        done
        externals_json+="}"

        # Overall status
        local overall
        overall=$(compute_overall "$now")

        jq -n \
            --argjson profiles "$profiles_json" \
            --argjson external "$externals_json" \
            --arg overall "$overall" \
            --arg file "$AUTH_PROFILES" \
            --argjson timestamp "$now" \
            '{
                timestamp: $timestamp,
                file: $file,
                overall: $overall,
                profiles: $profiles,
                external: $external
            }'
    elif [ "$JSON_PARSER" = "python3" ]; then
        python3 -c "
import json, sys, os, time

now = $now
warn = $EXPIRY_WARN_MS
auth_file = '$AUTH_PROFILES'
result = {'timestamp': now, 'file': auth_file, 'overall': 'MISSING', 'profiles': [], 'external': {}}

if os.path.exists(auth_file):
    try:
        data = json.load(open(auth_file))
        profiles = data.get('profiles', {})
        for pid, cred in profiles.items():
            ctype = cred.get('type', 'unknown')
            expires = cred.get('expires', 0) or 0
            email = cred.get('email')
            if ctype == 'oauth':
                if expires <= 0: state = 'MISSING'
                elif expires <= now: state = 'EXPIRED'
                elif (expires - now) < warn: state = 'EXPIRING'
                else: state = 'OK'
            elif ctype == 'api_key': state = 'ok'
            elif ctype == 'token':
                if expires <= 0: state = 'ok'
                elif expires <= now: state = 'EXPIRED'
                elif (expires - now) < warn: state = 'EXPIRING'
                else: state = 'ok'
            else: state = 'unknown'
            result['profiles'].append({'id': pid, 'type': ctype, 'state': state, 'expires': expires, 'email': email})
    except Exception:
        pass

# External CLIs
ext_files = {
    'claude': ('$CLAUDE_CLI_CREDS', ['claudeAiOauth', 'expiresAt']),
    'codex': ('$CODEX_CLI_AUTH', ['expiresAt']),
    'qwen': ('$QWEN_CLI_CREDS', ['expiresAt']),
    'minimax': ('$MINIMAX_CLI_CREDS', ['expiresAt']),
}
for key, (fpath, fields) in ext_files.items():
    if not os.path.exists(fpath):
        result['external'][key] = 'MISSING'
        continue
    try:
        d = json.load(open(fpath))
        v = d
        for f in fields:
            v = v.get(f, 0) if isinstance(v, dict) else 0
        v = int(v or 0)
        if v < 10000000000: v *= 1000
        if v <= 0: result['external'][key] = 'MISSING'
        elif v <= now: result['external'][key] = 'EXPIRED'
        elif (v - now) < warn: result['external'][key] = 'EXPIRING'
        else: result['external'][key] = 'OK'
    except Exception:
        result['external'][key] = 'MISSING'

# Compute overall
states = [p['state'] for p in result['profiles']] + list(result['external'].values())
if not states or all(s == 'MISSING' for s in states):
    result['overall'] = 'MISSING'
elif any(s == 'EXPIRED' for s in states):
    result['overall'] = 'EXPIRED'
elif any(s == 'EXPIRING' for s in states):
    result['overall'] = 'EXPIRING'
else:
    result['overall'] = 'OK'

print(json.dumps(result, indent=2))
" 2>/dev/null
    else
        echo '{"error": "No JSON parser available (install jq or python3)"}'
    fi
}

output_simple() {
    local now
    now=$(now_ms)
    compute_overall "$now"
}

# ─── Compute overall status ───────────────────────────────────────────────

compute_overall() {
    local now="$1"
    local worst="OK"
    local has_any=false

    # Check profiles
    if [ -f "$AUTH_PROFILES" ]; then
        while IFS='|' read -r pid ptype pstate pexpires pemail; do
            has_any=true
            case "$pstate" in
                EXPIRED)  worst="EXPIRED" ;;
                EXPIRING) [ "$worst" != "EXPIRED" ] && worst="EXPIRING" ;;
            esac
        done < <(parse_profiles "$AUTH_PROFILES" "$now")
    fi

    # Check external CLIs
    for entry in "claude|$CLAUDE_CLI_CREDS|.claudeAiOauth.expiresAt" \
                 "codex|$CODEX_CLI_AUTH|.expiresAt" \
                 "qwen|$QWEN_CLI_CREDS|.expiresAt" \
                 "minimax|$MINIMAX_CLI_CREDS|.expiresAt"; do
        IFS='|' read -r ext_key ext_file ext_field <<< "$entry"
        local result
        result=$(check_external_cli "$ext_key" "$ext_file" "$ext_field" "$now")
        IFS='|' read -r ext_state ext_remaining <<< "$result"
        case "$ext_state" in
            OK|EXPIRING|EXPIRED) has_any=true ;;
        esac
        case "$ext_state" in
            EXPIRED)  worst="EXPIRED" ;;
            EXPIRING) [ "$worst" != "EXPIRED" ] && worst="EXPIRING" ;;
        esac
    done

    if [ "$has_any" = false ]; then
        echo "MISSING"
    else
        echo "$worst"
    fi
}

# ─── Main ─────────────────────────────────────────────────────────────────

detect_json_parser

MODE="${1:-full}"

case "$MODE" in
    full)
        output_full
        ;;
    json)
        output_json
        ;;
    simple)
        output_simple
        ;;
    *)
        echo "Usage: $0 [full|json|simple]" >&2
        exit 1
        ;;
esac

# Set exit code based on overall status
NOW=$(now_ms)
OVERALL=$(compute_overall "$NOW")
case "$OVERALL" in
    OK)       exit 0 ;;
    EXPIRED)  exit 1 ;;
    EXPIRING) exit 2 ;;
    MISSING)  exit 3 ;;
    *)        exit 0 ;;
esac
