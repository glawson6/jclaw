package io.jaiclaw.core.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * On-disk structure for auth profile storage ({@code auth-profiles.json}).
 * <p>
 * Compatible with OpenClaw's format for read/write interoperability.
 *
 * @param version    store format version (currently {@value CURRENT_VERSION})
 * @param profiles   profileId → credential mapping
 * @param order      provider → ordered list of profileIds for rotation
 * @param lastGood   provider → last-known-good profileId
 * @param usageStats profileId → usage and failure tracking
 */
public record AuthProfileStore(
        int version,
        Map<String, AuthProfileCredential> profiles,
        Map<String, List<String>> order,
        Map<String, String> lastGood,
        Map<String, ProfileUsageStats> usageStats
) {
    public static final int CURRENT_VERSION = 1;

    public static AuthProfileStore empty() {
        return new AuthProfileStore(CURRENT_VERSION, Map.of(), Map.of(), Map.of(), Map.of());
    }

    /** Returns a copy with the given profiles map (for immutable updates). */
    public AuthProfileStore withProfiles(Map<String, AuthProfileCredential> newProfiles) {
        return new AuthProfileStore(version, newProfiles, order, lastGood, usageStats);
    }

    /** Returns a copy with a single profile added or replaced. */
    public AuthProfileStore withProfile(String profileId, AuthProfileCredential credential) {
        Map<String, AuthProfileCredential> newProfiles = new HashMap<>(profiles);
        newProfiles.put(profileId, credential);
        return new AuthProfileStore(version, Map.copyOf(newProfiles), order, lastGood, usageStats);
    }

    /** Returns a copy with a single profile removed. */
    public AuthProfileStore withoutProfile(String profileId) {
        Map<String, AuthProfileCredential> newProfiles = new HashMap<>(profiles);
        newProfiles.remove(profileId);
        return new AuthProfileStore(version, Map.copyOf(newProfiles), order, lastGood, usageStats);
    }

    /** Returns a copy with updated order for a provider. */
    public AuthProfileStore withOrder(String provider, List<String> profileIds) {
        Map<String, List<String>> newOrder = new HashMap<>(order);
        if (profileIds == null || profileIds.isEmpty()) {
            newOrder.remove(provider);
        } else {
            newOrder.put(provider, List.copyOf(profileIds));
        }
        return new AuthProfileStore(version, profiles, Map.copyOf(newOrder), lastGood, usageStats);
    }

    /** Returns a copy with updated lastGood for a provider. */
    public AuthProfileStore withLastGood(String provider, String profileId) {
        Map<String, String> newLastGood = new HashMap<>(lastGood);
        newLastGood.put(provider, profileId);
        return new AuthProfileStore(version, profiles, order, Map.copyOf(newLastGood), usageStats);
    }

    /** Returns a copy with updated usage stats for a profile. */
    public AuthProfileStore withUsageStats(String profileId, ProfileUsageStats stats) {
        Map<String, ProfileUsageStats> newStats = new HashMap<>(usageStats);
        newStats.put(profileId, stats);
        return new AuthProfileStore(version, profiles, order, lastGood, Map.copyOf(newStats));
    }
}
