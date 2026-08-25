# SEV-005b — `MessagingMcpToolProvider.handleBroadcastMessage` recipient-tenant validation

**Filed:** 2026-08-25
**Source finding:** `security-report-2026-08-24.md` SEV-005 (partial)
**PR context:** deferred from PR-2 of the security-fix plan (`~/.claude/plans/abundant-crafting-alpaca.md`)

## Problem

`MessagingMcpToolProvider.handleBroadcastMessage()` at
`extensions/jaiclaw-messaging/src/main/java/io/jaiclaw/messaging/mcp/MessagingMcpToolProvider.java:211-276`
iterates a caller-supplied `recipients` list. Each recipient is
`{channelId, peerId}`. Today only the channel allow-list is enforced —
there's no validation that the caller's tenant owns the `peerId` on the
target channel.

Impact in a multi-tenant deployment: an authenticated caller in tenant A
may broadcast a message to a `peerId` (e.g., a Telegram chat id, Slack
user id) that belongs to tenant B, if the caller can guess or enumerate
peer ids. This is a **cross-tenant outbound impersonation vector**, not a
data-read vulnerability — but it's still a broken-isolation finding.

A regression guard was added as a `@PendingFeature` in
`MessagingMcpToolProviderSpec` — it currently fails on purpose to
document the gap. Remove `@PendingFeature` when the fix lands.

## Related latent issue

`handleAgentChatAsync()` at
`extensions/jaiclaw-messaging/.../MessagingMcpToolProvider.java:310-317`
hardcodes `sessionKey = "default:" + channelId + ":mcp:" + peerId` — the
`"default"` prefix ignores `TenantContextHolder.get().getTenantId()`.
The async continuation (`future.thenAccept`) also runs on a background
thread without `TenantContextPropagator`. Same class of bug. Consider
fixing both in the same design pass.

## Design questions

1. **Does `ChannelRegistry` track per-peer tenant today?** Grep suggests
   no — channels are tenant-scoped at the adapter level (`SlackAdapter`,
   `TelegramAdapter`), but the individual `peerId` values aren't linked
   back to a tenant in a lookup service.
2. **Options:**
   - Add a `PeerTenantResolver` SPI that channels implement — provider
     asks "which tenant owns this peerId on this channel?" before
     sending. Requires each adapter to maintain a peer→tenant index.
   - Restrict recipients to channels the caller-tenant owns. Simpler
     but coarser — assumes channels are 1:1 with tenants, which is
     mostly true today but not enforced.
   - Block cross-channel broadcast entirely when in multi-tenant mode,
     with an explicit opt-in property.
3. **Also address `handleAgentChatAsync`:**
   - Read tenant from `TenantContextHolder.get()` instead of hardcoding.
   - Wrap the async continuation with `TenantContextPropagator.wrap(...)`
     (see `PipelineEventBroadcaster.java` for the pattern).

## Acceptance criteria

- [ ] `broadcast_message` rejects recipients whose peer/channel doesn't
      belong to the caller's tenant, with a `CrossTenantAccessException`
      translated to `McpToolResult.error("cross-tenant access denied")`.
- [ ] `handleAgentChatAsync` resolves session key from
      `TenantContextHolder.get()` and propagates the tenant into the
      async continuation.
- [ ] `MessagingMcpToolProviderSpec` — remove `@PendingFeature`; add a
      pair of specs that assert same-tenant recipient succeeds and a
      cross-tenant recipient is rejected.
- [ ] Update `security-report-2026-08-24.md` SEV-005 to mark SEV-005b
      as fixed.
