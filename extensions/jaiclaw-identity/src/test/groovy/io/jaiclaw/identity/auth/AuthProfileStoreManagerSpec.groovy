package io.jaiclaw.identity.auth

import io.jaiclaw.core.auth.*
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class AuthProfileStoreManagerSpec extends Specification {

    @TempDir
    Path tempDir

    AuthProfileStoreManager manager

    def setup() {
        manager = new AuthProfileStoreManager(tempDir)
    }

    private Path mainAgentDir() {
        Path dir = manager.resolveMainAgentDir()
        Files.createDirectories(dir)
        return dir
    }

    def "loadMainStore returns empty store when no file exists"() {
        when:
        AuthProfileStore store = manager.loadMainStore()

        then:
        store.profiles().isEmpty()
        store.version() == AuthProfileStore.CURRENT_VERSION
    }

    def "upsertProfile creates store file and adds credential"() {
        given:
        Path agentDir = mainAgentDir()
        ApiKeyCredential cred = new ApiKeyCredential("openai", "sk-test", "test@test.com")

        when:
        manager.upsertProfile(agentDir, "openai-main", cred)

        then:
        Files.exists(manager.storePath(agentDir))
        AuthProfileStore store = manager.loadForAgent(agentDir)
        store.profiles().size() == 1
        store.profiles().get("openai-main") instanceof ApiKeyCredential
    }

    def "upsertProfile replaces existing credential"() {
        given:
        Path agentDir = mainAgentDir()
        ApiKeyCredential cred1 = new ApiKeyCredential("openai", "sk-old", "old@test.com")
        ApiKeyCredential cred2 = new ApiKeyCredential("openai", "sk-new", "new@test.com")
        manager.upsertProfile(agentDir, "openai-main", cred1)

        when:
        manager.upsertProfile(agentDir, "openai-main", cred2)

        then:
        AuthProfileStore store = manager.loadForAgent(agentDir)
        ((ApiKeyCredential) store.profiles().get("openai-main")).key() == "sk-new"
    }

    def "removeProfile removes credential"() {
        given:
        Path agentDir = mainAgentDir()
        manager.upsertProfile(agentDir, "openai-main", new ApiKeyCredential("openai", "sk-test", "t@t.com"))

        when:
        manager.removeProfile(agentDir, "openai-main")

        then:
        manager.loadForAgent(agentDir).profiles().isEmpty()
    }

    def "setProfileOrder persists rotation order"() {
        given:
        Path agentDir = mainAgentDir()

        when:
        manager.setProfileOrder(agentDir, "openai", List.of("p1", "p2", "p3"))

        then:
        AuthProfileStore store = manager.loadForAgent(agentDir)
        store.order().get("openai") == ["p1", "p2", "p3"]
    }

    def "markProfileGood tracks last known good"() {
        given:
        Path agentDir = mainAgentDir()

        when:
        manager.markProfileGood(agentDir, "anthropic", "anthropic-primary")

        then:
        AuthProfileStore store = manager.loadForAgent(agentDir)
        store.lastGood().get("anthropic") == "anthropic-primary"
    }

    def "merge combines base and override stores"() {
        given:
        ApiKeyCredential baseCred = new ApiKeyCredential("openai", "sk-base", "base@test.com")
        ApiKeyCredential overrideCred = new ApiKeyCredential("openai", "sk-override", "override@test.com")
        AuthProfileStore base = AuthProfileStore.empty().withProfile("shared", baseCred)
        AuthProfileStore override = AuthProfileStore.empty().withProfile("shared", overrideCred)

        when:
        AuthProfileStore merged = manager.merge(base, override)

        then:
        ((ApiKeyCredential) merged.profiles().get("shared")).key() == "sk-override"
    }

    def "merge preserves non-overlapping profiles"() {
        given:
        AuthProfileStore base = AuthProfileStore.empty()
                .withProfile("p1", new ApiKeyCredential("openai", "sk-1", "a@a.com"))
        AuthProfileStore override = AuthProfileStore.empty()
                .withProfile("p2", new ApiKeyCredential("anthropic", "sk-2", "b@b.com"))

        when:
        AuthProfileStore merged = manager.merge(base, override)

        then:
        merged.profiles().size() == 2
        merged.profiles().containsKey("p1")
        merged.profiles().containsKey("p2")
    }

    def "loadForRuntime merges main and sub-agent stores"() {
        given:
        Path mainDir = mainAgentDir()
        Path subDir = tempDir.resolve("agents/sub-agent/agent")
        Files.createDirectories(subDir)

        manager.upsertProfile(mainDir, "main-profile", new ApiKeyCredential("openai", "sk-main", "main@t.com"))
        manager.upsertProfile(subDir, "sub-profile", new ApiKeyCredential("anthropic", "sk-sub", "sub@t.com"))

        when:
        AuthProfileStore runtime = manager.loadForRuntime(subDir)

        then:
        runtime.profiles().containsKey("main-profile")
        runtime.profiles().containsKey("sub-profile")
    }

    def "read-only mode silently skips save instead of throwing"() {
        given:
        AuthProfileStoreManager readOnlyManager = new AuthProfileStoreManager(tempDir, true)
        Path agentDir = mainAgentDir()

        when:
        readOnlyManager.save(agentDir, AuthProfileStore.empty().withProfile("test",
                new ApiKeyCredential("openai", "sk-x", "e@e.com")))

        then:
        // Read-only save is a no-op (no exception, no file written)
        noExceptionThrown()
        !Files.exists(readOnlyManager.storePath(agentDir))
    }

    def "storePath and lockPath resolve correctly"() {
        given:
        Path agentDir = tempDir.resolve("agents/default/agent")

        expect:
        manager.storePath(agentDir).fileName.toString() == AuthProfileConstants.AUTH_PROFILE_FILENAME
        manager.lockPath(agentDir).fileName.toString().endsWith(".lock")
    }

    def "concurrent upserts are safe with file locking"() {
        given:
        Path agentDir = mainAgentDir()
        int threadCount = 5

        when:
        List<Thread> threads = (1..threadCount).collect { int i ->
            Thread.startVirtualThread {
                manager.upsertProfile(agentDir, "profile-${i}",
                        new ApiKeyCredential("openai", "sk-${i}", "user${i}@test.com"))
            }
        }
        threads.each { it.join() }

        then:
        AuthProfileStore store = manager.loadForAgent(agentDir)
        store.profiles().size() == threadCount
    }
}
