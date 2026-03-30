package io.jaiclaw.identity.secret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Resolves secrets from environment variables.
 */
public class EnvSecretProvider {

    private static final Logger log = LoggerFactory.getLogger(EnvSecretProvider.class);
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Z][A-Z0-9_]{0,127}$");

    /**
     * Resolve an environment variable.
     *
     * @param id     the env var name
     * @param config provider config (for allowlist)
     * @return the env var value
     * @throws SecretRefResolver.SecretResolutionException if not found or not allowed
     */
    public String resolve(String id, SecretProviderConfig config) {
        if (!VALID_NAME.matcher(id).matches()) {
            throw new SecretRefResolver.SecretResolutionException(
                    "Invalid environment variable name: " + id);
        }

        if (config.allowlist() != null && !config.allowlist().isEmpty()
                && !config.allowlist().contains(id)) {
            throw new SecretRefResolver.SecretResolutionException(
                    "Environment variable '" + id + "' is not in the allowlist");
        }

        String value = System.getenv(id);
        if (value == null || value.isBlank()) {
            throw new SecretRefResolver.SecretResolutionException(
                    "Environment variable '" + id + "' is not set or empty");
        }

        return value;
    }
}
