#!/usr/bin/env bash
#
# JaiClaw Auth Monitor — cron-compatible OAuth expiry monitor
#
# Checks auth profile status and sends notifications when tokens are
# expiring or expired. Designed to be run from cron or systemd timers.
#
# Cron example (check every 30 minutes):
#   */30 * * * * /path/to/jaiclaw/scripts/auth-monitor.sh
#
# Configuration (env vars):
#   JAICLAW_NOTIFY_NTFY    — ntfy.sh topic name (e.g., "jaiclaw-auth")
#   JAICLAW_NOTIFY_EMAIL   — email address for alerts (via `mail` command)
#   JAICLAW_AUTH_WARN_HOURS — warning threshold in hours (default: 2)
#   JAICLAW_NTFY_SERVER    — ntfy server URL (default: https://ntfy.sh)
#
# State file: ~/.jaiclaw/auth-monitor-state (last notification epoch)
# Log file:   ~/.jaiclaw/auth-monitor.log
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUTH_STATUS_SCRIPT="$SCRIPT_DIR/auth-status.sh"

JAICLAW_STATE_DIR="${JAICLAW_STATE_DIR:-$HOME/.jaiclaw}"
STATE_FILE="$JAICLAW_STATE_DIR/auth-monitor-state"
LOG_FILE="$JAICLAW_STATE_DIR/auth-monitor.log"

# Notification config
NTFY_TOPIC="${JAICLAW_NOTIFY_NTFY:-}"
NTFY_SERVER="${JAICLAW_NTFY_SERVER:-https://ntfy.sh}"
NOTIFY_EMAIL="${JAICLAW_NOTIFY_EMAIL:-}"
WARN_HOURS="${JAICLAW_AUTH_WARN_HOURS:-2}"

# Notification cooldown (1 hour in seconds)
COOLDOWN_SECS=3600

# ─── Logging ─────────────────────────────────────────────────────────────────

log() {
    local timestamp
    timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    echo "[$timestamp] $*" >> "$LOG_FILE"
}

# ─── Notification state ──────────────────────────────────────────────────────

last_notify_epoch() {
    if [ -f "$STATE_FILE" ]; then
        cat "$STATE_FILE" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

save_notify_epoch() {
    mkdir -p "$(dirname "$STATE_FILE")"
    date +%s > "$STATE_FILE"
}

is_cooled_down() {
    local last
    last=$(last_notify_epoch)
    local now
    now=$(date +%s)
    local elapsed=$(( now - last ))
    [ "$elapsed" -ge "$COOLDOWN_SECS" ]
}

# ─── Notifications ───────────────────────────────────────────────────────────

notify_ntfy() {
    local title="$1"
    local message="$2"
    local priority="${3:-default}"

    if [ -z "$NTFY_TOPIC" ]; then return; fi

    curl -sf \
        -H "Title: $title" \
        -H "Priority: $priority" \
        -H "Tags: key,warning" \
        -d "$message" \
        "${NTFY_SERVER}/${NTFY_TOPIC}" >/dev/null 2>&1 || {
        log "WARN: Failed to send ntfy notification to ${NTFY_SERVER}/${NTFY_TOPIC}"
    }
}

notify_email() {
    local subject="$1"
    local body="$2"

    if [ -z "$NOTIFY_EMAIL" ]; then return; fi

    if command -v mail &>/dev/null; then
        echo "$body" | mail -s "$subject" "$NOTIFY_EMAIL" 2>/dev/null || {
            log "WARN: Failed to send email notification to $NOTIFY_EMAIL"
        }
    else
        log "WARN: 'mail' command not found — cannot send email notification"
    fi
}

send_notification() {
    local status="$1"
    local title=""
    local message=""
    local priority="default"

    case "$status" in
        EXPIRING)
            title="JaiClaw: OAuth tokens expiring soon"
            message="One or more OAuth tokens will expire within ${WARN_HOURS} hours. Run './start.sh login <provider>' to re-authenticate."
            priority="default"
            ;;
        EXPIRED)
            title="JaiClaw: OAuth tokens EXPIRED"
            message="One or more OAuth tokens have expired! Run './start.sh login <provider>' to re-authenticate immediately."
            priority="high"
            ;;
        *)
            return
            ;;
    esac

    notify_ntfy "$title" "$message" "$priority"
    notify_email "$title" "$message"
    save_notify_epoch
    log "NOTIFY: Sent $status notification (ntfy=${NTFY_TOPIC:-none}, email=${NOTIFY_EMAIL:-none})"
}

# ─── Main ─────────────────────────────────────────────────────────────────────

mkdir -p "$JAICLAW_STATE_DIR"

# Verify auth-status.sh exists
if [ ! -x "$AUTH_STATUS_SCRIPT" ]; then
    log "ERROR: auth-status.sh not found or not executable at $AUTH_STATUS_SCRIPT"
    exit 1
fi

# Override warning threshold if configured (convert hours to ms for auth-status.sh)
if [ "$WARN_HOURS" != "2" ]; then
    export EXPIRY_WARN_MS=$(( WARN_HOURS * 3600000 ))
fi

# Check auth status
STATUS=$("$AUTH_STATUS_SCRIPT" simple 2>/dev/null) || STATUS="MISSING"

log "CHECK: status=$STATUS"

case "$STATUS" in
    OK)
        # All good — nothing to do
        ;;
    EXPIRING|EXPIRED)
        if is_cooled_down; then
            send_notification "$STATUS"
        else
            log "SKIP: Notification cooldown still active"
        fi
        ;;
    MISSING)
        log "INFO: No auth profiles found — nothing to monitor"
        ;;
esac
