package io.jaiclaw.identity.oauth

import spock.lang.AutoCleanup
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

class OAuthCallbackServerIT extends Specification {

    @AutoCleanup
    MockOAuthServer mockTokenServer = new MockOAuthServer()

    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()

    private static int findFreePort() {
        ServerSocket socket = new ServerSocket(0)
        int port = socket.localPort
        socket.close()
        port
    }

    def "successful callback returns authorization code"() {
        given:
        int callbackPort = findFreePort()
        String expectedState = PkceGenerator.generateState()

        when:
        OAuthCallbackServer server = new OAuthCallbackServer(
                callbackPort, '/callback', expectedState, Duration.ofSeconds(10))

        and: 'simulate browser redirect with auth code and state'
        CompletableFuture<OAuthCallbackServer.OAuthCallbackResult> resultFuture =
                CompletableFuture.supplyAsync {
                    server.awaitCallback()
                }

        // Small delay to ensure server is ready
        Thread.sleep(100)
        HttpRequest browserRedirect = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:${callbackPort}/callback?code=AUTH_CODE_FROM_BROWSER&state=${expectedState}"))
                .GET()
                .build()
        HttpResponse<String> browserResponse = httpClient.send(browserRedirect, HttpResponse.BodyHandlers.ofString())

        then:
        browserResponse.statusCode() == 200
        browserResponse.body().contains('Login Successful')

        and:
        OAuthCallbackServer.OAuthCallbackResult callbackResult = resultFuture.get()
        callbackResult.code() == 'AUTH_CODE_FROM_BROWSER'
        callbackResult.state() == expectedState

        cleanup:
        server?.close()
    }

    def "callback with wrong state triggers CSRF error"() {
        given:
        int callbackPort = findFreePort()
        String expectedState = 'correct-state-value'
        OAuthCallbackServer server = new OAuthCallbackServer(
                callbackPort, '/callback', expectedState, Duration.ofSeconds(10))

        when: 'send callback with wrong state'
        Thread.sleep(100)
        HttpRequest wrongStateRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:${callbackPort}/callback?code=SOME_CODE&state=wrong-state"))
                .GET()
                .build()
        HttpResponse<String> response = httpClient.send(wrongStateRequest, HttpResponse.BodyHandlers.ofString())

        then: 'HTTP response indicates error'
        response.statusCode() == 400
        response.body().contains('CSRF')

        when: 'awaiting callback throws OAuthFlowException'
        server.awaitCallback()

        then:
        OAuthFlowException ex = thrown(OAuthFlowException)
        ex.message.contains('CSRF')

        cleanup:
        server?.close()
    }

    def "callback with error parameter triggers error response"() {
        given:
        int callbackPort = findFreePort()
        String state = PkceGenerator.generateState()
        OAuthCallbackServer server = new OAuthCallbackServer(
                callbackPort, '/callback', state, Duration.ofSeconds(10))

        when: 'send error callback'
        Thread.sleep(100)
        HttpRequest errorCallback = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:${callbackPort}/callback?error=access_denied"))
                .GET()
                .build()
        HttpResponse<String> response = httpClient.send(errorCallback, HttpResponse.BodyHandlers.ofString())

        then: 'HTTP response indicates error'
        response.statusCode() == 400
        response.body().contains('Login Failed')

        when: 'awaiting callback throws OAuthFlowException'
        server.awaitCallback()

        then:
        OAuthFlowException ex = thrown(OAuthFlowException)
        ex.message.contains('access_denied')

        cleanup:
        server?.close()
    }

    def "awaitCallback times out when no callback received"() {
        given:
        int callbackPort = findFreePort()
        String state = PkceGenerator.generateState()

        when:
        OAuthCallbackServer server = new OAuthCallbackServer(
                callbackPort, '/callback', state, Duration.ofMillis(200))
        server.awaitCallback()

        then:
        thrown(TimeoutException)

        cleanup:
        server?.close()
    }

    def "full callback + token exchange end-to-end"() {
        given:
        int callbackPort = findFreePort()
        String state = PkceGenerator.generateState()
        PkceGenerator.PkceChallenge pkce = PkceGenerator.generate()

        mockTokenServer.tokenEndpoint('/token', '''{
            "access_token": "e2e-access-token",
            "refresh_token": "e2e-refresh-token",
            "expires_in": 3600
        }''')
        mockTokenServer.userinfoEndpoint('/userinfo', '''{
            "email": "e2e@example.com",
            "sub": "e2e-user-id"
        }''')

        OAuthProviderConfig config = new OAuthProviderConfig(
                'e2e-provider',
                "${mockTokenServer.baseUrl()}/authorize",
                "${mockTokenServer.baseUrl()}/token",
                "${mockTokenServer.baseUrl()}/userinfo",
                null,
                'e2e-client-id',
                null, null,
                callbackPort,
                '/callback',
                ['openid', 'email'],
                OAuthFlowType.AUTHORIZATION_CODE
        )

        AuthorizationCodeFlow authFlow = new AuthorizationCodeFlow(httpClient)

        when: 'start callback server and simulate browser redirect'
        OAuthCallbackServer server = new OAuthCallbackServer(
                callbackPort, '/callback', state, Duration.ofSeconds(10))

        CompletableFuture<OAuthCallbackServer.OAuthCallbackResult> callbackFuture =
                CompletableFuture.supplyAsync { server.awaitCallback() }

        Thread.sleep(100)
        HttpRequest browserRedirect = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:${callbackPort}/callback?code=E2E_AUTH_CODE&state=${state}"))
                .GET()
                .build()
        httpClient.send(browserRedirect, HttpResponse.BodyHandlers.ofString())

        OAuthCallbackServer.OAuthCallbackResult callback = callbackFuture.get()

        and: 'exchange the code for tokens'
        OAuthFlowResult result = authFlow.exchangeCode(config, callback.code(), pkce.verifier())

        then:
        callback.code() == 'E2E_AUTH_CODE'
        result.accessToken() == 'e2e-access-token'
        result.refreshToken() == 'e2e-refresh-token'
        result.email() == 'e2e@example.com'
        result.accountId() == 'e2e-user-id'
        result.clientId() == 'e2e-client-id'

        and: 'token request included PKCE verifier'
        Map<String, String> tokenParams = mockTokenServer.getRequests('/token')[0].formParams()
        tokenParams['code'] == 'E2E_AUTH_CODE'
        tokenParams['code_verifier'] == pkce.verifier()

        cleanup:
        server?.close()
    }
}
