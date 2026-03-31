package io.jaiclaw.config;

import java.util.List;
import java.util.Set;

public record ToolsProperties(
        String profile,
        Set<String> allow,
        Set<String> deny,
        WebToolsProperties web,
        ExecToolProperties exec
) {
    public static final ToolsProperties DEFAULT = new ToolsProperties(
            "coding", Set.of(), Set.of(),
            WebToolsProperties.DEFAULT, ExecToolProperties.DEFAULT
    );

    public record WebToolsProperties(
            boolean searchEnabled,
            boolean fetchEnabled
    ) {
        public static final WebToolsProperties DEFAULT = new WebToolsProperties(true, true);
    }

    public record ExecToolProperties(
            String host,
            String policy,
            List<String> allowedCommands,
            List<String> blockedPatterns,
            int maxTimeout,
            KubectlPolicyProperties kubectl
    ) {
        public static final ExecToolProperties DEFAULT = new ExecToolProperties(
                "sandbox", "unrestricted", List.of(), List.of(), 300,
                KubectlPolicyProperties.DEFAULT
        );
    }

    public record KubectlPolicyProperties(
            String policy,
            List<String> allowedVerbs,
            List<String> blockedVerbs
    ) {
        public static final KubectlPolicyProperties DEFAULT = new KubectlPolicyProperties(
                "unrestricted", List.of(), List.of()
        );
    }
}
