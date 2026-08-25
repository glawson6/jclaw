package io.jaiclaw.core.tenant;

/**
 * Thrown when a caller supplies a tenant identifier (typically via a tool
 * argument) that does not match the tenant on their {@link TenantContext}.
 * MCP tool providers should catch this and translate it into a user-facing
 * error rather than leaking internal state.
 */
public class CrossTenantAccessException extends RuntimeException {

    private final String callerTenantId;
    private final String requestedTenantId;
    private final String argName;

    public CrossTenantAccessException(String callerTenantId, String requestedTenantId, String argName) {
        super("Cross-tenant access denied: caller tenant '" + callerTenantId
                + "' cannot act as tenant '" + requestedTenantId
                + "' (via argument '" + argName + "')");
        this.callerTenantId = callerTenantId;
        this.requestedTenantId = requestedTenantId;
        this.argName = argName;
    }

    public String callerTenantId() {
        return callerTenantId;
    }

    public String requestedTenantId() {
        return requestedTenantId;
    }

    public String argName() {
        return argName;
    }
}
