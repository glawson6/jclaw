package io.jaiclaw.identity.oauth.provider;

import io.jaiclaw.identity.oauth.OAuthFlowType;
import io.jaiclaw.identity.oauth.OAuthProviderConfig;

import java.util.List;

/** Google Gemini CLI OAuth provider configuration. */
public final class GoogleGeminiOAuthProvider {
    private GoogleGeminiOAuthProvider() {}

    public static OAuthProviderConfig config() {
        return config(null, null);
    }

    public static OAuthProviderConfig config(String clientId, String clientSecret) {
        return new OAuthProviderConfig(
                "google-gemini-cli",
                "https://accounts.google.com/o/oauth2/v2/auth",
                "https://oauth2.googleapis.com/token",
                "https://www.googleapis.com/oauth2/v3/userinfo",
                null,
                clientId != null ? clientId : envOrNull("GEMINI_CLI_OAUTH_CLIENT_ID"),
                clientSecret != null ? clientSecret : envOrNull("GEMINI_CLI_OAUTH_CLIENT_SECRET"),
                null,
                8085,
                "/oauth2callback",
                List.of("https://www.googleapis.com/auth/cloud-platform",
                        "https://www.googleapis.com/auth/userinfo.email",
                        "https://www.googleapis.com/auth/userinfo.profile"),
                OAuthFlowType.AUTHORIZATION_CODE
        );
    }

    private static String envOrNull(String name) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : null;
    }
}
