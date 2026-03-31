---
name: imsg
description: iMessage/SMS CLI for listing chats, history, and sending messages via Messages.app.
requiredBins: [imsg]
platforms: [darwin]
version: 1.0.0
---

# imsg

Use `imsg` to read and send iMessage/SMS via macOS Messages.app.

## When to Use

- User explicitly asks to send iMessage or SMS
- Reading iMessage conversation history
- Checking recent Messages.app chats
- Sending to phone numbers or Apple IDs

## When NOT to Use

- Telegram, Signal, WhatsApp, Discord, Slack messages — use their respective channels
- Group chat management (adding/removing members) — not supported
- Bulk/mass messaging — always confirm with user first

## Requirements

- macOS with Messages.app signed in
- Full Disk Access for terminal
- Automation permission for Messages.app (for sending)

## Common Commands

### List Chats

```bash
imsg chats --limit 10 --json
```

### View History

```bash
imsg history --chat-id 1 --limit 20 --json
imsg history --chat-id 1 --limit 20 --attachments --json
```

### Watch for New Messages

```bash
imsg watch --chat-id 1 --attachments
```

### Send Messages

```bash
imsg send --to "+14155551212" --text "Hello!"
imsg send --to "+14155551212" --text "Check this out" --file /path/to/image.jpg
imsg send --to "+14155551212" --text "Hi" --service imessage
imsg send --to "+14155551212" --text "Hi" --service sms
```

## Safety Rules

1. Always confirm recipient and message content before sending
2. Never send to unknown numbers without explicit user approval
3. Be careful with attachments — confirm file path exists
4. Rate limit yourself — don't spam
