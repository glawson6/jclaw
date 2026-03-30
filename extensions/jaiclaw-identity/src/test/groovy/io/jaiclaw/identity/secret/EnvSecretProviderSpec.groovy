package io.jaiclaw.identity.secret

import io.jaiclaw.core.auth.SecretRefSource
import spock.lang.Specification

class EnvSecretProviderSpec extends Specification {

    EnvSecretProvider provider = new EnvSecretProvider()

    def "resolves existing environment variable"() {
        given:
        SecretProviderConfig config = SecretProviderConfig.defaultEnv()

        when:
        String value = provider.resolve("PATH", config)

        then:
        value != null
        !value.isEmpty()
    }

    def "throws for nonexistent environment variable"() {
        given:
        SecretProviderConfig config = SecretProviderConfig.defaultEnv()

        when:
        provider.resolve("JAICLAW_NONEXISTENT_ABC_999", config)

        then:
        thrown(SecretRefResolver.SecretResolutionException)
    }

    def "rejects invalid env var names"() {
        given:
        SecretProviderConfig config = SecretProviderConfig.defaultEnv()

        when:
        provider.resolve(invalidName, config)

        then:
        thrown(SecretRefResolver.SecretResolutionException)

        where:
        invalidName << ["lowercase", "has space", "1STARTS_WITH_NUMBER", "", "A".repeat(200)]
    }

    def "respects allowlist when configured"() {
        given:
        SecretProviderConfig config = new SecretProviderConfig(
                SecretRefSource.ENV, "restricted", List.of("ALLOWED_VAR"),
                null, null, false,
                SecretProviderConfig.DEFAULT_TIMEOUT_MS, SecretProviderConfig.DEFAULT_MAX_BYTES,
                null, null, false, Map.of(), List.of())

        when:
        provider.resolve("PATH", config) // PATH exists but is not in allowlist

        then:
        thrown(SecretRefResolver.SecretResolutionException)
    }
}
