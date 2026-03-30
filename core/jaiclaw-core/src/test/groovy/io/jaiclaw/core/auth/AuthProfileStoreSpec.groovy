package io.jaiclaw.core.auth

import spock.lang.Specification

class AuthProfileStoreSpec extends Specification {

    def "empty store has version 1 and empty collections"() {
        when:
        AuthProfileStore store = AuthProfileStore.empty()

        then:
        store.version() == AuthProfileStore.CURRENT_VERSION
        store.profiles().isEmpty()
        store.order().isEmpty()
        store.lastGood().isEmpty()
        store.usageStats().isEmpty()
    }

    def "withProfile adds a credential"() {
        given:
        AuthProfileStore store = AuthProfileStore.empty()
        ApiKeyCredential cred = new ApiKeyCredential("openai", "sk-abc", "test@example.com")

        when:
        AuthProfileStore updated = store.withProfile("openai-main", cred)

        then:
        updated.profiles().size() == 1
        updated.profiles().get("openai-main") == cred
        store.profiles().isEmpty() // original unchanged
    }

    def "withProfile replaces existing credential"() {
        given:
        ApiKeyCredential cred1 = new ApiKeyCredential("openai", "sk-old", "old@example.com")
        ApiKeyCredential cred2 = new ApiKeyCredential("openai", "sk-new", "new@example.com")
        AuthProfileStore store = AuthProfileStore.empty().withProfile("openai-main", cred1)

        when:
        AuthProfileStore updated = store.withProfile("openai-main", cred2)

        then:
        updated.profiles().get("openai-main") == cred2
    }

    def "withoutProfile removes a credential"() {
        given:
        ApiKeyCredential cred = new ApiKeyCredential("openai", "sk-abc", "test@example.com")
        AuthProfileStore store = AuthProfileStore.empty().withProfile("openai-main", cred)

        when:
        AuthProfileStore updated = store.withoutProfile("openai-main")

        then:
        updated.profiles().isEmpty()
    }

    def "withOrder sets rotation order for a provider"() {
        given:
        AuthProfileStore store = AuthProfileStore.empty()

        when:
        AuthProfileStore updated = store.withOrder("openai", List.of("openai-1", "openai-2", "openai-3"))

        then:
        updated.order().get("openai") == ["openai-1", "openai-2", "openai-3"]
    }

    def "withLastGood tracks last known good profile"() {
        given:
        AuthProfileStore store = AuthProfileStore.empty()

        when:
        AuthProfileStore updated = store.withLastGood("anthropic", "anthropic-primary")

        then:
        updated.lastGood().get("anthropic") == "anthropic-primary"
    }

    def "withUsageStats tracks profile usage"() {
        given:
        AuthProfileStore store = AuthProfileStore.empty()
        ProfileUsageStats stats = ProfileUsageStats.empty()

        when:
        AuthProfileStore updated = store.withUsageStats("profile-1", stats)

        then:
        updated.usageStats().get("profile-1") == stats
    }

    def "profiles map is immutable"() {
        given:
        AuthProfileStore store = AuthProfileStore.empty()
                .withProfile("p1", new ApiKeyCredential("openai", "key", "e@e.com"))

        when:
        store.profiles().put("p2", new ApiKeyCredential("openai", "key2", "e@e.com"))

        then:
        thrown(UnsupportedOperationException)
    }
}
