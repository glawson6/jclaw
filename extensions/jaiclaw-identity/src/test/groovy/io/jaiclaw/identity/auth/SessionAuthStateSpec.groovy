package io.jaiclaw.identity.auth

import spock.lang.Specification

class SessionAuthStateSpec extends Specification {

    def "empty state has no override"() {
        given:
        SessionAuthState state = SessionAuthState.empty()

        expect:
        state.authProfileOverride() == null
        state.overrideSource() == null
        state.compactionCount() == null
    }

    def "withUserOverride sets user source"() {
        given:
        SessionAuthState state = SessionAuthState.empty()

        when:
        SessionAuthState pinned = state.withUserOverride("openai-primary")

        then:
        pinned.authProfileOverride() == "openai-primary"
        pinned.overrideSource() == SessionAuthState.SOURCE_USER
    }

    def "withAutoOverride sets auto source and compaction count"() {
        given:
        SessionAuthState state = SessionAuthState.empty()

        when:
        SessionAuthState auto = state.withAutoOverride("openai-2", 3)

        then:
        auto.authProfileOverride() == "openai-2"
        auto.overrideSource() == SessionAuthState.SOURCE_AUTO
        auto.compactionCount() == 3
    }

    def "cleared removes override"() {
        given:
        SessionAuthState state = SessionAuthState.empty().withUserOverride("profile-1")

        when:
        SessionAuthState cleared = state.cleared()

        then:
        cleared.authProfileOverride() == null
        cleared.overrideSource() == null
    }

    def "source constants are correct"() {
        expect:
        SessionAuthState.SOURCE_USER == "user"
        SessionAuthState.SOURCE_AUTO == "auto"
    }
}
