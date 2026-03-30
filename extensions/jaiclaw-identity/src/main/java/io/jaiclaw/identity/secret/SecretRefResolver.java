package io.jaiclaw.identity.secret;

import io.jaiclaw.core.auth.SecretRef;
import io.jaiclaw.core.auth.SecretRefSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@link SecretRef} values to their plaintext secrets via the configured backend.
 * Supports ENV, FILE, and EXEC source types.
 */
public class SecretRefResolver {

    private static final Logger log = LoggerFactory.getLogger(SecretRefResolver.class);
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("^\\$\\{([A-Z][A-Z0-9_]*)\\}$");

    private final Map<String, SecretProviderConfig> providerConfigs;
    private final EnvSecretProvider envProvider;
    private final FileSecretProvider fileProvider;
    private final ExecSecretProvider execProvider;

    public SecretRefResolver() {
        this(Map.of());
    }

    public SecretRefResolver(Map<String, SecretProviderConfig> providerConfigs) {
        this.providerConfigs = new ConcurrentHashMap<>(providerConfigs);
        this.envProvider = new EnvSecretProvider();
        this.fileProvider = new FileSecretProvider();
        this.execProvider = new ExecSecretProvider();
    }

    /**
     * Resolve a SecretRef to its plaintext value.
     *
     * @throws SecretResolutionException if the secret cannot be resolved
     */
    public String resolve(SecretRef ref) {
        SecretProviderConfig config = providerConfigs.getOrDefault(
                ref.provider(), SecretProviderConfig.defaultEnv());

        return switch (ref.source()) {
            case ENV -> envProvider.resolve(ref.id(), config);
            case FILE -> fileProvider.resolve(ref.id(), config);
            case EXEC -> execProvider.resolve(ref.id(), config);
        };
    }

    /**
     * Resolve an inline value or SecretRef. If the inline value is a {@code ${VAR}} pattern,
     * it's treated as an env var reference.
     *
     * @param inlineValue inline string value (nullable)
     * @param ref         SecretRef (nullable, takes precedence if set)
     * @return the resolved secret string
     */
    public String resolveInlineOrRef(String inlineValue, SecretRef ref) {
        if (ref != null) {
            return resolve(ref);
        }
        if (inlineValue != null) {
            Matcher matcher = ENV_VAR_PATTERN.matcher(inlineValue);
            if (matcher.matches()) {
                return resolve(SecretRef.env(matcher.group(1)));
            }
            return inlineValue;
        }
        throw new SecretResolutionException("No inline value or ref provided");
    }

    /** Register a provider configuration. */
    public void registerProvider(String name, SecretProviderConfig config) {
        providerConfigs.put(name, config);
    }

    /**
     * Exception thrown when a secret cannot be resolved.
     */
    public static class SecretResolutionException extends RuntimeException {
        public SecretResolutionException(String message) {
            super(message);
        }

        public SecretResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
