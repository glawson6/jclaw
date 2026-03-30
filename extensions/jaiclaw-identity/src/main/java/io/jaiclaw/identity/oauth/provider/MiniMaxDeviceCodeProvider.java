package io.jaiclaw.identity.oauth.provider;

import io.jaiclaw.identity.oauth.OAuthFlowType;
import io.jaiclaw.identity.oauth.OAuthProviderConfig;

import java.util.List;

/** MiniMax Portal device code OAuth provider configuration. */
public final class MiniMaxDeviceCodeProvider {
    private MiniMaxDeviceCodeProvider() {}

    public static OAuthProviderConfig config() {
        return config(false);
    }

    /**
     * @param useChinaEndpoint if true, use api.minimaxi.com instead of api.minimax.io
     */
    public static OAuthProviderConfig config(boolean useChinaEndpoint) {
        String baseUrl = useChinaEndpoint ? "https://api.minimaxi.com" : "https://api.minimax.io";
        return new OAuthProviderConfig(
                "minimax-portal",
                null,
                baseUrl + "/oauth/token",
                null,
                baseUrl + "/oauth/code",
                "78257093-7e40-4613-99e0-527b14b39113",
                null,
                null,
                0,
                null,
                List.of("group_id", "profile", "model.completion"),
                OAuthFlowType.DEVICE_CODE
        );
    }
}
