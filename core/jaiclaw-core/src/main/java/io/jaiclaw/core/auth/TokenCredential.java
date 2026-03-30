package io.jaiclaw.core.auth;

/**
 * Static bearer / PAT token credential, not refreshable.
 * Either {@code token} (inline) or {@code tokenRef} (indirect) must be set.
 *
 * @param provider provider identifier
 * @param token    inline plaintext token (nullable if tokenRef is set)
 * @param tokenRef indirect secret reference (nullable if token is set)
 * @param expires  expiry timestamp in ms-since-epoch (nullable = no expiry)
 * @param email    associated email (nullable)
 */
public record TokenCredential(
        String provider,
        String token,
        SecretRef tokenRef,
        Long expires,
        String email
) implements AuthProfileCredential {

    /** Convenience constructor for inline token without ref. */
    public TokenCredential(String provider, String token, Long expires, String email) {
        this(provider, token, null, expires, email);
    }
}
