package io.jaiclaw.voicecall.model

import spock.lang.Specification
import spock.lang.Unroll

class CallStateSpec extends Specification {

    @Unroll
    def "terminal states: #state.isTerminal() == #expected"() {
        expect:
        state.isTerminal() == expected

        where:
        state                    | expected
        CallState.INITIATED      | false
        CallState.RINGING        | false
        CallState.ANSWERED       | false
        CallState.ACTIVE         | false
        CallState.SPEAKING       | false
        CallState.LISTENING      | false
        CallState.COMPLETED      | true
        CallState.HANGUP_USER    | true
        CallState.HANGUP_BOT     | true
        CallState.TIMEOUT        | true
        CallState.ERROR          | true
        CallState.FAILED         | true
        CallState.NO_ANSWER      | true
        CallState.BUSY           | true
        CallState.VOICEMAIL      | true
    }

    def "all states are accounted for"() {
        given:
        def terminalStates = CallState.values().findAll { it.isTerminal() }
        def nonTerminalStates = CallState.values().findAll { !it.isTerminal() }

        expect:
        terminalStates.size() == 9
        nonTerminalStates.size() == 6
        terminalStates.size() + nonTerminalStates.size() == CallState.values().length
    }
}
