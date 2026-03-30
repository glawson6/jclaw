package io.jaiclaw.core.auth;

/**
 * Backend type for resolving a {@link SecretRef}.
 */
public enum SecretRefSource {
    /** Environment variable. */
    ENV,
    /** File-backed secret (JSON pointer or single value). */
    FILE,
    /** External command / plugin. */
    EXEC
}
