package io.jaiclaw.core.auth

import spock.lang.Specification

class ProfileUsageStatsSpec extends Specification {

    def "empty stats has zero error count and no flags"() {
        given:
        ProfileUsageStats stats = ProfileUsageStats.empty()

        expect:
        stats.errorCount() == 0
        stats.lastUsed() == null
        stats.cooldownUntil() == null
        stats.disabledUntil() == null
        stats.disabledReason() == null
        stats.lastFailureAt() == null
        stats.failureCounts().isEmpty()
    }

    def "isInCooldown returns false when not set"() {
        expect:
        !ProfileUsageStats.empty().isInCooldown()
    }

    def "isInCooldown returns true when cooldownUntil is in the future"() {
        given:
        long future = System.currentTimeMillis() + 60_000
        ProfileUsageStats stats = new ProfileUsageStats(null, future, null, null, 1, Map.of(), null)

        expect:
        stats.isInCooldown()
    }

    def "isInCooldown returns false when cooldownUntil is in the past"() {
        given:
        long past = System.currentTimeMillis() - 60_000
        ProfileUsageStats stats = new ProfileUsageStats(null, past, null, null, 1, Map.of(), null)

        expect:
        !stats.isInCooldown()
    }

    def "isDisabled returns false when not set"() {
        expect:
        !ProfileUsageStats.empty().isDisabled()
    }

    def "isDisabled returns true when disabledUntil is in the future"() {
        given:
        long future = System.currentTimeMillis() + 300_000
        ProfileUsageStats stats = new ProfileUsageStats(null, null, future, AuthProfileFailureReason.RATE_LIMIT, 3, Map.of(), null)

        expect:
        stats.isDisabled()
        stats.disabledReason() == AuthProfileFailureReason.RATE_LIMIT
    }

    def "isDisabled returns false when disabledUntil is in the past"() {
        given:
        long past = System.currentTimeMillis() - 300_000
        ProfileUsageStats stats = new ProfileUsageStats(null, null, past, AuthProfileFailureReason.BILLING, 2, Map.of(), null)

        expect:
        !stats.isDisabled()
    }
}
