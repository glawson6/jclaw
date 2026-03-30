package io.jaiclaw.identity.auth

import io.jaiclaw.core.auth.*
import spock.lang.Specification

class AuthProfileStoreSerializerSpec extends Specification {

    def "round-trip serialize and deserialize empty store"() {
        given:
        AuthProfileStore store = AuthProfileStore.empty()

        when:
        byte[] json = AuthProfileStoreSerializer.serialize(store)
        AuthProfileStore result = AuthProfileStoreSerializer.deserialize(json)

        then:
        result.version() == AuthProfileStore.CURRENT_VERSION
        result.profiles().isEmpty()
        result.order().isEmpty()
    }

    def "deserialize with ApiKeyCredential"() {
        given:
        String json = '''{
            "version": 1,
            "profiles": {
                "openai-main": {
                    "type": "api_key",
                    "provider": "openai",
                    "key": "sk-test123",
                    "email": "dev@example.com"
                }
            },
            "order": {},
            "lastGood": {},
            "usageStats": {}
        }'''

        when:
        AuthProfileStore result = AuthProfileStoreSerializer.deserialize(json)

        then:
        result.profiles().size() == 1
        AuthProfileCredential cred = result.profiles().get("openai-main")
        cred instanceof ApiKeyCredential
        ((ApiKeyCredential) cred).provider() == "openai"
        ((ApiKeyCredential) cred).key() == "sk-test123"
        ((ApiKeyCredential) cred).email() == "dev@example.com"
    }

    def "deserialize with OAuthCredential"() {
        given:
        long expires = System.currentTimeMillis() + 3600_000
        String json = """{
            "version": 1,
            "profiles": {
                "chutes-oauth": {
                    "type": "oauth",
                    "provider": "chutes",
                    "access": "acc-tok",
                    "refresh": "ref-tok",
                    "expires": ${expires},
                    "email": "user@chutes.ai",
                    "clientId": "client-123"
                }
            },
            "order": {},
            "lastGood": {},
            "usageStats": {}
        }"""

        when:
        AuthProfileStore result = AuthProfileStoreSerializer.deserialize(json)

        then:
        result.profiles().size() == 1
        AuthProfileCredential cred = result.profiles().get("chutes-oauth")
        cred instanceof OAuthCredential
        ((OAuthCredential) cred).provider() == "chutes"
        ((OAuthCredential) cred).access() == "acc-tok"
        ((OAuthCredential) cred).refresh() == "ref-tok"
        ((OAuthCredential) cred).expires() == expires
    }

    def "deserialize with TokenCredential"() {
        given:
        long expires = System.currentTimeMillis() + 7200_000
        String json = """{
            "version": 1,
            "profiles": {
                "anthropic-tok": {
                    "type": "token",
                    "provider": "anthropic",
                    "token": "tok-abc",
                    "expires": ${expires},
                    "email": "test@anthropic.com"
                }
            },
            "order": {},
            "lastGood": {},
            "usageStats": {}
        }"""

        when:
        AuthProfileStore result = AuthProfileStoreSerializer.deserialize(json)

        then:
        result.profiles().size() == 1
        AuthProfileCredential cred = result.profiles().get("anthropic-tok")
        cred instanceof TokenCredential
        ((TokenCredential) cred).token() == "tok-abc"
        ((TokenCredential) cred).expires() == expires
    }

    def "deserialize with rotation order"() {
        given:
        String json = '''{
            "version": 1,
            "profiles": {
                "openai-1": { "type": "api_key", "provider": "openai", "key": "sk-1" },
                "openai-2": { "type": "api_key", "provider": "openai", "key": "sk-2" }
            },
            "order": { "openai": ["openai-1", "openai-2"] },
            "lastGood": {},
            "usageStats": {}
        }'''

        when:
        AuthProfileStore result = AuthProfileStoreSerializer.deserialize(json)

        then:
        result.order().get("openai") == ["openai-1", "openai-2"]
    }

    def "coerceEnvRef converts dollar-brace syntax"() {
        when:
        SecretRef ref = AuthProfileStoreSerializer.coerceEnvRef('${OPENAI_API_KEY}')

        then:
        ref != null
        ref.source() == SecretRefSource.ENV
        ref.provider() == SecretRef.DEFAULT_PROVIDER
        ref.id() == "OPENAI_API_KEY"
    }

    def "coerceEnvRef returns null for non-env-ref strings"() {
        expect:
        AuthProfileStoreSerializer.coerceEnvRef("sk-plain-key") == null
        AuthProfileStoreSerializer.coerceEnvRef("") == null
        AuthProfileStoreSerializer.coerceEnvRef(null) == null
    }

    def "serialize strips inline key when keyRef is present"() {
        given:
        SecretRef ref = SecretRef.env("MY_KEY")
        AuthProfileStore store = AuthProfileStore.empty()
                .withProfile("p1", new ApiKeyCredential("openai", "sk-inline", ref, "e@e.com", Map.of()))

        when:
        String json = AuthProfileStoreSerializer.serializeToString(store)

        then:
        !json.contains("sk-inline")
        json.contains("MY_KEY")
    }

    def "deserialize handles OpenClaw mode alias for type"() {
        given:
        String json = '''{
            "version": 1,
            "profiles": {
                "openai-main": {
                    "mode": "api_key",
                    "provider": "openai",
                    "key": "sk-test"
                }
            },
            "order": {},
            "lastGood": {},
            "usageStats": {}
        }'''

        when:
        AuthProfileStore store = AuthProfileStoreSerializer.deserialize(json)

        then:
        store.profiles().get("openai-main") instanceof ApiKeyCredential
        ((ApiKeyCredential) store.profiles().get("openai-main")).key() == "sk-test"
    }

    def "deserialize handles OpenClaw apiKey alias for key"() {
        given:
        String json = '''{
            "version": 1,
            "profiles": {
                "openai-main": {
                    "type": "api_key",
                    "provider": "openai",
                    "apiKey": "sk-from-alias"
                }
            },
            "order": {},
            "lastGood": {},
            "usageStats": {}
        }'''

        when:
        AuthProfileStore store = AuthProfileStoreSerializer.deserialize(json)

        then:
        ((ApiKeyCredential) store.profiles().get("openai-main")).key() == "sk-from-alias"
    }

    def "deserialize coerces inline dollar-brace to SecretRef"() {
        given:
        String json = '''{
            "version": 1,
            "profiles": {
                "openai-main": {
                    "type": "api_key",
                    "provider": "openai",
                    "key": "${OPENAI_API_KEY}"
                }
            },
            "order": {},
            "lastGood": {},
            "usageStats": {}
        }'''

        when:
        AuthProfileStore store = AuthProfileStoreSerializer.deserialize(json)

        then:
        ApiKeyCredential cred = (ApiKeyCredential) store.profiles().get("openai-main")
        cred.keyRef() != null
        cred.keyRef().source() == SecretRefSource.ENV
        cred.keyRef().id() == "OPENAI_API_KEY"
    }
}
