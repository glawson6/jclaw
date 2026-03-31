---
name: node-connect
description: Diagnose node connection and pairing failures for Android, iOS, and macOS companion apps.
version: 1.0.0
---

# Node Connect

Goal: find the one real route from node -> gateway, verify the gateway is advertising that route, then fix pairing/auth.

## Topology first

Decide which case you are in before proposing fixes:

- same machine / emulator / USB tunnel
- same LAN / local Wi-Fi
- same Tailscale tailnet
- public URL / reverse proxy

Do not mix them.

- Local Wi-Fi problem: do not switch to Tailscale unless remote access is actually needed.
- VPS / remote gateway problem: do not keep debugging `localhost` or LAN IPs.

## If ambiguous, ask first

If the setup is unclear or the failure report is vague, ask short clarifying questions before diagnosing.

Ask for:

- which route they intend: same machine, same LAN, Tailscale tailnet, or public URL
- whether they used QR/setup code or manual host/port
- the exact app text/status/error, quoted exactly if possible
- whether the device list shows a pending pairing request

Do not guess from `can't connect`.

## Canonical checks

Check the gateway configuration:

```bash
# Check gateway mode and bind settings
gateway config get gateway.mode
gateway config get gateway.bind
gateway config get gateway.tailscale.mode
gateway config get gateway.remote.url
gateway config get gateway.auth.mode
gateway config get gateway.auth.allowTailscale

# Check QR/setup code payload
gateway qr --json

# Check device and node status
gateway devices list
gateway nodes status
```

If Tailscale is part of the story:

```bash
tailscale status --json
```

## Root-cause map

If QR output says `Gateway is only bound to loopback`:

- remote node cannot connect yet
- fix the route, then generate a fresh setup code
- same LAN: use `gateway.bind=lan`
- same tailnet: prefer `gateway.tailscale.mode=serve` or use `gateway.bind=tailnet`
- public internet: set a real public URL or `gateway.remote.url`

If `gateway.bind=tailnet set, but no tailnet IP was found`:

- gateway host is not actually on Tailscale

If the app says `pairing required`:

- network route and auth worked
- approve the pending device

```bash
gateway devices list
gateway devices approve --latest
```

If the app says `bootstrap token invalid or expired`:

- old setup code
- generate a fresh one and rescan
- do this after any URL/auth fix too

If the app says `unauthorized`:

- wrong token/password, or wrong Tailscale expectation
- for Tailscale Serve, `gateway.auth.allowTailscale` must match the intended flow

## Fast heuristics

- Same Wi-Fi setup + gateway advertises `127.0.0.1`, `localhost`, or loopback-only config: wrong.
- Remote setup + setup/manual uses private LAN IP: wrong.
- Tailnet setup + gateway advertises LAN IP instead of MagicDNS / tailnet route: wrong.
- Public URL set but QR still advertises something else: inspect `urlSource`; config is not what you think.
- Device list shows pending requests: stop changing network config and approve first.

## Fix style

Reply with one concrete diagnosis and one route.

If there is not enough signal yet, ask for setup + exact app text instead of guessing.

Good:

- `The gateway is still loopback-only, so a node on another network can never reach it. Enable Tailscale Serve, restart the gateway, run qr again, rescan, then approve the pending device pairing.`

Bad:

- `Maybe LAN, maybe Tailscale, maybe port forwarding, maybe public URL.`
