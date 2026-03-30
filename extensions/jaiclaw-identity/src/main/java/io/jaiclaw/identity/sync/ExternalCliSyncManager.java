package io.jaiclaw.identity.sync;

import io.jaiclaw.core.auth.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates credential sync from external CLIs (Claude, Codex, Qwen, MiniMax).
 * Called on every store load to keep credentials fresh.
 */
public class ExternalCliSyncManager {

    private static final Logger log = LoggerFactory.getLogger(ExternalCliSyncManager.class);

    private final CachedCredentialReader<AuthProfileCredential> claudeReader;
    private final CachedCredentialReader<OAuthCredential> codexReader;
    private final CachedCredentialReader<OAuthCredential> qwenReader;
    private final CachedCredentialReader<OAuthCredential> minimaxReader;

    public ExternalCliSyncManager() {
        KeychainReader keychain = new KeychainReader();
        this.claudeReader = new CachedCredentialReader<>(
                () -> new ClaudeCliCredentialReader(keychain).read());
        this.codexReader = new CachedCredentialReader<>(
                () -> new CodexCliCredentialReader(keychain).read());
        this.qwenReader = new CachedCredentialReader<>(
                () -> new QwenCliCredentialReader().read());
        this.minimaxReader = new CachedCredentialReader<>(
                () -> new MiniMaxCliCredentialReader().read());
    }

    /**
     * Sync all external CLI credentials into the given store.
     *
     * @param store the current auth profile store
     * @return updated store if any profiles were mutated, or the original store if unchanged
     */
    public AuthProfileStore syncAll(AuthProfileStore store) {
        Map<String, AuthProfileCredential> profiles = new HashMap<>(store.profiles());
        boolean mutated = false;

        mutated |= syncClaude(profiles);
        mutated |= syncCodex(profiles);
        mutated |= syncQwen(profiles);
        mutated |= syncMiniMax(profiles);

        if (mutated) {
            log.debug("External CLI sync updated {} profile(s)", profiles.size() - store.profiles().size());
            return store.withProfiles(Map.copyOf(profiles));
        }
        return store;
    }

    private boolean syncClaude(Map<String, AuthProfileCredential> profiles) {
        return syncProvider(profiles, AuthProfileConstants.CLAUDE_CLI_PROFILE_ID, "anthropic",
                claudeReader.read());
    }

    private boolean syncCodex(Map<String, AuthProfileCredential> profiles) {
        Optional<OAuthCredential> cred = codexReader.read();
        return syncProvider(profiles, AuthProfileConstants.CODEX_CLI_PROFILE_ID, "openai-codex",
                cred.map(c -> (AuthProfileCredential) c));
    }

    private boolean syncQwen(Map<String, AuthProfileCredential> profiles) {
        Optional<OAuthCredential> cred = qwenReader.read();
        return syncProvider(profiles, AuthProfileConstants.QWEN_CLI_PROFILE_ID, "qwen-portal",
                cred.map(c -> (AuthProfileCredential) c));
    }

    private boolean syncMiniMax(Map<String, AuthProfileCredential> profiles) {
        Optional<OAuthCredential> cred = minimaxReader.read();
        return syncProvider(profiles, AuthProfileConstants.MINIMAX_CLI_PROFILE_ID, "minimax-portal",
                cred.map(c -> (AuthProfileCredential) c));
    }

    private boolean syncProvider(Map<String, AuthProfileCredential> profiles,
                                 String profileId, String provider,
                                 Optional<AuthProfileCredential> newCred) {
        if (newCred.isEmpty()) return false;

        long now = System.currentTimeMillis();
        AuthProfileCredential existing = profiles.get(profileId);

        // Skip if existing credential is still fresh
        if (existing instanceof OAuthCredential existingOAuth) {
            if (existingOAuth.expires() > now + AuthProfileConstants.EXTERNAL_CLI_NEAR_EXPIRY_MS) {
                return false;
            }
        }

        // Update if: no existing, existing is expired, or new has later expiry
        boolean shouldUpdate = false;
        if (existing == null) {
            shouldUpdate = true;
        } else if (existing instanceof OAuthCredential existingOAuth) {
            if (existingOAuth.expires() <= now) {
                shouldUpdate = true;
            } else if (newCred.get() instanceof OAuthCredential newOAuth
                    && newOAuth.expires() > existingOAuth.expires()) {
                shouldUpdate = true;
            }
        } else {
            shouldUpdate = true;
        }

        if (shouldUpdate) {
            profiles.put(profileId, newCred.get());
            log.debug("Synced credential from external CLI: {}", profileId);
            return true;
        }
        return false;
    }
}
