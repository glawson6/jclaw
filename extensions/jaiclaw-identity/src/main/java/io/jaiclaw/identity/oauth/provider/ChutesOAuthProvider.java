package io.jaiclaw.identity.oauth.provider;

import io.jaiclaw.identity.oauth.OAuthFlowType;
import io.jaiclaw.identity.oauth.OAuthProviderConfig;

import java.util.List;

/** Chutes OAuth provider configuration. */
public final class ChutesOAuthProvider {
    private ChutesOAuthProvider() {}

    public static OAuthProviderConfig config() {
        return config(null, null);
    }

    public static OAuthProviderConfig config(String clientId, String clientSecret) {
        return new OAuthProviderConfig(
                "chutes",
                "https://api.chutes.ai/idp/authorize",
                "https://api.chutes.ai/idp/token",
                "https://api.chutes.ai/idp/userinfo",
                null,
                clientId != null ? clientId : envOrNull("CHUTES_CLIENT_ID"),
                clientSecret != null ? clientSecret : envOrNull("CHUTES_CLIENT_SECRET"),
                envOrNull("CHUTES_OAUTH_REDIRECT_URI"),
                1456,
                "/oauth-callback",
                List.of("openid", "profile", "chutes:invoke"),
                OAuthFlowType.AUTHORIZATION_CODE
        );
    }

    private static String envOrNull(String name) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : null;
    }
}
