package io.jaiclaw.identity.auth;

/**
 * A resolved credential ready for use — the final output of {@link AuthProfileResolver}.
 *
 * @param apiKey   the resolved API key or access token
 * @param provider provider identifier
 * @param email    associated email (nullable)
 */
public record ResolvedCredential(String apiKey, String provider, String email) {}
