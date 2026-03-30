package io.jaiclaw.identity.auth

import io.jaiclaw.core.auth.OAuthCredential
import spock.lang.Specification

class ProviderTokenRefresherRegistrySpec extends Specification {

    def "empty registry has no refreshers"() {
        given:
        ProviderTokenRefresherRegistry registry = new ProviderTokenRefresherRegistry()

        expect:
        registry.registeredProviders().isEmpty()
        !registry.hasRefresher("openai")
        registry.get("openai").isEmpty()
    }

    def "register and retrieve a refresher"() {
        given:
        ProviderTokenRefresherRegistry registry = new ProviderTokenRefresherRegistry()
        TokenRefresher refresher = Mock(TokenRefresher) {
            providerId() >> "chutes"
        }

        when:
        registry.register(refresher)

        then:
        registry.hasRefresher("chutes")
        registry.get("chutes").isPresent()
        registry.get("chutes").get() == refresher
        registry.registeredProviders() == Set.of("chutes")
    }

    def "lookup is case-insensitive"() {
        given:
        ProviderTokenRefresherRegistry registry = new ProviderTokenRefresherRegistry()
        TokenRefresher refresher = Mock(TokenRefresher) {
            providerId() >> "OpenAI"
        }
        registry.register(refresher)

        expect:
        registry.hasRefresher("openai")
        registry.hasRefresher("OPENAI")
        registry.get("openai").isPresent()
    }

    def "constructor accepts initial list"() {
        given:
        TokenRefresher r1 = Mock(TokenRefresher) { providerId() >> "provider1" }
        TokenRefresher r2 = Mock(TokenRefresher) { providerId() >> "provider2" }
        ProviderTokenRefresherRegistry registry = new ProviderTokenRefresherRegistry(List.of(r1, r2))

        expect:
        registry.registeredProviders().size() == 2
        registry.hasRefresher("provider1")
        registry.hasRefresher("provider2")
    }
}
