package io.jaiclaw.identity.auth;

import io.jaiclaw.core.auth.*;

/**
 * Pure utility methods for evaluating credential expiry state and eligibility.
 * Port of OpenClaw's {@code credential-state.ts}.
 */
public final class CredentialStateEvaluator {

    private CredentialStateEvaluator() {}

    /**
     * Evaluate the expiry state of a timestamp.
     *
     * @param expires ms-since-epoch expiry timestamp (nullable)
     * @return the credential state
     */
    public static CredentialState resolveTokenExpiryState(Long expires) {
        if (expires == null) return CredentialState.MISSING;
        if (expires <= 0) return CredentialState.INVALID;
        if (System.currentTimeMillis() >= expires) return CredentialState.EXPIRED;
        return CredentialState.VALID;
    }

    /**
     * Evaluate whether a stored credential is eligible for use.
     *
     * @param credential the credential to evaluate
     * @return eligibility result with reason code
     */
    public static CredentialEligibility evaluateEligibility(AuthProfileCredential credential) {
        return switch (credential) {
            case ApiKeyCredential c -> {
                boolean hasKey = c.key() != null && !c.key().isBlank();
                boolean hasRef = c.keyRef() != null;
                yield hasKey || hasRef
                        ? CredentialEligibility.ok()
                        : CredentialEligibility.missing();
            }
            case TokenCredential c -> {
                boolean hasToken = (c.token() != null && !c.token().isBlank()) || c.tokenRef() != null;
                if (!hasToken) yield CredentialEligibility.missing();
                CredentialState state = resolveTokenExpiryState(c.expires());
                yield switch (state) {
                    case EXPIRED -> new CredentialEligibility(false, CredentialEligibility.EXPIRED);
                    case INVALID -> new CredentialEligibility(false, CredentialEligibility.INVALID_EXPIRES);
                    default -> CredentialEligibility.ok();
                };
            }
            case OAuthCredential c -> {
                boolean hasAccess = c.access() != null && !c.access().isBlank();
                boolean hasRefresh = c.refresh() != null && !c.refresh().isBlank();
                yield hasAccess || hasRefresh
                        ? CredentialEligibility.ok()
                        : CredentialEligibility.missing();
            }
        };
    }

    /**
     * Compute the expiry timestamp from an expires_in value.
     * Subtracts 5 minutes as a safety margin, with a floor of now + 30 seconds.
     *
     * @param expiresInSeconds the expires_in value from the token response
     * @return absolute expiry timestamp in ms-since-epoch
     */
    public static long computeExpiresAt(int expiresInSeconds) {
        long now = System.currentTimeMillis();
        long fiveMinutes = 5 * 60 * 1000L;
        long thirtySeconds = 30 * 1000L;
        long value = now + Math.max(0, expiresInSeconds) * 1000L - fiveMinutes;
        return Math.max(value, now + thirtySeconds);
    }
}
