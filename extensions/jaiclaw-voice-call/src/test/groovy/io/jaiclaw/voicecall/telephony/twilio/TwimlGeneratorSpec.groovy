package io.jaiclaw.voicecall.telephony.twilio

import spock.lang.Specification

class TwimlGeneratorSpec extends Specification {

    def "notifySay generates valid TwiML with say and hangup"() {
        when:
        def twiml = TwimlGenerator.notifySay("Hello world", "Polly.Amy")

        then:
        twiml.contains('<?xml version="1.0"')
        twiml.contains('<Response>')
        twiml.contains('<Say voice="Polly.Amy">Hello world</Say>')
        twiml.contains('<Hangup/>')
        twiml.contains('</Response>')
    }

    def "notifySay uses default voice when null"() {
        when:
        def twiml = TwimlGenerator.notifySay("Test", null)

        then:
        twiml.contains('voice="Polly.Amy"')
    }

    def "notifySay escapes XML special characters"() {
        when:
        def twiml = TwimlGenerator.notifySay('Hello <world> & "friends"', "Polly.Amy")

        then:
        twiml.contains("Hello &lt;world&gt; &amp; &quot;friends&quot;")
    }

    def "connectStream generates valid TwiML with stream parameters"() {
        when:
        def twiml = TwimlGenerator.connectStream("wss://example.com/stream", "call-123", "token-abc")

        then:
        twiml.contains('<Connect>')
        twiml.contains('<Stream url="wss://example.com/stream">')
        twiml.contains('<Parameter name="callId" value="call-123"/>')
        twiml.contains('<Parameter name="authToken" value="token-abc"/>')
        twiml.contains('</Stream>')
        twiml.contains('</Connect>')
    }

    def "gatherSpeech generates valid TwiML with speech input"() {
        when:
        def twiml = TwimlGenerator.gatherSpeech("https://example.com/webhook", "en-US", 30, "turn-1")

        then:
        twiml.contains('<Gather input="speech"')
        twiml.contains('language="en-US"')
        twiml.contains('timeout="30"')
        twiml.contains('speechTimeout="auto"')
        twiml.contains('turnToken=turn-1')
        twiml.contains('<Pause length="120"/>')
    }

    def "gatherSpeech uses default language when null"() {
        when:
        def twiml = TwimlGenerator.gatherSpeech("https://example.com/webhook", null, 10, null)

        then:
        twiml.contains('language="en-US"')
        !twiml.contains('turnToken')
    }

    def "pause generates correct TwiML"() {
        when:
        def twiml = TwimlGenerator.pause(60)

        then:
        twiml.contains('<Pause length="60"/>')
    }

    def "empty generates minimal TwiML"() {
        when:
        def twiml = TwimlGenerator.empty()

        then:
        twiml.contains('<Response/>')
    }

    def "hangup generates hangup TwiML"() {
        when:
        def twiml = TwimlGenerator.hangup()

        then:
        twiml.contains('<Hangup/>')
    }

    def "escapeXml handles null"() {
        expect:
        TwimlGenerator.escapeXml(null) == ""
    }
}
