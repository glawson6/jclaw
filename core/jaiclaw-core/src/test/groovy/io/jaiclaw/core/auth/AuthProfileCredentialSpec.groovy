package io.jaiclaw.core.auth

import spock.lang.Specification

class AuthProfileCredentialSpec extends Specification {

    def "ApiKeyCredential full constructor"() {
        given:
        SecretRef ref = SecretRef.env("OPENAI_KEY")
        ApiKeyCredential cred = new ApiKeyCredential("openai", null, ref, "test@example.com", Map.of("org", "myorg"))

        expect:
        cred.provider() == "openai"
        cred.key() == null
        cred.keyRef() == ref
        cred.email() == "test@example.com"
        cred.metadata().get("org") == "myorg"
    }

    def "ApiKeyCredential convenience constructor"() {
        given:
        ApiKeyCredential cred = new ApiKeyCredential("openai", "sk-abc123", "user@example.com")

        expect:
        cred.provider() == "openai"
        cred.key() == "sk-abc123"
        cred.keyRef() == null
        cred.email() == "user@example.com"
        cred.metadata().isEmpty()
    }

    def "TokenCredential stores expiry"() {
        given:
        long expires = System.currentTimeMillis() + 3600_000
        TokenCredential cred = new TokenCredential("anthropic", "tok-xyz", expires, "dev@example.com")

        expect:
        cred.provider() == "anthropic"
        cred.token() == "tok-xyz"
        cred.tokenRef() == null
        cred.expires() == expires
        cred.email() == "dev@example.com"
    }

    def "TokenCredential with SecretRef"() {
        given:
        SecretRef ref = new SecretRef(SecretRefSource.FILE, "vault", "/tokens/anthropic")
        TokenCredential cred = new TokenCredential("anthropic", null, ref, null, null)

        expect:
        cred.token() == null
        cred.tokenRef() == ref
        cred.expires() == null
    }

    def "OAuthCredential stores all fields"() {
        given:
        long expires = System.currentTimeMillis() + 3600_000
        OAuthCredential cred = new OAuthCredential("chutes", "access-tok", "refresh-tok", expires,
                "user@chutes.ai", "client-123", "acct-456", "proj-789", "https://api.chutes.ai")

        expect:
        cred.provider() == "chutes"
        cred.access() == "access-tok"
        cred.refresh() == "refresh-tok"
        cred.expires() == expires
        cred.email() == "user@chutes.ai"
        cred.clientId() == "client-123"
        cred.accountId() == "acct-456"
        cred.projectId() == "proj-789"
        cred.enterpriseUrl() == "https://api.chutes.ai"
    }

    def "OAuthCredential convenience constructor"() {
        given:
        OAuthCredential cred = new OAuthCredential("openai", "acc", "ref", 1000L, "e@e.com", "cid")

        expect:
        cred.accountId() == null
        cred.projectId() == null
        cred.enterpriseUrl() == null
    }

    def "OAuthCredential withRefreshedTokens returns updated copy"() {
        given:
        OAuthCredential cred = new OAuthCredential("openai", "old-acc", "old-ref", 1000L, "e@e.com", "cid")

        when:
        OAuthCredential refreshed = cred.withRefreshedTokens("new-acc", "new-ref", 2000L)

        then:
        refreshed.access() == "new-acc"
        refreshed.refresh() == "new-ref"
        refreshed.expires() == 2000L
        // unchanged fields
        refreshed.provider() == "openai"
        refreshed.email() == "e@e.com"
        refreshed.clientId() == "cid"
    }

    def "sealed interface permits only three implementations"() {
        expect:
        AuthProfileCredential.isSealed()
        AuthProfileCredential.getPermittedSubclasses()*.simpleName.sort() == ["ApiKeyCredential", "OAuthCredential", "TokenCredential"]
    }

    def "instanceof works on sealed hierarchy"() {
        given:
        AuthProfileCredential cred = new ApiKeyCredential("openai", "sk-key", "e@e.com")

        expect:
        cred instanceof ApiKeyCredential
        !(cred instanceof TokenCredential)
        !(cred instanceof OAuthCredential)
        ((ApiKeyCredential) cred).key() == "sk-key"
    }
}
