package io.jaiclaw.identity.oauth

import io.jaiclaw.identity.auth.AuthProfileStoreManager
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.TempDir

import java.net.http.HttpClient
import java.nio.file.Path
import java.time.Duration

class AuthorizationCodeFlowIT extends Specification {

    @AutoCleanup
    MockOAuthServer mockServer = new MockOAuthServer()

    @TempDir
    Path tempDir

    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()

    AuthorizationCodeFlow flow = new AuthorizationCodeFlow(httpClient)

    private OAuthProviderConfig providerConfig(String userinfoPath = '/userinfo') {
        new OAuthProviderConfig(
                'test-provider',
                "${mockServer.baseUrl()}/authorize",
                "${mockServer.baseUrl()}/token",
                userinfoPath ? "${mockServer.baseUrl()}${userinfoPath}" : null,
                null,       // deviceCodeUrl
                'test-client-id',
                null,       // clientSecret
                null,       // redirectUri (auto-resolved)
                18888,      // callbackPort
                '/callback',
                ['openid', 'email'],
                OAuthFlowType.AUTHORIZATION_CODE
        )
    }

    def "buildAuthorizeUrl includes PKCE params and state"() {
        given:
        OAuthProviderConfig config = providerConfig()
        PkceGenerator.PkceChallenge pkce = PkceGenerator.generate()
        String state = PkceGenerator.generateState()

        when:
        String url = flow.buildAuthorizeUrl(config, pkce, state)

        then:
        url.startsWith("${mockServer.baseUrl()}/authorize?")
        url.contains("response_type=code")
        url.contains("client_id=test-client-id")
        url.contains("code_challenge=${URLEncoder.encode(pkce.challenge(), 'UTF-8')}")
        url.contains("code_challenge_method=S256")
        url.contains("state=${state}")
        url.contains("scope=openid+email")
    }

    def "exchangeCode sends correct form params and returns tokens"() {
        given:
        OAuthProviderConfig config = providerConfig(null) // no userinfo
        mockServer.tokenEndpoint('/token', '''{
            "access_token": "mock-access-token",
            "refresh_token": "mock-refresh-token",
            "token_type": "Bearer",
            "expires_in": 3600
        }''')

        when:
        OAuthFlowResult result = flow.exchangeCode(config, 'AUTH_CODE_123', 'test-verifier')

        then: 'result contains correct tokens'
        result.accessToken() == 'mock-access-token'
        result.refreshToken() == 'mock-refresh-token'
        result.clientId() == 'test-client-id'
        result.expiresAt() > System.currentTimeMillis()

        and: 'token request sent correct form params'
        List<MockOAuthServer.RecordedRequest> requests = mockServer.getRequests('/token')
        requests.size() == 1
        Map<String, String> params = requests[0].formParams()
        params['grant_type'] == 'authorization_code'
        params['client_id'] == 'test-client-id'
        params['code'] == 'AUTH_CODE_123'
        params['code_verifier'] == 'test-verifier'
    }

    def "exchangeCode fetches userinfo when endpoint is configured"() {
        given:
        OAuthProviderConfig config = providerConfig('/userinfo')
        mockServer.tokenEndpoint('/token', '''{
            "access_token": "access-with-userinfo",
            "refresh_token": "refresh-123",
            "expires_in": 7200
        }''')
        mockServer.userinfoEndpoint('/userinfo', '''{
            "email": "test@example.com",
            "sub": "user-123"
        }''')

        when:
        OAuthFlowResult result = flow.exchangeCode(config, 'AUTH_CODE_456', 'verifier-456')

        then:
        result.accessToken() == 'access-with-userinfo'
        result.email() == 'test@example.com'
        result.accountId() == 'user-123'

        and: 'userinfo request was made with Bearer token'
        List<MockOAuthServer.RecordedRequest> userinfoRequests = mockServer.getRequests('/userinfo')
        userinfoRequests.size() == 1
        userinfoRequests[0].headers['Authorization'] == 'Bearer access-with-userinfo'
    }

    def "exchangeCode gracefully handles userinfo failure"() {
        given:
        OAuthProviderConfig config = providerConfig('/userinfo')
        mockServer.tokenEndpoint('/token', '''{
            "access_token": "access-ok",
            "expires_in": 3600
        }''')
        mockServer.errorEndpoint('/userinfo', 500, 'server_error', 'Internal error')

        when:
        OAuthFlowResult result = flow.exchangeCode(config, 'AUTH_CODE_789', 'verifier-789')

        then: 'tokens are still returned even though userinfo failed'
        result.accessToken() == 'access-ok'
        result.email() == null
        result.accountId() == null
    }

    def "exchangeCode throws on token endpoint error"() {
        given:
        OAuthProviderConfig config = providerConfig(null)
        mockServer.errorEndpoint('/token', 400, 'invalid_grant', 'Code expired')

        when:
        flow.exchangeCode(config, 'EXPIRED_CODE', 'verifier')

        then:
        OAuthFlowException ex = thrown(OAuthFlowException)
        ex.message.contains('400')
    }

    def "exchangeCode result can be stored via AuthProfileStoreManager"() {
        given:
        OAuthProviderConfig config = providerConfig('/userinfo')
        mockServer.tokenEndpoint('/token', '''{
            "access_token": "stored-access-token",
            "refresh_token": "stored-refresh-token",
            "expires_in": 3600
        }''')
        mockServer.userinfoEndpoint('/userinfo', '''{
            "email": "stored@example.com",
            "sub": "stored-user"
        }''')

        AuthProfileStoreManager storeManager = new AuthProfileStoreManager(tempDir)
        Path agentDir = tempDir.resolve('agents/default/agent')

        when: 'exchange code for tokens (simulates what login() does internally)'
        OAuthFlowResult result = flow.exchangeCode(config, 'STORE_CODE', 'store-verifier')

        and: 'store the credential (same logic as OAuthFlowManager.login)'
        String profileId = "test-provider:${result.email()}"
        io.jaiclaw.core.auth.OAuthCredential credential = new io.jaiclaw.core.auth.OAuthCredential(
                'test-provider', result.accessToken(), result.refreshToken(), result.expiresAt(),
                result.email(), result.clientId(), result.accountId(), null, null)
        storeManager.upsertProfile(agentDir, profileId, credential)

        then:
        result.accessToken() == 'stored-access-token'
        result.email() == 'stored@example.com'

        and: 'credential was persisted to store'
        def store = storeManager.loadForAgent(agentDir)
        store.profiles().containsKey('test-provider:stored@example.com')
        def storedCred = store.profiles().get('test-provider:stored@example.com') as io.jaiclaw.core.auth.OAuthCredential
        storedCred.access() == 'stored-access-token'
        storedCred.refresh() == 'stored-refresh-token'
        storedCred.email() == 'stored@example.com'
    }
}
