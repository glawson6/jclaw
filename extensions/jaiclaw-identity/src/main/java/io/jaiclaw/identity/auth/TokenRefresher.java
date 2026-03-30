package io.jaiclaw.identity.auth;

import io.jaiclaw.core.auth.OAuthCredential;

/**
 * SPI for provider-specific OAuth token refresh.
 * Implementations handle the {@code refresh_token} grant for a specific provider.
 */
public interface TokenRefresher {

    /** The provider ID this refresher handles (e.g. "chutes", "qwen-portal"). */
    String providerId();

    /**
     * Refresh the credential, returning updated tokens.
     *
     * @param current the current (expired) OAuth credential
     * @return updated credential with fresh access token and expiry
     * @throws TokenRefreshException if refresh fails
     */
    OAuthCredential refresh(OAuthCredential current) throws TokenRefreshException;
}
