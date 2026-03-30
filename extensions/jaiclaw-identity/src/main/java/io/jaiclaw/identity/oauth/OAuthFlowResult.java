package io.jaiclaw.identity.oauth;

/**
 * Result of a completed OAuth flow (authorization code or device code).
 *
 * @param accessToken  the access token
 * @param refreshToken the refresh token (nullable for some flows)
 * @param expiresAt    expiry timestamp in ms-since-epoch
 * @param email        user email from userinfo (nullable)
 * @param accountId    provider-specific account ID (nullable)
 * @param projectId    Google Cloud project ID (nullable)
 * @param clientId     the client ID used (stored for refresh)
 */
public record OAuthFlowResult(
        String accessToken,
        String refreshToken,
        long expiresAt,
        String email,
        String accountId,
        String projectId,
        String clientId
) {}
