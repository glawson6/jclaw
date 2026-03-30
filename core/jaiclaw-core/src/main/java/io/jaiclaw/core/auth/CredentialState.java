package io.jaiclaw.core.auth;

/**
 * Token expiry state for a credential.
 */
public enum CredentialState {
    /** Token is present and not expired. */
    VALID,
    /** Token has expired (now >= expires). */
    EXPIRED,
    /** No expiry value provided. */
    MISSING,
    /** Expiry value is not a valid timestamp (non-finite or <= 0). */
    INVALID
}
