package io.jaiclaw.identity.secret;

import io.jaiclaw.core.auth.SecretRefSource;

import java.util.List;
import java.util.Map;

/**
 * Configuration for a named secret provider backend.
 *
 * @param source          which backend type (ENV, FILE, EXEC)
 * @param name            provider alias
 * @param allowlist       ENV: permitted env var names (null = any)
 * @param path            FILE: absolute file path
 * @param mode            FILE: "json" (default) or "singleValue"
 * @param allowSymlinkPath FILE: whether to follow symlinks (default false)
 * @param timeoutMs       FILE/EXEC: timeout in milliseconds (default 5000)
 * @param maxBytes        FILE/EXEC: max payload size (default 1MB)
 * @param command         EXEC: absolute command path
 * @param args            EXEC: command arguments
 * @param jsonOnly        EXEC: require JSON output (default true)
 * @param env             EXEC: extra environment variables
 * @param trustedDirs     EXEC: allowed command directories
 */
public record SecretProviderConfig(
        SecretRefSource source,
        String name,
        List<String> allowlist,
        String path,
        String mode,
        boolean allowSymlinkPath,
        long timeoutMs,
        long maxBytes,
        String command,
        List<String> args,
        boolean jsonOnly,
        Map<String, String> env,
        List<String> trustedDirs
) {
    public static final String MODE_JSON = "json";
    public static final String MODE_SINGLE_VALUE = "singleValue";
    public static final long DEFAULT_TIMEOUT_MS = 5000;
    public static final long DEFAULT_MAX_BYTES = 1024 * 1024; // 1MB

    /** Creates a default ENV provider config. */
    public static SecretProviderConfig defaultEnv() {
        return new SecretProviderConfig(SecretRefSource.ENV, "default",
                null, null, null, false, DEFAULT_TIMEOUT_MS, DEFAULT_MAX_BYTES,
                null, List.of(), true, Map.of(), List.of());
    }
}
