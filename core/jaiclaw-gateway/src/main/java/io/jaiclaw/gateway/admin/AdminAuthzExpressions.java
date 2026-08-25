package io.jaiclaw.gateway.admin;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/**
 * Holder bean referenced from {@code @PreAuthorize} SpEL on the
 * {@link AdminController}. Mirrors the pattern used by
 * {@code PipelineAuthzExpressions} and {@code GdprAuthzExpressions}.
 * Bean is registered as {@code adminAuthzExpressions} — that's the exact
 * name the {@code @PreAuthorize} expression uses.
 *
 * <p><b>Degraded-mode behaviour</b>:
 * <ul>
 *   <li>If Spring Security is not on the classpath, the {@code @PreAuthorize}
 *       annotation is silently inert — this bean's methods are never invoked
 *       and any authenticated principal that passes the filter chain reaches
 *       the endpoint.</li>
 *   <li>If Spring Security IS on the classpath but the role config value is
 *       blank (the default), the check short-circuits to {@code true} —
 *       authenticated principals may invoke regardless of authority.</li>
 *   <li>If the role is set and no principal is authenticated, deny.</li>
 *   <li>If the role is set and the authenticated principal lacks the role,
 *       deny.</li>
 * </ul>
 */
public class AdminAuthzExpressions {

    private final AdminAuthzProperties.Roles roles;

    public AdminAuthzExpressions(AdminAuthzProperties.Roles roles) {
        this.roles = roles == null ? AdminAuthzProperties.Roles.defaults() : roles;
    }

    /** Authorises all destructive and read admin endpoints. */
    public boolean admin() {
        return hasAuthorityOrBlank(roles.admin());
    }

    private static boolean hasAuthorityOrBlank(String authority) {
        if (authority == null || authority.isBlank()) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (Objects.equals(a.getAuthority(), authority)) return true;
        }
        return false;
    }
}
