---
name: canvas
description: Display HTML content on connected nodes via the canvas tools (canvas_present, canvas_eval, canvas_snapshot).
version: 1.0.0
---

# Canvas

Display HTML content on connected nodes using the canvas tools.

## Overview

The canvas tools let you present web content on any connected client's canvas view. Great for:

- Displaying games, visualizations, dashboards
- Showing generated HTML content
- Interactive demos and forms

## Tools

| Tool | Description |
| ---- | ----------- |
| `canvas_present` | Push HTML content for rich visual display |
| `canvas_eval` | Execute JavaScript in the canvas context |
| `canvas_snapshot` | Capture current canvas content as HTML |

## Architecture

```
+------------------+     +------------------+     +--------------+
|  Canvas Service  |---->|  File Manager    |---->|  HTTP Server |
|  (state + URL)   |     |  (disk storage)  |     |  Port 18793  |
+------------------+     +------------------+     +--------------+
                                                        |
                                                        v
                                                  +--------------+
                                                  |  Client App  |
                                                  |  (WebView)   |
                                                  +--------------+
```

1. **Canvas Service**: Manages per-session state, writes HTML via file manager, returns URLs
2. **File Manager**: Persists HTML files with UUID naming
3. **HTTP Server**: Serves the HTML files to connected clients

## Configuration

Canvas is configured via application properties:

```yaml
jaiclaw:
  canvas:
    enabled: true
    port: 18793
    host: 127.0.0.1
    live-reload: true
```

| Property | Default | Description |
| -------- | ------- | ----------- |
| `enabled` | false | Enable the canvas feature |
| `port` | 18793 | HTTP server port |
| `host` | 127.0.0.1 | Bind address |
| `live-reload` | true | Auto-reload on file changes |

## Workflow

### 1. Present HTML content

Use the `canvas_present` tool with an `html` parameter:

```
canvas_present html:"<html><body><h1>Hello Canvas!</h1></body></html>"
```

The tool returns a URL like `http://127.0.0.1:18793/<uuid>.html`.

### 2. Capture current state

Use `canvas_snapshot` to retrieve the current HTML content:

```
canvas_snapshot
```

Returns the raw HTML of the currently displayed canvas.

### 3. Execute JavaScript

Use `canvas_eval` to run JavaScript in the canvas context:

```
canvas_eval javascript:"document.title = 'Updated'"
```

Note: Requires a connected client with bidirectional communication.

## Tips

- Keep HTML self-contained (inline CSS/JS) for best results
- The canvas persists until you present new content or the session ends
- Live reload makes development fast — save and it updates automatically
- Use `canvas_snapshot` to capture state before modifying content
- Multi-tenant: each tenant gets isolated canvas state and file storage

## URL Format

Canvas content is served at:

```
http://{host}:{port}/{uuid}.html
```

The host and port come from the canvas configuration. The UUID is auto-generated per content push.

## Bind Modes

The canvas host binds based on configuration:

| Host Setting | Access |
| ------------ | ------ |
| `127.0.0.1` | Local only (default) |
| LAN IP | Same network |
| Tailscale IP | Same tailnet |
| `0.0.0.0` | All interfaces |

For remote clients, set the host to an accessible address.
