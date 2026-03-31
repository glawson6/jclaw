---
name: voice-call
description: Make and manage phone calls via telephony providers (Twilio)
alwaysInclude: false
---

# Voice Call

Make and manage phone calls through telephony providers. Currently supports Twilio with WebSocket media streaming for real-time conversation.

## Available Tools

### voice_call_initiate
Initiate an outbound phone call.

**Parameters:**
- `to` (required): Phone number to call in E.164 format (e.g., +15551234567)
- `message` (optional): Message to speak when the call connects
- `mode` (optional): Call mode — `conversation` (interactive, default) or `notify` (one-way message then hangup)

**Example:**
```
voice_call_initiate(to: "+15551234567", message: "Hello, this is a reminder about your appointment tomorrow at 3 PM.", mode: "notify")
```

### voice_call_continue
Speak a message and wait for the user's response on an active call. Returns the transcribed speech.

**Parameters:**
- `callId` (required): ID of the active call
- `message` (required): Message to speak before listening

**Example:**
```
voice_call_continue(callId: "abc-123", message: "Would you like to reschedule? Say yes or no.")
```

### voice_call_speak
Speak a message on an active call without waiting for a response.

**Parameters:**
- `callId` (required): ID of the active call
- `message` (required): Message to speak

**Example:**
```
voice_call_speak(callId: "abc-123", message: "Please hold while I look that up.")
```

### voice_call_end
End an active phone call.

**Parameters:**
- `callId` (required): ID of the call to end

**Example:**
```
voice_call_end(callId: "abc-123")
```

### voice_call_status
Get the current status of a call, including state, direction, and full transcript.

**Parameters:**
- `callId` (required): ID of the call to check

**Example:**
```
voice_call_status(callId: "abc-123")
```

## Configuration

Enable via `application.yml`:

```yaml
jaiclaw:
  voice-call:
    enabled: true
    provider: twilio
    twilio:
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
    serve:
      webhook-path: /voice/webhook
      public-url: https://your-domain.com
    outbound:
      from-number: "+15551234567"
      default-mode: CONVERSATION
      max-duration-sec: 600
      silence-timeout-sec: 30
    inbound:
      policy: disabled  # disabled, open, allowlist
      greeting: "Hello, how can I help you?"
      allowed-from:
        - "+15559876543"
```

## Call Modes

- **NOTIFY**: Deliver a one-way message, then automatically hang up. Use for reminders, alerts, and notifications.
- **CONVERSATION**: Interactive multi-turn voice call. The bot speaks, listens for user speech, and can respond. Use for customer service, surveys, and interactive tasks.

## Inbound Call Policies

- `disabled`: Reject all inbound calls (default)
- `open`: Accept all inbound calls
- `allowlist`: Accept only from numbers in the `allowed-from` list
