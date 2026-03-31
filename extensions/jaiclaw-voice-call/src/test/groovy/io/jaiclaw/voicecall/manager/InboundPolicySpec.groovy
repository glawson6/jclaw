package io.jaiclaw.voicecall.manager

import io.jaiclaw.voicecall.config.VoiceCallProperties
import spock.lang.Specification

class InboundPolicySpec extends Specification {

    def "disabled policy rejects all"() {
        given:
        def inbound = new VoiceCallProperties.InboundProperties("disabled", null, null, [])
        def policy = new InboundPolicy(inbound)

        expect:
        !policy.shouldAcceptInbound("+15551234567")
    }

    def "open policy accepts all"() {
        given:
        def inbound = new VoiceCallProperties.InboundProperties("open", null, null, [])
        def policy = new InboundPolicy(inbound)

        expect:
        policy.shouldAcceptInbound("+15551234567")
        policy.shouldAcceptInbound("+447700900000")
    }

    def "allowlist policy accepts listed numbers"() {
        given:
        def inbound = new VoiceCallProperties.InboundProperties(
                "allowlist", null, null, ["+15551234567", "+15559876543"])
        def policy = new InboundPolicy(inbound)

        expect:
        policy.shouldAcceptInbound("+15551234567")
        policy.shouldAcceptInbound("+15559876543")
        !policy.shouldAcceptInbound("+15550000000")
    }

    def "phone number normalization strips formatting"() {
        given:
        def inbound = new VoiceCallProperties.InboundProperties(
                "allowlist", null, null, ["+1 (555) 123-4567"])
        def policy = new InboundPolicy(inbound)

        expect:
        policy.shouldAcceptInbound("+15551234567")
        policy.shouldAcceptInbound("+1 555 123 4567")
        policy.shouldAcceptInbound("+1-555-123-4567")
    }

    def "null config rejects all"() {
        given:
        def policy = new InboundPolicy(null)

        expect:
        !policy.shouldAcceptInbound("+15551234567")
    }
}
