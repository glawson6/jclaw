package io.jaiclaw.identity.oauth.provider;

import io.jaiclaw.identity.oauth.OAuthFlowType;
import io.jaiclaw.identity.oauth.OAuthProviderConfig;

import java.util.List;

/** Qwen Portal device code OAuth provider configuration. */
public final class QwenDeviceCodeProvider {
    private QwenDeviceCodeProvider() {}

    public static OAuthProviderConfig config() {
        return new OAuthProviderConfig(
                "qwen-portal",
                null,
                "https://chat.qwen.ai/api/v1/oauth2/token",
                null,
                "https://chat.qwen.ai/api/v1/oauth2/device/code",
                "f0304373b74a44d2b584a3fb70ca9e56",
                null,
                null,
                0,
                null,
                List.of("openid", "profile", "email", "model.completion"),
                OAuthFlowType.DEVICE_CODE
        );
    }
}
