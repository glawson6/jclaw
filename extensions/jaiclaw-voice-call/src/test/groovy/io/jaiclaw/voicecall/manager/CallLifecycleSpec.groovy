package io.jaiclaw.voicecall.manager

import io.jaiclaw.voicecall.model.*
import spock.lang.Specification
import spock.lang.Unroll

class CallLifecycleSpec extends Specification {

    def "forward state progression: INITIATED -> RINGING -> ANSWERED -> ACTIVE -> SPEAKING"() {
        given:
        def call = new CallRecord("c1", "twilio", CallDirection.OUTBOUND,
                "+1555", "+1666", CallMode.CONVERSATION)

        expect:
        call.state == CallState.INITIATED

        when:
        CallLifecycle.transitionState(call, CallState.RINGING)
        then:
        call.state == CallState.RINGING

        when:
        CallLifecycle.transitionState(call, CallState.ANSWERED)
        then:
        call.state == CallState.ANSWERED

        when:
        CallLifecycle.transitionState(call, CallState.ACTIVE)
        then:
        call.state == CallState.ACTIVE

        when:
        CallLifecycle.transitionState(call, CallState.SPEAKING)
        then:
        call.state == CallState.SPEAKING
    }

    def "conversation cycling: SPEAKING <-> LISTENING"() {
        given:
        def call = new CallRecord("c1", "twilio", CallDirection.OUTBOUND,
                "+1555", "+1666", CallMode.CONVERSATION)
        call.state = CallState.SPEAKING

        when:
        def result1 = CallLifecycle.transitionState(call, CallState.LISTENING)
        then:
        result1
        call.state == CallState.LISTENING

        when:
        def result2 = CallLifecycle.transitionState(call, CallState.SPEAKING)
        then:
        result2
        call.state == CallState.SPEAKING
    }

    def "terminal states always reachable from non-terminal"() {
        given:
        def call = new CallRecord("c1", "twilio", CallDirection.OUTBOUND,
                "+1555", "+1666", CallMode.CONVERSATION)
        call.state = CallState.ACTIVE

        when:
        def result = CallLifecycle.transitionState(call, CallState.HANGUP_USER)

        then:
        result
        call.state == CallState.HANGUP_USER
    }

    def "no-op for same state"() {
        given:
        def call = new CallRecord("c1", "twilio", CallDirection.OUTBOUND,
                "+1555", "+1666", CallMode.CONVERSATION)
        call.state = CallState.ACTIVE

        when:
        def result = CallLifecycle.transitionState(call, CallState.ACTIVE)

        then:
        !result
        call.state == CallState.ACTIVE
    }

    def "no transitions from terminal states"() {
        given:
        def call = new CallRecord("c1", "twilio", CallDirection.OUTBOUND,
                "+1555", "+1666", CallMode.CONVERSATION)
        call.state = CallState.COMPLETED

        when:
        def result = CallLifecycle.transitionState(call, CallState.ACTIVE)

        then:
        !result
        call.state == CallState.COMPLETED
    }

    @Unroll
    def "invalid transition: #from -> #to"() {
        expect:
        !CallLifecycle.isValidTransition(from, to)

        where:
        from               | to
        CallState.INITIATED | CallState.SPEAKING
        CallState.RINGING   | CallState.LISTENING
        CallState.ACTIVE    | CallState.RINGING
        CallState.SPEAKING  | CallState.ANSWERED
    }
}
