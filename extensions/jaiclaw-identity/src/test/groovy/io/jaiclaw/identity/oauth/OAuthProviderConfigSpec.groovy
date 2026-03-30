package io.jaiclaw.identity.oauth

import spock.lang.Specification

class OAuthProviderConfigSpec extends Specification {

    def "resolvedRedirectUri computes from port and path"() {
        given:
        OAuthProviderConfig config = new OAuthProviderConfig(
                "test-provider",
                "https://auth.example.com/authorize",
                "https://auth.example.com/token",
                null, null,
                "client-123", null,
                null, 8080, "/callback",
                List.of("openid"), OAuthFlowType.AUTHORIZATION_CODE)

        expect:
        config.resolvedRedirectUri() == "http://127.0.0.1:8080/callback"
    }

    def "resolvedRedirectUri returns explicit redirectUri when set"() {
        given:
        OAuthProviderConfig config = new OAuthProviderConfig(
                "test-provider",
                "https://auth.example.com/authorize",
                "https://auth.example.com/token",
                null, null,
                "client-123", null,
                "https://custom.example.com/callback", 8080, "/callback",
                List.of("openid"), OAuthFlowType.AUTHORIZATION_CODE)

        expect:
        config.resolvedRedirectUri() == "https://custom.example.com/callback"
    }

    def "all fields are accessible"() {
        given:
        OAuthProviderConfig config = new OAuthProviderConfig(
                "chutes", "https://api.chutes.ai/idp/authorize",
                "https://api.chutes.ai/idp/token",
                "https://api.chutes.ai/idp/userinfo",
                null,
                "chutes-client", "chutes-secret",
                null, 1456, "/oauth/callback",
                List.of("openid", "profile"), OAuthFlowType.AUTHORIZATION_CODE)

        expect:
        config.providerId() == "chutes"
        config.authorizeUrl() == "https://api.chutes.ai/idp/authorize"
        config.tokenUrl() == "https://api.chutes.ai/idp/token"
        config.userinfoUrl() == "https://api.chutes.ai/idp/userinfo"
        config.deviceCodeUrl() == null
        config.clientId() == "chutes-client"
        config.clientSecret() == "chutes-secret"
        config.callbackPort() == 1456
        config.callbackPath() == "/oauth/callback"
        config.scopes() == ["openid", "profile"]
        config.flowType() == OAuthFlowType.AUTHORIZATION_CODE
    }

    def "device code flow type"() {
        given:
        OAuthProviderConfig config = new OAuthProviderConfig(
                "qwen", "https://chat.qwen.ai/authorize",
                "https://chat.qwen.ai/api/v1/oauth2/token",
                null,
                "https://chat.qwen.ai/api/v1/oauth2/device/code",
                "qwen-client", null,
                null, 0, "/",
                List.of(), OAuthFlowType.DEVICE_CODE)

        expect:
        config.flowType() == OAuthFlowType.DEVICE_CODE
        config.deviceCodeUrl() != null
    }
}
