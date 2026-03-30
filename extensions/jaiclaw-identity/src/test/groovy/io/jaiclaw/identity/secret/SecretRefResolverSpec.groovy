package io.jaiclaw.identity.secret

import io.jaiclaw.core.auth.SecretRef
import io.jaiclaw.core.auth.SecretRefSource
import spock.lang.Specification

class SecretRefResolverSpec extends Specification {

    def "resolve ENV ref reads environment variable"() {
        given:
        // Use a well-known env var that always exists
        SecretRefResolver resolver = new SecretRefResolver()
        SecretRef ref = SecretRef.env("PATH")

        when:
        String value = resolver.resolve(ref)

        then:
        value != null
        !value.isEmpty()
    }

    def "resolve ENV ref throws for missing variable"() {
        given:
        SecretRefResolver resolver = new SecretRefResolver()
        SecretRef ref = SecretRef.env("JAICLAW_NONEXISTENT_VAR_12345")

        when:
        resolver.resolve(ref)

        then:
        thrown(SecretRefResolver.SecretResolutionException)
    }

    def "resolveInlineOrRef returns inline value when present and no ref"() {
        given:
        SecretRefResolver resolver = new SecretRefResolver()

        expect:
        resolver.resolveInlineOrRef("my-api-key", null) == "my-api-key"
    }

    def "resolveInlineOrRef prefers ref over inline"() {
        given:
        SecretRefResolver resolver = new SecretRefResolver()
        SecretRef ref = SecretRef.env("PATH") // always exists

        when:
        String result = resolver.resolveInlineOrRef("ignored-inline", ref)

        then:
        result != null
        result != "ignored-inline"
    }

    def "resolveInlineOrRef coerces dollar-brace inline to ENV ref"() {
        given:
        SecretRefResolver resolver = new SecretRefResolver()

        when:
        String result = resolver.resolveInlineOrRef('${PATH}', null)

        then:
        result != null
        !result.isEmpty()
        result != '${PATH}'
    }

    def "resolveInlineOrRef throws when both inline and ref are null"() {
        given:
        SecretRefResolver resolver = new SecretRefResolver()

        when:
        resolver.resolveInlineOrRef(null, null)

        then:
        thrown(SecretRefResolver.SecretResolutionException)
    }
}
