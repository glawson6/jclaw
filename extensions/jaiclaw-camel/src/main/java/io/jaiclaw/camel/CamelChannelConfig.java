package io.jaiclaw.camel;

/**
 * Configuration for a single Camel-backed channel.
 *
 * @param channelId   unique channel identifier (required, non-blank)
 * @param displayName human-readable name (defaults to "Camel: {channelId}")
 * @param accountId   account identifier for session keys (defaults to "default")
 * @param outboundUri optional Camel endpoint URI for outbound messages (null = inbound-only)
 * @param inboundUri  optional Camel endpoint URI for inbound source (e.g. "aws2-s3://inbox")
 * @param outbound    optional JaiClaw channel name for cross-channel outbound routing (e.g. "telegram")
 * @param stateless   when true, each inbound message gets a fresh ephemeral session (no history persistence)
 */
public record CamelChannelConfig(
        String channelId,
        String displayName,
        String accountId,
        String outboundUri,
        String inboundUri,
        String outbound,
        boolean stateless
) {
    public CamelChannelConfig {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("channelId must not be null or blank");
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = "Camel: " + channelId;
        }
        if (accountId == null || accountId.isBlank()) {
            accountId = "default";
        }
        if (outboundUri != null && !outboundUri.isBlank()
                && outbound != null && !outbound.isBlank()) {
            throw new IllegalArgumentException(
                    "outboundUri and outbound are mutually exclusive for channel: " + channelId);
        }
    }

}
