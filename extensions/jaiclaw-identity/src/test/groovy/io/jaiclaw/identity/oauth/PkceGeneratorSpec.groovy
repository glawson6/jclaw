package io.jaiclaw.identity.oauth

import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class PkceGeneratorSpec extends Specification {

    def "generate produces valid PKCE challenge pair"() {
        when:
        PkceGenerator.PkceChallenge challenge = PkceGenerator.generate()

        then:
        challenge.verifier() != null
        challenge.challenge() != null
        challenge.verifier().length() >= 43 // RFC 7636 minimum
    }

    def "challenge is S256 of verifier"() {
        given:
        PkceGenerator.PkceChallenge pkce = PkceGenerator.generate()

        when:
        // Manually compute S256 challenge from verifier
        MessageDigest digest = MessageDigest.getInstance("SHA-256")
        byte[] hash = digest.digest(pkce.verifier().getBytes(StandardCharsets.US_ASCII))
        String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)

        then:
        pkce.challenge() == expected
    }

    def "each call generates unique verifier"() {
        when:
        PkceGenerator.PkceChallenge c1 = PkceGenerator.generate()
        PkceGenerator.PkceChallenge c2 = PkceGenerator.generate()

        then:
        c1.verifier() != c2.verifier()
        c1.challenge() != c2.challenge()
    }

    def "generateState produces 32-char hex string"() {
        when:
        String state = PkceGenerator.generateState()

        then:
        state != null
        state.length() == 32
        state.matches('[a-f0-9]{32}')
    }

    def "each state call generates unique value"() {
        when:
        String s1 = PkceGenerator.generateState()
        String s2 = PkceGenerator.generateState()

        then:
        s1 != s2
    }
}
