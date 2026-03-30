package io.jaiclaw.identity.auth

import io.jaiclaw.core.auth.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class SessionAuthProfileResolverSpec extends Specification {

    @TempDir
    Path tempDir

    AuthProfileStoreManager storeManager
    SessionAuthProfileResolver resolver

    def setup() {
        storeManager = new AuthProfileStoreManager(tempDir)
        resolver = new SessionAuthProfileResolver(storeManager)
    }

    private Path mainAgentDir() {
        Path dir = storeManager.resolveMainAgentDir()
        Files.createDirectories(dir)
        return dir
    }

    def "returns empty when no rotation order for provider"() {
        given:
        Path agentDir = mainAgentDir()
        storeManager.upsertProfile(agentDir, "openai-main",
                new ApiKeyCredential("openai", "sk-test", "e@e.com"))
        SessionAuthState state = SessionAuthState.empty()

        expect:
        resolver.resolve("openai", agentDir, state, true, null).isEmpty()
    }

    def "returns user-pinned override when profile exists in order"() {
        given:
        Path agentDir = mainAgentDir()
        storeManager.upsertProfile(agentDir, "openai-main",
                new ApiKeyCredential("openai", "sk-test", "e@e.com"))
        storeManager.setProfileOrder(agentDir, "openai", List.of("openai-main"))
        SessionAuthState state = SessionAuthState.empty().withUserOverride("openai-main")

        when:
        Optional<String> result = resolver.resolve("openai", agentDir, state, false, null)

        then:
        result.isPresent()
        result.get() == "openai-main"
    }

    def "resolves from rotation order on new session"() {
        given:
        Path agentDir = mainAgentDir()
        storeManager.upsertProfile(agentDir, "openai-1",
                new ApiKeyCredential("openai", "sk-1", "a@a.com"))
        storeManager.upsertProfile(agentDir, "openai-2",
                new ApiKeyCredential("openai", "sk-2", "b@b.com"))
        storeManager.setProfileOrder(agentDir, "openai", List.of("openai-1", "openai-2"))
        SessionAuthState state = SessionAuthState.empty()

        when:
        Optional<String> result = resolver.resolve("openai", agentDir, state, true, null)

        then:
        result.isPresent()
        result.get() in ["openai-1", "openai-2"]
    }

    def "skips profiles in cooldown"() {
        given:
        Path agentDir = mainAgentDir()
        long future = System.currentTimeMillis() + 300_000
        ProfileUsageStats cooldownStats = new ProfileUsageStats(null, future, null, null, 1, Map.of(), null)

        AuthProfileStore store = AuthProfileStore.empty()
                .withProfile("openai-1", new ApiKeyCredential("openai", "sk-1", "a@a.com"))
                .withProfile("openai-2", new ApiKeyCredential("openai", "sk-2", "b@b.com"))
                .withOrder("openai", List.of("openai-1", "openai-2"))
                .withUsageStats("openai-1", cooldownStats)
        storeManager.save(agentDir, store)

        SessionAuthState state = SessionAuthState.empty()

        when:
        Optional<String> result = resolver.resolve("openai", agentDir, state, true, null)

        then:
        result.isPresent()
        result.get() == "openai-2"
    }
}
