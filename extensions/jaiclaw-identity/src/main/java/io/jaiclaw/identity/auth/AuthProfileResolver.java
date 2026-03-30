package io.jaiclaw.identity.auth;

import io.jaiclaw.core.auth.*;
import io.jaiclaw.identity.secret.SecretRefResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves a profile ID to a usable credential (API key or access token).
 * Handles secret ref resolution, token expiry checking, and OAuth token refresh.
 */
public class AuthProfileResolver {

    private static final Logger log = LoggerFactory.getLogger(AuthProfileResolver.class);

    private final AuthProfileStoreManager storeManager;
    private final SecretRefResolver secretRefResolver;
    private final ProviderTokenRefresherRegistry refresherRegistry;

    public AuthProfileResolver(AuthProfileStoreManager storeManager,
                               SecretRefResolver secretRefResolver,
                               ProviderTokenRefresherRegistry refresherRegistry) {
        this.storeManager = storeManager;
        this.secretRefResolver = secretRefResolver;
        this.refresherRegistry = refresherRegistry;
    }

    /**
     * Resolve a profile ID to a usable credential.
     *
     * @param profileId the profile ID (e.g. "anthropic:default")
     * @param agentDir  the agent directory (null for main agent)
     * @return the resolved credential
     * @throws CredentialResolutionException if resolution fails
     */
    public ResolvedCredential resolve(String profileId, Path agentDir) {
        AuthProfileStore store = agentDir != null
                ? storeManager.loadForRuntime(agentDir)
                : storeManager.loadMainStore();

        AuthProfileCredential credential = store.profiles().get(profileId);
        if (credential == null) {
            throw new CredentialResolutionException(profileId, "Profile not found: " + profileId);
        }

        return switch (credential) {
            case ApiKeyCredential c -> resolveApiKey(profileId, c);
            case TokenCredential c -> resolveToken(profileId, c);
            case OAuthCredential c -> resolveOAuth(profileId, c, agentDir);
        };
    }

    private ResolvedCredential resolveApiKey(String profileId, ApiKeyCredential cred) {
        String key;
        if (cred.keyRef() != null) {
            key = secretRefResolver.resolve(cred.keyRef());
        } else if (cred.key() != null && !cred.key().isBlank()) {
            // Check for ${VAR} coercion
            SecretRef coerced = AuthProfileStoreSerializer.coerceEnvRef(cred.key());
            if (coerced != null) {
                key = secretRefResolver.resolve(coerced);
            } else {
                key = cred.key();
            }
        } else {
            throw new CredentialResolutionException(profileId, "API key credential has no key or keyRef");
        }
        return new ResolvedCredential(key, cred.provider(), cred.email());
    }

    private ResolvedCredential resolveToken(String profileId, TokenCredential cred) {
        CredentialState state = CredentialStateEvaluator.resolveTokenExpiryState(cred.expires());
        if (state == CredentialState.EXPIRED || state == CredentialState.INVALID) {
            throw new CredentialResolutionException(profileId,
                    "Token credential is " + state.name().toLowerCase());
        }

        String token;
        if (cred.tokenRef() != null) {
            token = secretRefResolver.resolve(cred.tokenRef());
        } else if (cred.token() != null && !cred.token().isBlank()) {
            SecretRef coerced = AuthProfileStoreSerializer.coerceEnvRef(cred.token());
            if (coerced != null) {
                token = secretRefResolver.resolve(coerced);
            } else {
                token = cred.token();
            }
        } else {
            throw new CredentialResolutionException(profileId, "Token credential has no token or tokenRef");
        }
        return new ResolvedCredential(token, cred.provider(), cred.email());
    }

    private ResolvedCredential resolveOAuth(String profileId, OAuthCredential cred, Path agentDir) {
        // Try adopting from main agent first (sub-agent context)
        if (agentDir != null) {
            Optional<OAuthCredential> adopted = storeManager.adoptFromMain(agentDir, profileId);
            if (adopted.isPresent()) {
                cred = adopted.get();
            }
        }

        // If not expired, use directly
        if (System.currentTimeMillis() < cred.expires()) {
            return new ResolvedCredential(cred.access(), cred.provider(), cred.email());
        }

        // Refresh under lock
        try {
            OAuthCredential refreshed = refreshWithLock(profileId, agentDir != null ? agentDir : storeManager.resolveMainAgentDir(), cred);
            return new ResolvedCredential(refreshed.access(), refreshed.provider(), refreshed.email());
        } catch (TokenRefreshException e) {
            // Fallback: try main agent's credential for sub-agents
            if (agentDir != null) {
                Optional<OAuthCredential> mainFallback = tryMainAgentFallback(profileId, agentDir);
                if (mainFallback.isPresent()) {
                    return new ResolvedCredential(mainFallback.get().access(),
                            mainFallback.get().provider(), mainFallback.get().email());
                }
            }
            throw new CredentialResolutionException(profileId,
                    "OAuth token refresh failed for " + cred.provider(), e);
        }
    }

    private OAuthCredential refreshWithLock(String profileId, Path agentDir, OAuthCredential cred)
            throws TokenRefreshException {
        Path lockPath = storeManager.lockPath(agentDir);
        return AuthProfileFileLock.withLock(lockPath, () -> {
            // Re-read store under lock (another process may have refreshed)
            AuthProfileStore fresh = storeManager.loadForAgent(agentDir);
            AuthProfileCredential freshCred = fresh.profiles().get(profileId);
            if (freshCred instanceof OAuthCredential freshOAuth
                    && System.currentTimeMillis() < freshOAuth.expires()) {
                log.debug("Token was refreshed by another process for profile '{}'", profileId);
                return freshOAuth;
            }

            OAuthCredential toRefresh = (freshCred instanceof OAuthCredential fo) ? fo : cred;

            // Find refresher
            Optional<TokenRefresher> refresher = refresherRegistry.get(toRefresh.provider());
            if (refresher.isEmpty()) {
                throw new RuntimeException(new TokenRefreshException(toRefresh.provider(),
                        "No token refresher registered for provider: " + toRefresh.provider()));
            }

            try {
                OAuthCredential updated = refresher.get().refresh(toRefresh);
                storeManager.save(agentDir, fresh.withProfile(profileId, updated));
                log.info("Refreshed OAuth token for profile '{}'", profileId);
                return updated;
            } catch (TokenRefreshException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Optional<OAuthCredential> tryMainAgentFallback(String profileId, Path subAgentDir) {
        try {
            AuthProfileStore mainStore = storeManager.loadMainStore();
            AuthProfileCredential mainCred = mainStore.profiles().get(profileId);
            if (mainCred instanceof OAuthCredential mainOAuth
                    && System.currentTimeMillis() < mainOAuth.expires()) {
                log.info("Using main agent's valid credential as fallback for profile '{}'", profileId);
                storeManager.upsertProfile(subAgentDir, profileId, mainOAuth);
                return Optional.of(mainOAuth);
            }
        } catch (Exception e) {
            log.debug("Main agent fallback failed for profile '{}': {}", profileId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Exception thrown when credential resolution fails.
     */
    public static class CredentialResolutionException extends RuntimeException {
        private final String profileId;

        public CredentialResolutionException(String profileId, String message) {
            super(message);
            this.profileId = profileId;
        }

        public CredentialResolutionException(String profileId, String message, Throwable cause) {
            super(message, cause);
            this.profileId = profileId;
        }

        public String profileId() {
            return profileId;
        }
    }
}
