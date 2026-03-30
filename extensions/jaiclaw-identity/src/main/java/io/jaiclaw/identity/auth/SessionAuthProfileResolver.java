package io.jaiclaw.identity.auth;

import io.jaiclaw.core.auth.AuthProfileCredential;
import io.jaiclaw.core.auth.AuthProfileStore;
import io.jaiclaw.core.auth.ProfileUsageStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Resolves per-session auth profile overrides with round-robin rotation and cooldown awareness.
 */
public class SessionAuthProfileResolver {

    private static final Logger log = LoggerFactory.getLogger(SessionAuthProfileResolver.class);

    private final AuthProfileStoreManager storeManager;

    public SessionAuthProfileResolver(AuthProfileStoreManager storeManager) {
        this.storeManager = storeManager;
    }

    /**
     * Resolve the auth profile override for a session.
     *
     * @param provider                the provider to resolve for
     * @param agentDir                the agent directory
     * @param currentState            current session auth state
     * @param isNewSession            true if this is a new session
     * @param currentCompactionCount  current compaction cycle count (nullable)
     * @return the resolved profile ID, or empty if no rotation configured
     */
    public Optional<String> resolve(String provider, Path agentDir,
                                     SessionAuthState currentState,
                                     boolean isNewSession,
                                     Integer currentCompactionCount) {
        AuthProfileStore store = storeManager.loadForRuntime(agentDir);
        List<String> order = store.order().get(provider.toLowerCase());
        if (order == null || order.isEmpty()) {
            return Optional.empty();
        }

        String current = currentState != null ? currentState.authProfileOverride() : null;

        // Validate current override
        if (current != null) {
            // Clear if profile no longer exists
            if (!store.profiles().containsKey(current)) {
                current = null;
            }
            // Clear if wrong provider
            else {
                AuthProfileCredential cred = store.profiles().get(current);
                if (!cred.provider().equalsIgnoreCase(provider)) {
                    current = null;
                }
            }
            // Clear if not in order list
            if (current != null && !order.contains(current)) {
                current = null;
            }
        }

        // User-pinned overrides are sticky (unless invalid)
        if (current != null && currentState != null
                && SessionAuthState.SOURCE_USER.equals(currentState.overrideSource())) {
            // Only clear if profile is in cooldown
            if (!isInCooldown(store, current)) {
                return Optional.of(current);
            }
        }

        // Rotation triggers
        boolean shouldRotate = false;
        if (isNewSession) {
            shouldRotate = true;
        } else if (currentState != null && currentCompactionCount != null
                && currentState.compactionCount() != null
                && !currentCompactionCount.equals(currentState.compactionCount())) {
            shouldRotate = true;
        } else if (current != null && isInCooldown(store, current)) {
            shouldRotate = true;
        }

        if (current == null || shouldRotate) {
            String resolved = current == null
                    ? pickFirstAvailable(store, order)
                    : pickNextAvailable(store, order, current);
            return Optional.ofNullable(resolved);
        }

        return Optional.ofNullable(current);
    }

    /** Pick the first profile in order that is not in cooldown. */
    private String pickFirstAvailable(AuthProfileStore store, List<String> order) {
        for (String profileId : order) {
            if (!isInCooldown(store, profileId)) {
                return profileId;
            }
        }
        // Fallback: first in order regardless of cooldown
        return order.isEmpty() ? null : order.get(0);
    }

    /** Pick the next profile after the active one, wrapping around. */
    private String pickNextAvailable(AuthProfileStore store, List<String> order, String active) {
        int currentIndex = order.indexOf(active);
        int size = order.size();

        for (int i = 1; i <= size; i++) {
            String candidate = order.get((currentIndex + i) % size);
            if (!isInCooldown(store, candidate)) {
                return candidate;
            }
        }
        // Fallback: first in order
        return order.get(0);
    }

    private boolean isInCooldown(AuthProfileStore store, String profileId) {
        ProfileUsageStats stats = store.usageStats().get(profileId);
        return stats != null && (stats.isInCooldown() || stats.isDisabled());
    }
}
