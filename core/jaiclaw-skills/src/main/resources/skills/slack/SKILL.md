---
name: slack
description: Slack actions via the slack MCP tools (send, react, edit, delete, pin, member info, emoji list).
requiredConfig: [jaiclaw.channels.slack.bot-token]
version: 1.0.0
---

# Slack Tools

Use the Slack MCP tools for rich Slack interactions beyond basic messaging.

## Inputs to collect

- `channelId` — Slack channel ID (e.g. `C123`).
- `messageId` — Slack message timestamp (e.g. `1712023032.1234`).
- For reactions, an `emoji` (Unicode or `:name:` format).

Message context lines include `ts` and `channel` fields you can reuse directly.

## Available Tools

| Tool | Description |
| ---- | ----------- |
| `slack_send` | Send a message to a channel or user |
| `slack_read` | Read recent messages from a channel |
| `slack_react` | Add a reaction emoji to a message |
| `slack_edit` | Edit a message |
| `slack_delete` | Delete a message |
| `slack_pin` | Pin a message |
| `slack_unpin` | Unpin a message |
| `slack_list_pins` | List pinned items in a channel |
| `slack_member_info` | Get info about a Slack user |
| `slack_emoji_list` | List custom emoji in the workspace |

## Action Groups

| Group | Default | Notes |
| ----- | ------- | ----- |
| reactions | enabled | React + list reactions |
| messages | enabled | Read/send/edit/delete |
| pins | enabled | Pin/unpin/list |
| memberInfo | enabled | Member info |
| emojiList | enabled | Custom emoji list |

## Examples

### React to a message

```json
{
  "channelId": "C123",
  "messageId": "1712023032.1234",
  "emoji": "✅"
}
```

### Send a message

```json
{
  "channelId": "C123",
  "content": "Hello from JaiClaw"
}
```

### Edit a message

```json
{
  "channelId": "C123",
  "messageId": "1712023032.1234",
  "content": "Updated text"
}
```

### Delete a message

```json
{
  "channelId": "C123",
  "messageId": "1712023032.1234"
}
```

### Read recent messages

```json
{
  "channelId": "C123",
  "limit": 20
}
```

### Pin a message

```json
{
  "channelId": "C123",
  "messageId": "1712023032.1234"
}
```

### List pinned items

```json
{
  "channelId": "C123"
}
```

### Get member info

```json
{
  "userId": "U123"
}
```

### List custom emoji

```json
{}
```

## Ideas to try

- React with ✅ to mark completed tasks.
- Pin key decisions or weekly status updates.
