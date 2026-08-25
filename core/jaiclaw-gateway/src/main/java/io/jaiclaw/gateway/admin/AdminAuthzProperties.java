package io.jaiclaw.gateway.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Role configuration for the JaiClaw gateway {@link AdminController}.
 * Referenced from {@code @PreAuthorize} SpEL via the
 * {@link AdminAuthzExpressions} bean.
 *
 * <p>Config keys:
 * <pre>{@code
 * jaiclaw.gateway.admin.roles.admin: ""              # default (any authenticated)
 * jaiclaw.gateway.admin.roles.admin: JAICLAW_ADMIN   # opt-in to real gating
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
@ConfigurationProperties(prefix = "jaiclaw.gateway.admin")
public record AdminAuthzProperties(Roles roles) {

    public AdminAuthzProperties {
        if (roles == null) roles = Roles.defaults();
    }

    /**
     * Role names checked by {@link AdminAuthzExpressions}. Blank value
     * means any authenticated principal may invoke.
     */
    public record Roles(String admin) {

        public static final Roles DEFAULT = new Roles("");

        public Roles {
            if (admin == null) admin = "";
        }

        /** Programmatic default — the binder never sees this. */
        public static Roles defaults() {
            return DEFAULT;
        }
    }
}
