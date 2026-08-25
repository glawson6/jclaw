package io.jaiclaw.core.tenant;

import java.util.Map;

/**
 * Resolves a tenant identifier from an MCP tool argument map while defending
 * against cross-tenant escapes: if the caller supplies a {@code tenantId}
 * (or equivalent) that does not match their {@link TenantContext}, throw
 * {@link CrossTenantAccessException}.
 *
 * <p>Rules:
 * <ul>
 *   <li>If {@code args[argName]} is absent, null, or blank → return caller's
 *       tenant id (or {@code defaultTenant} when caller is null).</li>
 *   <li>If present and equals caller's tenant id → return that value.</li>
 *   <li>If present and does NOT equal caller's tenant id → throw
 *       {@link CrossTenantAccessException}.</li>
 *   <li>Caller null + arg present: allowed only if the arg equals
 *       {@code defaultTenant}, else throw.</li>
 * </ul>
 */
public final class TenantArgResolver {

    private TenantArgResolver() {}

    public static String resolveOrValidate(
            TenantContext caller,
            Map<String, Object> args,
            String argName,
            String defaultTenant) {
        String callerTenantId = caller == null ? defaultTenant : caller.getTenantId();
        String requested = extract(args, argName);
        if (requested == null) {
            return callerTenantId;
        }
        if (callerTenantId == null) {
            if (requested.equals(defaultTenant)) {
                return requested;
            }
            throw new CrossTenantAccessException(null, requested, argName);
        }
        if (!requested.equals(callerTenantId)) {
            throw new CrossTenantAccessException(callerTenantId, requested, argName);
        }
        return callerTenantId;
    }

    private static String extract(Map<String, Object> args, String argName) {
        if (args == null) {
            return null;
        }
        Object value = args.get(argName);
        if (value == null) {
            return null;
        }
        String s = value.toString();
        return s.isBlank() ? null : s;
    }
}
