package io.jaiclaw.core.auth;

/**
 * Indirect reference to a secret value, resolved at runtime via the configured backend.
 *
 * @param source   which backend to use (env, file, exec)
 * @param provider named provider alias from config, or "default"
 * @param id       key within that provider (env var name, JSON pointer, command ref ID)
 */
public record SecretRef(
        SecretRefSource source,
        String provider,
        String id
) {
    public static final String DEFAULT_PROVIDER = "default";

    /**
     * Shorthand for an environment variable reference using the default provider.
     */
    public static SecretRef env(String envVarName) {
        return new SecretRef(SecretRefSource.ENV, DEFAULT_PROVIDER, envVarName);
    }
}
