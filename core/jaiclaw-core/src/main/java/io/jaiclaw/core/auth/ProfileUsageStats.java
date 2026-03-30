package io.jaiclaw.core.auth;

import java.util.Map;

/**
 * Usage and failure tracking for an auth profile, used for round-robin rotation and cooldowns.
 *
 * @param lastUsed        last time this profile was used (ms-since-epoch, nullable)
 * @param cooldownUntil   profile is in cooldown until this time (ms-since-epoch, nullable)
 * @param disabledUntil   profile is disabled until this time (ms-since-epoch, nullable)
 * @param disabledReason  why the profile was disabled (nullable)
 * @param errorCount      total number of errors
 * @param failureCounts   per-reason failure counts
 * @param lastFailureAt   last failure timestamp (ms-since-epoch, nullable)
 */
public record ProfileUsageStats(
        Long lastUsed,
        Long cooldownUntil,
        Long disabledUntil,
        AuthProfileFailureReason disabledReason,
        int errorCount,
        Map<AuthProfileFailureReason, Integer> failureCounts,
        Long lastFailureAt
) {
    public static ProfileUsageStats empty() {
        return new ProfileUsageStats(null, null, null, null, 0, Map.of(), null);
    }

    /** Returns true if this profile is currently in cooldown. */
    public boolean isInCooldown() {
        return cooldownUntil != null && System.currentTimeMillis() < cooldownUntil;
    }

    /** Returns true if this profile is currently disabled. */
    public boolean isDisabled() {
        return disabledUntil != null && System.currentTimeMillis() < disabledUntil;
    }
}
