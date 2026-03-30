package io.jaiclaw.identity.oauth;

import java.util.List;

/**
 * Configuration for an OAuth provider (authorization server endpoints, client credentials, scopes).
 *
 * @param providerId    unique identifier (e.g. "chutes", "openai-codex")
 * @param authorizeUrl  authorization endpoint (auth code flow)
 * @param tokenUrl      token endpoint (both flows)
 * @param userinfoUrl   userinfo endpoint (nullable, for fetching email after login)
 * @param deviceCodeUrl device code endpoint (device code flow)
 * @param clientId      OAuth client ID
 * @param clientSecret  OAuth client secret (nullable, public clients)
 * @param redirectUri   callback URI (default: http://127.0.0.1:{port}{path})
 * @param callbackPort  loopback port for redirect (auth code flow)
 * @param callbackPath  path on loopback server (auth code flow)
 * @param scopes        requested scopes
 * @param flowType      which OAuth flow to use
 */
public record OAuthProviderConfig(
        String providerId,
        String authorizeUrl,
        String tokenUrl,
        String userinfoUrl,
        String deviceCodeUrl,
        String clientId,
        String clientSecret,
        String redirectUri,
        int callbackPort,
        String callbackPath,
        List<String> scopes,
        OAuthFlowType flowType
) {
    /** Compute the redirect URI from port and path if not explicitly set. */
    public String resolvedRedirectUri() {
        if (redirectUri != null && !redirectUri.isBlank()) return redirectUri;
        return "http://127.0.0.1:" + callbackPort + callbackPath;
    }
}
