package io.jaiclaw.core.auth;

/**
 * Reason why an auth profile was disabled or failed.
 */
public enum AuthProfileFailureReason {
    AUTH,
    AUTH_PERMANENT,
    FORMAT,
    OVERLOADED,
    RATE_LIMIT,
    BILLING,
    TIMEOUT,
    MODEL_NOT_FOUND,
    SESSION_EXPIRED,
    UNKNOWN
}
