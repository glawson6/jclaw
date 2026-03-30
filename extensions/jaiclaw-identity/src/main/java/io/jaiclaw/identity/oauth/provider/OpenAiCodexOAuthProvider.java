package io.jaiclaw.identity.oauth.provider;

import io.jaiclaw.identity.oauth.OAuthFlowType;
import io.jaiclaw.identity.oauth.OAuthProviderConfig;

import java.util.List;

/** OpenAI Codex (ChatGPT) OAuth provider configuration. */
public final class OpenAiCodexOAuthProvider {
    private OpenAiCodexOAuthProvider() {}

    public static OAuthProviderConfig config() {
        return new OAuthProviderConfig(
                "openai-codex",
                "https://auth.openai.com/oauth/authorize",
                "https://auth.openai.com/oauth/token",
                null,
                null,
                envOrNull("OPENAI_CODEX_CLIENT_ID"),
                null,
                null,
                1455,
                "/auth/callback",
                List.of("openid", "profile", "email"),
                OAuthFlowType.AUTHORIZATION_CODE
        );
    }

    private static String envOrNull(String name) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : null;
    }
}
