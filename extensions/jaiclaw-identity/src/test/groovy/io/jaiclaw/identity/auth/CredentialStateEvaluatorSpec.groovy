package io.jaiclaw.identity.auth

import io.jaiclaw.core.auth.*
import spock.lang.Specification

class CredentialStateEvaluatorSpec extends Specification {

    // --- resolveTokenExpiryState ---

    def "null expires returns MISSING"() {
        expect:
        CredentialStateEvaluator.resolveTokenExpiryState(null) == CredentialState.MISSING
    }

    def "zero or negative expires returns INVALID"() {
        expect:
        CredentialStateEvaluator.resolveTokenExpiryState(0L) == CredentialState.INVALID
        CredentialStateEvaluator.resolveTokenExpiryState(-1L) == CredentialState.INVALID
    }

    def "past expires returns EXPIRED"() {
        given:
        long past = System.currentTimeMillis() - 60_000

        expect:
        CredentialStateEvaluator.resolveTokenExpiryState(past) == CredentialState.EXPIRED
    }

    def "future expires returns VALID"() {
        given:
        long future = System.currentTimeMillis() + 3600_000

        expect:
        CredentialStateEvaluator.resolveTokenExpiryState(future) == CredentialState.VALID
    }

    // --- evaluateEligibility ---

    def "ApiKeyCredential with key is eligible"() {
        given:
        ApiKeyCredential cred = new ApiKeyCredential("openai", "sk-test", "e@e.com")

        expect:
        CredentialStateEvaluator.evaluateEligibility(cred).eligible()
    }

    def "ApiKeyCredential with keyRef is eligible"() {
        given:
        ApiKeyCredential cred = new ApiKeyCredential("openai", null, SecretRef.env("KEY"), "e@e.com", Map.of())

        expect:
        CredentialStateEvaluator.evaluateEligibility(cred).eligible()
    }

    def "ApiKeyCredential with no key and no keyRef is ineligible"() {
        given:
        ApiKeyCredential cred = new ApiKeyCredential("openai", null, null, "e@e.com", Map.of())

        when:
        CredentialEligibility eligibility = CredentialStateEvaluator.evaluateEligibility(cred)

        then:
        !eligibility.eligible()
        eligibility.reasonCode() == CredentialEligibility.MISSING_CREDENTIAL
    }

    def "TokenCredential with valid token is eligible"() {
        given:
        long future = System.currentTimeMillis() + 3600_000
        TokenCredential cred = new TokenCredential("anthropic", "tok-abc", future, "e@e.com")

        expect:
        CredentialStateEvaluator.evaluateEligibility(cred).eligible()
    }

    def "TokenCredential with expired token is ineligible"() {
        given:
        long past = System.currentTimeMillis() - 60_000
        TokenCredential cred = new TokenCredential("anthropic", "tok-abc", past, "e@e.com")

        when:
        CredentialEligibility eligibility = CredentialStateEvaluator.evaluateEligibility(cred)

        then:
        !eligibility.eligible()
        eligibility.reasonCode() == CredentialEligibility.EXPIRED
    }

    def "OAuthCredential with access and refresh is eligible"() {
        given:
        long future = System.currentTimeMillis() + 3600_000
        OAuthCredential cred = new OAuthCredential("chutes", "acc", "ref", future, "e@e.com", "cid")

        expect:
        CredentialStateEvaluator.evaluateEligibility(cred).eligible()
    }

    def "OAuthCredential with only refresh is still eligible"() {
        given:
        // expired access but has refresh — should be eligible for refresh
        long past = System.currentTimeMillis() - 60_000
        OAuthCredential cred = new OAuthCredential("chutes", null, "ref-tok", past, "e@e.com", "cid")

        expect:
        // evaluateEligibility checks hasAccess || hasRefresh, not expiry for OAuth
        CredentialStateEvaluator.evaluateEligibility(cred).eligible()
    }

    def "OAuthCredential with no access and no refresh is ineligible"() {
        given:
        OAuthCredential cred = new OAuthCredential("chutes", null, null, 0L, "e@e.com", "cid")

        when:
        CredentialEligibility eligibility = CredentialStateEvaluator.evaluateEligibility(cred)

        then:
        !eligibility.eligible()
        eligibility.reasonCode() == CredentialEligibility.MISSING_CREDENTIAL
    }

    // --- computeExpiresAt ---

    def "computeExpiresAt subtracts 5-minute safety margin"() {
        given:
        long before = System.currentTimeMillis()

        when:
        long expiresAt = CredentialStateEvaluator.computeExpiresAt(3600) // 1 hour

        then:
        long after = System.currentTimeMillis()
        // Should be roughly now + 3600s - 300s = now + 3300s
        expiresAt >= before + 3300_000 - 100  // 100ms tolerance
        expiresAt <= after + 3300_000 + 100
    }

    def "computeExpiresAt with very small value floors at now + 30s"() {
        given:
        long before = System.currentTimeMillis()

        when:
        long expiresAt = CredentialStateEvaluator.computeExpiresAt(10) // 10 seconds (< 5 min safety)

        then:
        long after = System.currentTimeMillis()
        // Floor: now + 30s
        expiresAt >= before + 30_000 - 100
        expiresAt <= after + 30_000 + 100
    }
}
