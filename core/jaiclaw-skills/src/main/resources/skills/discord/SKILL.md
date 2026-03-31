---
name: discord
description: Discord actions via the discord MCP tools (send, react, edit, delete, pin, thread, poll).
requiredConfig: [jaiclaw.channels.discord.bot-token]
version: 1.0.0
---

# Discord Tools

Use the Discord MCP tools for rich Discord interactions beyond basic messaging.

## Musts

- Always provide `channelId` and `messageId` where required.
- Prefer explicit IDs over names.
- Avoid Markdown tables in outbound Discord messages.
- Mention users as `<@USER_ID>`.

## Available Tools

| Tool | Description |
| ---- | ----------- |
| `discord_send` | Send a message to a channel |
| `discord_read` | Read recent messages from a channel |
| `discord_react` | Add a reaction emoji to a message |
| `discord_edit` | Edit a message |
| `discord_delete` | Delete a message |
| `discord_pin` | Pin a message |
| `discord_unpin` | Unpin a message |
| `discord_thread_create` | Create a thread from a message |
| `discord_poll` | Create a poll in a channel |

## Examples

### Send a message

```json
{
  "channelId": "123456789",
  "message": "Hello from JaiClaw!",
  "silent": true
}
```

### Send with silent (no notification)

Set `silent: true` to suppress Discord push notifications.

### React to a message

```json
{
  "channelId": "123456789",
  "messageId": "987654321",
  "emoji": "✅"
}
```

### Read recent messages

```json
{
  "channelId": "123456789",
  "limit": 20
}
```

### Edit a message

```json
{
  "channelId": "123456789",
  "messageId": "987654321",
  "message": "Updated content"
}
```

### Delete a message

```json
{
  "channelId": "123456789",
  "messageId": "987654321"
}
```

### Create a poll

```json
{
  "channelId": "123456789",
  "question": "Lunch?",
  "options": ["Pizza", "Sushi", "Salad"],
  "multiSelect": false,
  "durationHours": 24
}
```

### Pin a message

```json
{
  "channelId": "123456789",
  "messageId": "987654321"
}
```

### Create a thread

```json
{
  "channelId": "123456789",
  "messageId": "987654321",
  "threadName": "Bug triage"
}
```

## Writing Style (Discord)

- Short, conversational, low ceremony.
- No markdown tables in messages.
- Mention users as `<@USER_ID>`.
