package io.jaiclaw.voicecall.telephony.twilio

import io.jaiclaw.voicecall.model.WebhookContext
import spock.lang.Specification

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets

class TwilioWebhookVerifierSpec extends Specification {

    def authToken = "test-auth-token-12345"
    def verifier = new TwilioWebhookVerifier(authToken)

    def "rejects missing signature header"() {
        given:
        def ctx = new WebhookContext([:], "Body=test", "https://example.com/webhook", "POST", [:], "127.0.0.1")

        when:
        def result = verifier.verify(ctx)

        then:
        !result.ok()
        result.reason().contains("Missing X-Twilio-Signature")
    }

    def "rejects when no auth token configured"() {
        given:
        def noTokenVerifier = new TwilioWebhookVerifier(null)
        def ctx = new WebhookContext(
                ["x-twilio-signature": "sig"],
                "Body=test",
                "https://example.com/webhook",
                "POST", [:], "127.0.0.1")

        when:
        def result = noTokenVerifier.verify(ctx)

        then:
        !result.ok()
        result.reason().contains("No auth token")
    }

    def "verifies valid signature"() {
        given:
        def url = "https://example.com/webhook"
        def body = "AccountSid=AC123&CallSid=CA456&CallStatus=ringing"

        // Compute expected signature
        def dataToSign = url + "AccountSidAC123" + "CallSidCA456" + "CallStatusringing"
        def expectedSig = computeHmac(dataToSign, authToken)

        def ctx = new WebhookContext(
                ["x-twilio-signature": expectedSig],
                body, url, "POST", [:], "127.0.0.1")

        when:
        def result = verifier.verify(ctx)

        then:
        result.ok()
    }

    def "rejects invalid signature"() {
        given:
        def ctx = new WebhookContext(
                ["x-twilio-signature": "invalid-signature"],
                "Body=test",
                "https://example.com/webhook",
                "POST", [:], "127.0.0.1")

        when:
        def result = verifier.verify(ctx)

        then:
        !result.ok()
        result.reason().contains("Signature mismatch")
    }

    private String computeHmac(String data, String key) {
        def mac = Mac.getInstance("HmacSHA1")
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"))
        def rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8))
        return Base64.encoder.encodeToString(rawHmac)
    }
}
