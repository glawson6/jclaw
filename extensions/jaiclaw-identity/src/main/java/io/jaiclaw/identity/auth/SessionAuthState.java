package io.jaiclaw.identity.auth;

/**
 * Per-session auth profile override state.
 *
 * @param authProfileOverride active profileId override (nullable)
 * @param overrideSource      how it was set: "user" (manual pin) or "auto" (round-robin)
 * @param compactionCount     compaction cycle count when the override was set (nullable)
 */
public record SessionAuthState(
        String authProfileOverride,
        String overrideSource,
        Integer compactionCount
) {
    public static final String SOURCE_USER = "user";
    public static final String SOURCE_AUTO = "auto";

    public static SessionAuthState empty() {
        return new SessionAuthState(null, null, null);
    }

    /** Returns a copy with the override cleared. */
    public SessionAuthState cleared() {
        return empty();
    }

    /** Returns a copy with a user-pinned override. */
    public SessionAuthState withUserOverride(String profileId) {
        return new SessionAuthState(profileId, SOURCE_USER, compactionCount);
    }

    /** Returns a copy with an auto-rotated override. */
    public SessionAuthState withAutoOverride(String profileId, Integer currentCompactionCount) {
        return new SessionAuthState(profileId, SOURCE_AUTO, currentCompactionCount);
    }
}
