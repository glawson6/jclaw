package io.jaiclaw.core.auth;

import java.util.Map;

/**
 * Static API key credential. Either {@code key} (inline) or {@code keyRef} (indirect) must be set.
 *
 * @param provider provider identifier
 * @param key      inline plaintext key (nullable if keyRef is set)
 * @param keyRef   indirect secret reference (nullable if key is set)
 * @param email    associated email (nullable)
 * @param metadata provider-specific metadata (e.g. accountId, gatewayId)
 */
public record ApiKeyCredential(
        String provider,
        String key,
        SecretRef keyRef,
        String email,
        Map<String, String> metadata
) implements AuthProfileCredential {

    /** Convenience constructor for inline key without metadata. */
    public ApiKeyCredential(String provider, String key, String email) {
        this(provider, key, null, email, Map.of());
    }
}
