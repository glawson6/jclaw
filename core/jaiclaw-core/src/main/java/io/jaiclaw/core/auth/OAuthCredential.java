package io.jaiclaw.core.auth;

/**
 * Refreshable OAuth credential with access and refresh tokens.
 *
 * @param provider      provider identifier (e.g. "openai-codex", "chutes")
 * @param access        access token (required)
 * @param refresh       refresh token (required for refresh)
 * @param expires       expiry timestamp in ms-since-epoch (required)
 * @param email         associated email (nullable)
 * @param clientId      OAuth client ID, stored for refresh (nullable)
 * @param accountId     provider-specific account ID (nullable)
 * @param projectId     Google Cloud project ID (nullable)
 * @param enterpriseUrl custom base URL for enterprise deployments (nullable)
 */
public record OAuthCredential(
        String provider,
        String access,
        String refresh,
        long expires,
        String email,
        String clientId,
        String accountId,
        String projectId,
        String enterpriseUrl
) implements AuthProfileCredential {

    /** Convenience constructor for common fields. */
    public OAuthCredential(String provider, String access, String refresh,
                           long expires, String email, String clientId) {
        this(provider, access, refresh, expires, email, clientId, null, null, null);
    }

    /** Returns a copy with updated tokens and expiry. */
    public OAuthCredential withRefreshedTokens(String newAccess, String newRefresh, long newExpires) {
        return new OAuthCredential(
                provider, newAccess,
                newRefresh != null ? newRefresh : refresh,
                newExpires, email, clientId, accountId, projectId, enterpriseUrl
        );
    }
}
