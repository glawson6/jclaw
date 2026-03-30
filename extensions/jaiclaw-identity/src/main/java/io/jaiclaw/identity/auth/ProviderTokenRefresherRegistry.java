package io.jaiclaw.identity.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of provider-specific {@link TokenRefresher} implementations.
 * Falls back to logging an error if no refresher is registered for a provider.
 */
public class ProviderTokenRefresherRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProviderTokenRefresherRegistry.class);

    private final Map<String, TokenRefresher> refreshers = new ConcurrentHashMap<>();

    public ProviderTokenRefresherRegistry() {}

    public ProviderTokenRefresherRegistry(List<TokenRefresher> refreshers) {
        for (TokenRefresher refresher : refreshers) {
            register(refresher);
        }
    }

    /** Register a refresher for a provider. */
    public void register(TokenRefresher refresher) {
        String id = refresher.providerId().toLowerCase();
        refreshers.put(id, refresher);
        log.debug("Registered token refresher for provider '{}'", id);
    }

    /** Look up a refresher for a provider. */
    public Optional<TokenRefresher> get(String providerId) {
        return Optional.ofNullable(refreshers.get(providerId.toLowerCase()));
    }

    /** Check if a refresher exists for a provider. */
    public boolean hasRefresher(String providerId) {
        return refreshers.containsKey(providerId.toLowerCase());
    }

    /** Returns all registered provider IDs. */
    public java.util.Set<String> registeredProviders() {
        return java.util.Set.copyOf(refreshers.keySet());
    }
}
