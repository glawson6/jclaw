package io.jaiclaw.core.auth

import spock.lang.Specification

class SecretRefSpec extends Specification {

    def "env factory creates ENV ref with default provider"() {
        given:
        SecretRef ref = SecretRef.env("OPENAI_API_KEY")

        expect:
        ref.source() == SecretRefSource.ENV
        ref.provider() == SecretRef.DEFAULT_PROVIDER
        ref.id() == "OPENAI_API_KEY"
    }

    def "full constructor with FILE source"() {
        given:
        SecretRef ref = new SecretRef(SecretRefSource.FILE, "vault", "/secrets/api-key")

        expect:
        ref.source() == SecretRefSource.FILE
        ref.provider() == "vault"
        ref.id() == "/secrets/api-key"
    }

    def "full constructor with EXEC source"() {
        given:
        SecretRef ref = new SecretRef(SecretRefSource.EXEC, "1password", "openai-key")

        expect:
        ref.source() == SecretRefSource.EXEC
        ref.provider() == "1password"
        ref.id() == "openai-key"
    }

    def "DEFAULT_PROVIDER is 'default'"() {
        expect:
        SecretRef.DEFAULT_PROVIDER == "default"
    }

    def "SecretRefSource has three values"() {
        expect:
        SecretRefSource.values()*.name().sort() == ["ENV", "EXEC", "FILE"]
    }
}
