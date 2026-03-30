package io.jaiclaw.identity.oauth

import spock.lang.AutoCleanup
import spock.lang.Specification

import java.net.http.HttpClient
import java.time.Duration

class DeviceCodeFlowIT extends Specification {

    @AutoCleanup
    MockOAuthServer mockServer = new MockOAuthServer()

    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()

    DeviceCodeFlow flow = new DeviceCodeFlow(httpClient)

    private OAuthProviderConfig providerConfig() {
        new OAuthProviderConfig(
                'test-device-provider',
                null,       // authorizeUrl
                "${mockServer.baseUrl()}/token",
                null,       // userinfoUrl
                "${mockServer.baseUrl()}/device/code",
                'device-client-id',
                null,       // clientSecret
                null,       // redirectUri
                0,          // callbackPort
                null,       // callbackPath
                ['openid'],
                OAuthFlowType.DEVICE_CODE
        )
    }

    def "requestDeviceCode returns device code response"() {
        given:
        OAuthProviderConfig config = providerConfig()
        mockServer.deviceCodeEndpoint('/device/code', '''{
            "device_code": "DEVICE_CODE_ABC",
            "user_code": "ABCD-1234",
            "verification_uri": "https://example.com/activate",
            "interval": 1,
            "expires_in": 300
        }''')

        when:
        DeviceCodeFlow.DeviceCodeResponse response = flow.requestDeviceCode(config)

        then:
        response.deviceCode() == 'DEVICE_CODE_ABC'
        response.userCode() == 'ABCD-1234'
        response.verificationUri() == 'https://example.com/activate'
        response.interval() == 1
        response.expiresIn() == 300

        and: 'request was sent with correct client_id'
        List<MockOAuthServer.RecordedRequest> requests = mockServer.getRequests('/device/code')
        requests.size() == 1
        requests[0].formParams()['client_id'] == 'device-client-id'
    }

    def "pollForToken retries on authorization_pending then succeeds"() {
        given:
        OAuthProviderConfig config = providerConfig()
        mockServer.pendingThenSuccess('/token', 2, '''{
            "access_token": "device-access-token",
            "refresh_token": "device-refresh-token",
            "token_type": "Bearer",
            "expires_in": 3600
        }''')

        DeviceCodeFlow.DeviceCodeResponse deviceCode = new DeviceCodeFlow.DeviceCodeResponse(
                'DEVICE_CODE_XYZ', 'WXYZ-5678', 'https://example.com/activate',
                1, 30  // 1-second interval for fast test, 30s expiry
        )

        when:
        OAuthFlowResult result = flow.pollForToken(config, deviceCode)

        then:
        result.accessToken() == 'device-access-token'
        result.refreshToken() == 'device-refresh-token'
        result.clientId() == 'device-client-id'
        result.expiresAt() > System.currentTimeMillis()

        and: 'there were 3 poll requests (2 pending + 1 success)'
        mockServer.getRequests('/token').size() == 3
    }

    def "pollForToken handles slow_down by increasing interval"() {
        given:
        OAuthProviderConfig config = providerConfig()
        // First call: pending, second call: slow_down, third call: pending, fourth call: success
        mockServer.pendingThenSuccess('/token', 3, '''{
            "access_token": "slow-access-token",
            "expires_in": 1800
        }''', 1) // slow_down at index 1

        DeviceCodeFlow.DeviceCodeResponse deviceCode = new DeviceCodeFlow.DeviceCodeResponse(
                'DEVICE_SLOW', 'SLOW-1234', 'https://example.com/activate',
                1, 60  // 1-second interval, 60s expiry
        )

        when:
        OAuthFlowResult result = flow.pollForToken(config, deviceCode)

        then:
        result.accessToken() == 'slow-access-token'

        and: 'there were 4 poll requests'
        mockServer.getRequests('/token').size() == 4
    }

    def "pollForToken throws on access_denied"() {
        given:
        OAuthProviderConfig config = providerConfig()
        mockServer.errorEndpoint('/token', 400, 'access_denied', 'User denied authorization')

        DeviceCodeFlow.DeviceCodeResponse deviceCode = new DeviceCodeFlow.DeviceCodeResponse(
                'DEVICE_DENIED', 'DENY-1234', 'https://example.com/activate',
                1, 30
        )

        when:
        flow.pollForToken(config, deviceCode)

        then:
        OAuthFlowException ex = thrown(OAuthFlowException)
        ex.message.contains('denied')
    }

    def "requestDeviceCode throws when no device code URL configured"() {
        given:
        OAuthProviderConfig config = new OAuthProviderConfig(
                'no-device-url',
                null, "${mockServer.baseUrl()}/token", null,
                null,  // no device code URL
                'client-id', null, null, 0, null, [], OAuthFlowType.DEVICE_CODE
        )

        when:
        flow.requestDeviceCode(config)

        then:
        OAuthFlowException ex = thrown(OAuthFlowException)
        ex.message.contains('device code URL')
    }

    def "requestDeviceCode throws on server error"() {
        given:
        OAuthProviderConfig config = providerConfig()
        mockServer.errorEndpoint('/device/code', 500, 'server_error', 'Internal error')

        when:
        flow.requestDeviceCode(config)

        then:
        OAuthFlowException ex = thrown(OAuthFlowException)
        ex.message.contains('500')
    }

    def "pollForToken sends correct grant_type and device_code"() {
        given:
        OAuthProviderConfig config = providerConfig()
        mockServer.tokenEndpoint('/token', '''{
            "access_token": "immediate-token",
            "expires_in": 3600
        }''')

        DeviceCodeFlow.DeviceCodeResponse deviceCode = new DeviceCodeFlow.DeviceCodeResponse(
                'CHECK_PARAMS_CODE', 'CODE-9999', 'https://example.com/activate',
                1, 30
        )

        when:
        flow.pollForToken(config, deviceCode)

        then:
        List<MockOAuthServer.RecordedRequest> requests = mockServer.getRequests('/token')
        requests.size() == 1
        Map<String, String> params = requests[0].formParams()
        params['grant_type'] == 'urn:ietf:params:oauth:grant-type:device_code'
        params['device_code'] == 'CHECK_PARAMS_CODE'
        params['client_id'] == 'device-client-id'
    }
}
