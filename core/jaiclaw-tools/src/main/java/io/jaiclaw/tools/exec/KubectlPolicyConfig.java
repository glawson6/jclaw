package io.jaiclaw.tools.exec;

import java.util.List;

/**
 * Configuration for the kubectl execution policy. Pure Java — no Spring dependency.
 *
 * @param policy       "unrestricted" (default), "read-only", or "allowlist"
 * @param allowedVerbs verbs allowed when policy is "allowlist"
 * @param blockedVerbs verbs always blocked (even in unrestricted mode)
 */
public record KubectlPolicyConfig(
        String policy,
        List<String> allowedVerbs,
        List<String> blockedVerbs
) {
    public static final String POLICY_UNRESTRICTED = "unrestricted";
    public static final String POLICY_READ_ONLY = "read-only";
    public static final String POLICY_ALLOWLIST = "allowlist";

    /** Read-only verbs permitted when policy is "read-only". */
    public static final List<String> READ_ONLY_VERBS = List.of(
            "get", "describe", "logs", "top", "explain", "api-resources", "api-versions"
    );

    public static final KubectlPolicyConfig DEFAULT = new KubectlPolicyConfig(
            POLICY_UNRESTRICTED, List.of(), List.of()
    );
}
