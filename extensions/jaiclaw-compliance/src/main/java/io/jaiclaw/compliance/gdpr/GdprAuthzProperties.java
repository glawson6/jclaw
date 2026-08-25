package io.jaiclaw.compliance.gdpr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Role configuration for the GDPR REST controller ({@link GdprController}).
 * Referenced from {@code @PreAuthorize} SpEL on Article 15 / 17 endpoints
 * via the {@link GdprAuthzExpressions} bean.
 *
 * <p>Config keys:
 * <pre>{@code
 * jaiclaw.compliance.gdpr.roles.operator: ""              # default (any authenticated)
 * jaiclaw.compliance.gdpr.roles.operator: GDPR_OPERATOR   # opt-in to real gating
 * }</pre>
 *
 * <p>The default is blank ({@code ""}) — the check short-circuits to
 * "any authenticated principal". This preserves backward-compatibility for
 * api-key deployments whose principals have no granted authorities. Adopters
 * opt in by setting the role explicitly and ensuring their auth layer grants
 * a matching authority. When Spring Security is absent from the classpath,
 * the {@code @PreAuthorize} annotations are silently inert.
 *
 * <p>One public constructor per {@code @ConfigurationProperties} record
 * (Spring Boot 4 record-binder rule from the framework CLAUDE.md).
 * Programmatic defaults live on {@link Roles#defaults()}, not on an
 * overload constructor.
 */
@ConfigurationProperties(prefix = "jaiclaw.compliance.gdpr")
public record GdprAuthzProperties(Roles roles) {

    public GdprAuthzProperties {
        if (roles == null) roles = Roles.defaults();
    }

    /**
     * Role names checked by {@link GdprAuthzExpressions}. Each field maps
     * to one method on that bean. Blank value → any authenticated principal
     * may invoke.
     */
    public record Roles(String operator) {

        /**
         * Default operator role is intentionally blank so that turning on
         * {@code @EnableMethodSecurity} does not break existing api-key
         * deployments whose principals have no granted authorities.
         * Adopters opt in to role gating by setting
         * {@code jaiclaw.compliance.gdpr.roles.operator=GDPR_OPERATOR}
         * (or their real role) in configuration.
         */
        public static final Roles DEFAULT = new Roles("");

        public Roles {
            if (operator == null) operator = "";
        }

        /** Programmatic default — the binder never sees this. */
        public static Roles defaults() {
            return DEFAULT;
        }
    }
}
