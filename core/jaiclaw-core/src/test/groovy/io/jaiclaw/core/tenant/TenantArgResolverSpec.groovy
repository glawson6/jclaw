package io.jaiclaw.core.tenant

import spock.lang.Specification

/**
 * Validates {@link TenantArgResolver#resolveOrValidate} — the shared helper
 * MCP tool providers use to defend against caller-supplied {@code tenantId}
 * arguments overriding their real {@link TenantContext}.
 */
class TenantArgResolverSpec extends Specification {

    static TenantContext ctxFor(String id) {
        return new DefaultTenantContext(id, id)
    }

    def "returns caller tenant when args omit the field"() {
        given:
        def caller = ctxFor("tenant-A")

        expect:
        TenantArgResolver.resolveOrValidate(caller, [:], "tenantId", "default") == "tenant-A"
        TenantArgResolver.resolveOrValidate(caller, [tenantId: null], "tenantId", "default") == "tenant-A"
        TenantArgResolver.resolveOrValidate(caller, [tenantId: ""], "tenantId", "default") == "tenant-A"
        TenantArgResolver.resolveOrValidate(caller, [tenantId: "   "], "tenantId", "default") == "tenant-A"
    }

    def "returns caller tenant when args field matches"() {
        given:
        def caller = ctxFor("tenant-A")

        expect:
        TenantArgResolver.resolveOrValidate(caller, [tenantId: "tenant-A"], "tenantId", "default") == "tenant-A"
    }

    def "throws CrossTenantAccessException when args tenant differs from caller"() {
        given:
        def caller = ctxFor("tenant-A")

        when:
        TenantArgResolver.resolveOrValidate(caller, [tenantId: "tenant-B"], "tenantId", "default")

        then:
        def ex = thrown(CrossTenantAccessException)
        ex.callerTenantId() == "tenant-A"
        ex.requestedTenantId() == "tenant-B"
        ex.argName() == "tenantId"
    }

    def "falls back to defaultTenant when caller is null and no arg provided"() {
        expect:
        TenantArgResolver.resolveOrValidate(null, [:], "tenantId", "default") == "default"
        TenantArgResolver.resolveOrValidate(null, null, "tenantId", "default") == "default"
    }

    def "accepts null-caller + matching-default arg"() {
        expect:
        TenantArgResolver.resolveOrValidate(null, [tenantId: "default"], "tenantId", "default") == "default"
    }

    def "throws when caller is null and arg specifies a non-default tenant"() {
        when:
        TenantArgResolver.resolveOrValidate(null, [tenantId: "sneaky"], "tenantId", "default")

        then:
        def ex = thrown(CrossTenantAccessException)
        ex.callerTenantId() == "default"
        ex.requestedTenantId() == "sneaky"
    }

    def "coerces non-string arg values via toString"() {
        given:
        def caller = ctxFor("tenant-A")

        expect: "numeric arg toString => 'tenant-A' would never match a real tenant id, expected to throw"
        // Any Object.toString() branch is exercised by CrossTenantAccessException detection
        when:
        TenantArgResolver.resolveOrValidate(caller, [tenantId: 42], "tenantId", "default")

        then:
        thrown(CrossTenantAccessException)
    }
}
