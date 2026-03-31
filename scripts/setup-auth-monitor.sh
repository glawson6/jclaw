#!/usr/bin/env bash
#
# JaiClaw Auth Monitor Setup — interactive wizard for cron/systemd timer installation
#
# Usage:
#   ./scripts/setup-auth-monitor.sh             # interactive wizard
#   ./scripts/setup-auth-monitor.sh --remove     # remove installed cron/timer
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONITOR_SCRIPT="$SCRIPT_DIR/auth-monitor.sh"

# ─── Colors ─────────────────────────────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
DIM='\033[2m'
BOLD='\033[1m'
NC='\033[0m'

info()   { printf "${CYAN}▸${NC} %s\n" "$*"; }
ok()     { printf "${GREEN}✓${NC} %s\n" "$*"; }
warn()   { printf "${YELLOW}!${NC} %s\n" "$*"; }
err()    { printf "${RED}✗${NC} %s\n" "$*" >&2; }
header() { printf "\n${BOLD}${CYAN}── %s ──${NC}\n\n" "$*"; }

CRON_TAG="# jaiclaw-auth-monitor"
SYSTEMD_TIMER_NAME="jaiclaw-auth-monitor"
SYSTEMD_USER_DIR="$HOME/.config/systemd/user"

# ─── Remove ──────────────────────────────────────────────────────────────────

remove_monitor() {
    header "Remove Auth Monitor"

    local removed=false

    # Remove cron entry
    if crontab -l 2>/dev/null | grep -q "$CRON_TAG"; then
        crontab -l 2>/dev/null | grep -v "$CRON_TAG" | grep -v "auth-monitor.sh" | crontab -
        ok "Removed cron entry"
        removed=true
    fi

    # Remove systemd timer
    if [ -f "$SYSTEMD_USER_DIR/${SYSTEMD_TIMER_NAME}.timer" ]; then
        systemctl --user stop "${SYSTEMD_TIMER_NAME}.timer" 2>/dev/null || true
        systemctl --user disable "${SYSTEMD_TIMER_NAME}.timer" 2>/dev/null || true
        rm -f "$SYSTEMD_USER_DIR/${SYSTEMD_TIMER_NAME}.timer"
        rm -f "$SYSTEMD_USER_DIR/${SYSTEMD_TIMER_NAME}.service"
        systemctl --user daemon-reload 2>/dev/null || true
        ok "Removed systemd timer"
        removed=true
    fi

    if [ "$removed" = false ]; then
        info "No auth monitor installation found"
    fi
}

# ─── Install cron ────────────────────────────────────────────────────────────

install_cron() {
    local interval_mins="$1"
    local env_vars="$2"

    # Remove existing entry if present
    if crontab -l 2>/dev/null | grep -q "$CRON_TAG"; then
        crontab -l 2>/dev/null | grep -v "$CRON_TAG" | grep -v "auth-monitor.sh" | crontab -
    fi

    local cron_line="*/${interval_mins} * * * * ${env_vars}${MONITOR_SCRIPT} ${CRON_TAG}"
    (crontab -l 2>/dev/null; echo "$cron_line") | crontab -
    ok "Installed cron job (every ${interval_mins} minutes)"
    echo ""
    info "Cron entry:"
    printf "  ${DIM}%s${NC}\n" "$cron_line"
}

# ─── Install systemd ────────────────────────────────────────────────────────

install_systemd() {
    local interval_mins="$1"
    local env_vars="$2"

    mkdir -p "$SYSTEMD_USER_DIR"

    # Build Environment= lines for the service
    local env_lines=""
    if [ -n "$env_vars" ]; then
        # Parse space-separated KEY=VALUE pairs
        for kv in $env_vars; do
            env_lines="${env_lines}Environment=${kv}\n"
        done
    fi

    # Service unit
    cat > "$SYSTEMD_USER_DIR/${SYSTEMD_TIMER_NAME}.service" << EOF
[Unit]
Description=JaiClaw Auth Monitor
After=network-online.target

[Service]
Type=oneshot
ExecStart=${MONITOR_SCRIPT}
$(echo -e "$env_lines")
EOF

    # Timer unit
    cat > "$SYSTEMD_USER_DIR/${SYSTEMD_TIMER_NAME}.timer" << EOF
[Unit]
Description=JaiClaw Auth Monitor Timer

[Timer]
OnCalendar=*:0/${interval_mins}
Persistent=true

[Install]
WantedBy=timers.target
EOF

    systemctl --user daemon-reload
    systemctl --user enable --now "${SYSTEMD_TIMER_NAME}.timer"
    ok "Installed systemd user timer (every ${interval_mins} minutes)"
    echo ""
    info "Check status:"
    printf "  ${BOLD}systemctl --user status ${SYSTEMD_TIMER_NAME}.timer${NC}\n"
}

