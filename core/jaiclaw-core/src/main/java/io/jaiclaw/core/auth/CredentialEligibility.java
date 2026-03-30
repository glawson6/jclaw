package io.jaiclaw.core.auth;

/**
 * Result of evaluating whether a stored credential is eligible for use.
 *
 * @param eligible   true if the credential can be used
 * @param reasonCode reason code explaining the eligibility result
 */
public record CredentialEligibility(boolean eligible, String reasonCode) {

    public static final String OK = "ok";
    public static final String MISSING_CREDENTIAL = "missing_credential";
    public static final String INVALID_EXPIRES = "invalid_expires";
    public static final String EXPIRED = "expired";
    public static final String UNRESOLVED_REF = "unresolved_ref";

    public static CredentialEligibility ok() {
        return new CredentialEligibility(true, OK);
    }

    public static CredentialEligibility missing() {
        return new CredentialEligibility(false, MISSING_CREDENTIAL);
    }
}
