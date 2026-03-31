---
name: sonoscli
description: Control Sonos speakers (discover/status/play/volume/group).
requiredBins: [sonos]
version: 1.0.0
---

# Sonos CLI

Use `sonos` to control Sonos speakers on the local network.

## Quick start

- `sonos discover`
- `sonos status --name "Kitchen"`
- `sonos play|pause|stop --name "Kitchen"`
- `sonos volume set 15 --name "Kitchen"`

## Common tasks

- Grouping: `sonos group status|join|unjoin|party|solo`
- Favorites: `sonos favorites list|open`
- Queue: `sonos queue list|play|clear`
- Spotify search (via SMAPI): `sonos smapi search --service "Spotify" --category tracks "query"`

## Notes

- If SSDP fails, specify `--ip <speaker-ip>`.
- Spotify Web API search is optional and requires `SPOTIFY_CLIENT_ID/SECRET`.
- If there is an error, check the troubleshooting section and offer advice if there is a decent match.

## Troubleshooting

### `sonos discover` - `no route to host`

- On error `Error: write udp4 0.0.0.0:64326->239.255.255.250:1900: sendto: no route to host`
  - The port number after `0.0.0.0:` is ephemeral and will change
  - The `sendto: no route to host` should stay consistent
- Advise the user that Local Network permissions may need to be enabled for the host parent process
- An alternative option is to use a sandbox (docker container) with network access allowed

### `sonos discover` - `bind: operation not permitted`

- On error `Error: listen udp4 0.0.0.0:0: bind: operation not permitted`
- Advise the user that they may be running via a sandbox that does not permit network access