# ─── Wizard ──────────────────────────────────────────────────────────────────

wizard() {
    header "JaiClaw Auth Monitor Setup"

    # Step 1: Show current auth status
    if [ -x "$SCRIPT_DIR/auth-status.sh" ]; then
        info "Current auth status:"
        "$SCRIPT_DIR/auth-status.sh" full 2>/dev/null || true
    fi

    # Step 2: ntfy.sh topic
    echo ""
    printf "${BOLD}Notification: ntfy.sh${NC}\n"
    echo "  ntfy.sh is a free push notification service."
    echo "  Enter a topic name to receive mobile/desktop alerts."
    echo ""
    read -rp "$(printf "${CYAN}▸${NC} ntfy.sh topic (leave blank to skip): ")" ntfy_topic

    # Step 3: Email
    echo ""
    printf "${BOLD}Notification: Email${NC}\n"
    echo "  Requires the 'mail' command on this system."
    echo ""
    read -rp "$(printf "${CYAN}▸${NC} Email address (leave blank to skip): ")" notify_email

    if [ -z "$ntfy_topic" ] && [ -z "$notify_email" ]; then
        warn "No notification method configured. The monitor will only log to ~/.jaiclaw/auth-monitor.log"
        echo ""
        read -rp "$(printf "${CYAN}▸${NC} Continue anyway? (y/N): ")" confirm
        if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
            echo "Setup cancelled."
            return
        fi
    fi

    # Step 4: Warning threshold
    echo ""
    read -rp "$(printf "${CYAN}▸${NC} Warning threshold in hours [2]: ")" warn_hours
    warn_hours="${warn_hours:-2}"

    # Step 5: Check interval
    echo ""
    read -rp "$(printf "${CYAN}▸${NC} Check interval in minutes [30]: ")" interval_mins
    interval_mins="${interval_mins:-30}"

    # Step 6: Scheduler type
    echo ""
    printf "${BOLD}Scheduler:${NC}\n"

    local has_systemd=false
    if command -v systemctl &>/dev/null && systemctl --user status 2>/dev/null | grep -q "loaded"; then
        has_systemd=true
    fi

    if [ "$has_systemd" = true ]; then
        echo "  1. cron (traditional, works everywhere)"
        echo "  2. systemd user timer (recommended on Linux with systemd)"
        echo ""
        read -rp "$(printf "${CYAN}▸${NC} Choice [1]: ")" sched_choice
        sched_choice="${sched_choice:-1}"
    else
        sched_choice=1
        info "Using cron (systemd user timers not available)"
    fi

    # Build env var string
    local env_vars=""
    if [ -n "$ntfy_topic" ]; then
        env_vars="${env_vars}JAICLAW_NOTIFY_NTFY=${ntfy_topic} "
    fi
    if [ -n "$notify_email" ]; then
        env_vars="${env_vars}JAICLAW_NOTIFY_EMAIL=${notify_email} "
    fi
    if [ "$warn_hours" != "2" ]; then
        env_vars="${env_vars}JAICLAW_AUTH_WARN_HOURS=${warn_hours} "
    fi

    # Install
    echo ""
    case "$sched_choice" in
        2)  install_systemd "$interval_mins" "$env_vars" ;;
        *)  install_cron "$interval_mins" "$env_vars" ;;
    esac

    # Step 7: Run initial check
    echo ""
    info "Running initial check..."
    if [ -n "$ntfy_topic" ]; then export JAICLAW_NOTIFY_NTFY="$ntfy_topic"; fi
    if [ -n "$notify_email" ]; then export JAICLAW_NOTIFY_EMAIL="$notify_email"; fi
    if [ "$warn_hours" != "2" ]; then export JAICLAW_AUTH_WARN_HOURS="$warn_hours"; fi
    "$MONITOR_SCRIPT" 2>/dev/null && ok "Initial check complete" || warn "Initial check finished with warnings"

    echo ""
    ok "Auth monitor setup complete!"
    echo ""
    info "Log file: ~/.jaiclaw/auth-monitor.log"
    if [ -n "$ntfy_topic" ]; then
        info "ntfy.sh topic: $ntfy_topic"
    fi
    if [ -n "$notify_email" ]; then
        info "Email: $notify_email"
    fi
    echo ""
}

# ─── Main ─────────────────────────────────────────────────────────────────────

if [ ! -x "$MONITOR_SCRIPT" ]; then
    err "auth-monitor.sh not found at $MONITOR_SCRIPT"
    exit 1
fi

case "${1:-}" in
    --remove)  remove_monitor ;;
    --help|-h) echo "Usage: $0 [--remove]"; echo "  Interactive wizard to set up cron/systemd auth monitoring." ;;
    *)         wizard ;;
esac
