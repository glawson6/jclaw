package io.jaiclaw.core.auth

import spock.lang.Specification

class CredentialEligibilitySpec extends Specification {

    def "ok factory returns eligible"() {
        given:
        CredentialEligibility eligibility = CredentialEligibility.ok()

        expect:
        eligibility.eligible()
        eligibility.reasonCode() == CredentialEligibility.OK
    }

    def "missing factory returns ineligible"() {
        given:
        CredentialEligibility eligibility = CredentialEligibility.missing()

        expect:
        !eligibility.eligible()
        eligibility.reasonCode() == CredentialEligibility.MISSING_CREDENTIAL
    }

    def "custom ineligible reason"() {
        given:
        CredentialEligibility eligibility = new CredentialEligibility(false, CredentialEligibility.EXPIRED)

        expect:
        !eligibility.eligible()
        eligibility.reasonCode() == "expired"
    }

    def "reason code constants are consistent"() {
        expect:
        CredentialEligibility.OK == "ok"
        CredentialEligibility.MISSING_CREDENTIAL == "missing_credential"
        CredentialEligibility.INVALID_EXPIRES == "invalid_expires"
        CredentialEligibility.EXPIRED == "expired"
        CredentialEligibility.UNRESOLVED_REF == "unresolved_ref"
    }
}
