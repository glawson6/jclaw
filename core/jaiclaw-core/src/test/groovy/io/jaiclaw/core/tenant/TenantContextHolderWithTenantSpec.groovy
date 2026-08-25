package io.jaiclaw.core.tenant

import spock.lang.Specification

/**
 * Validates {@link TenantContextHolder#withTenant} save/restore semantics.
 * The naive {@code set → try → clear} pattern clobbers an outer caller's
 * tenant on {@code finally}; the utility must preserve it.
 */
class TenantContextHolderWithTenantSpec extends Specification {

    def setup() {
        TenantContextHolder.clear()
    }

    def cleanup() {
        TenantContextHolder.clear()
    }

    def "sets tenant for the duration of the action and clears afterwards when no prior context"() {
        given:
        def inner = new DefaultTenantContext("tenant-A", "A")
        assert TenantContextHolder.get() == null

        when:
        def captured = TenantContextHolder.withTenant(inner, { -> TenantContextHolder.get() } as java.util.function.Supplier)

        then:
        captured.getTenantId() == "tenant-A"
        TenantContextHolder.get() == null
    }

    def "restores the prior tenant when one was set before the wrap"() {
        given:
        def outer = new DefaultTenantContext("tenant-outer", "outer")
        def inner = new DefaultTenantContext("tenant-inner", "inner")
        TenantContextHolder.set(outer)

        when:
        def captured = TenantContextHolder.withTenant(inner, { -> TenantContextHolder.get() } as java.util.function.Supplier)

        then:
        captured.getTenantId() == "tenant-inner"
        TenantContextHolder.get().getTenantId() == "tenant-outer"
    }

    def "restores the prior tenant when the action throws"() {
        given:
        def outer = new DefaultTenantContext("tenant-outer", "outer")
        def inner = new DefaultTenantContext("tenant-inner", "inner")
        TenantContextHolder.set(outer)

        when:
        TenantContextHolder.withTenant(inner, { -> throw new RuntimeException("boom") } as Runnable)

        then:
        thrown(RuntimeException)
        TenantContextHolder.get().getTenantId() == "tenant-outer"
    }

    def "leaves prior tenant untouched when passed a null context"() {
        given:
        def outer = new DefaultTenantContext("tenant-outer", "outer")
        TenantContextHolder.set(outer)

        when:
        def captured = TenantContextHolder.withTenant(null, { -> TenantContextHolder.get() } as java.util.function.Supplier)

        then:
        captured.getTenantId() == "tenant-outer"
        TenantContextHolder.get().getTenantId() == "tenant-outer"
    }

    def "Runnable overload delegates to Supplier version"() {
        given:
        def inner = new DefaultTenantContext("tenant-A", "A")
        def captured = new String[1]

        when:
        TenantContextHolder.withTenant(inner, { -> captured[0] = TenantContextHolder.get()?.getTenantId() } as Runnable)

        then:
        captured[0] == "tenant-A"
        TenantContextHolder.get() == null
    }
}
