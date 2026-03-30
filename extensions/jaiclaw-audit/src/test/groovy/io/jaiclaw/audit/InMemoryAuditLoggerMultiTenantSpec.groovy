package io.jaiclaw.audit

import io.jaiclaw.core.tenant.DefaultTenantContext
import io.jaiclaw.core.tenant.TenantContextHolder
import io.jaiclaw.core.tenant.TenantGuard
import io.jaiclaw.core.tenant.TenantMode
import io.jaiclaw.core.tenant.TenantProperties
import spock.lang.Specification

class InMemoryAuditLoggerMultiTenantSpec extends Specification {

    def multiGuard = new TenantGuard(new TenantProperties(TenantMode.MULTI, "default"))
    def logger = new InMemoryAuditLogger(multiGuard)

    def setTenant(String id) {
        TenantContextHolder.set(new DefaultTenantContext(id, id))
    }

    def cleanup() {
        TenantContextHolder.clear()
    }

    def "log() auto-stamps tenantId from TenantGuard"() {
        given:
        setTenant("tenant-a")
        def event = AuditEvent.builder()
                .id("evt-1")
                .action("test.action")
                .actor("user-1")
                .resource("res-1")
                .build()

        when:
        logger.log(event)

        then:
        def stored = logger.findById("evt-1")
        stored.isPresent()
        stored.get().tenantId() == "tenant-a"
    }

    def "query() returns only current tenant's events"() {
        given:
        setTenant("tenant-a")
        logger.log(AuditEvent.success("a-1", null, "user", "action", "res"))
        logger.log(AuditEvent.success("a-2", null, "user", "action", "res"))

        setTenant("tenant-b")
        logger.log(AuditEvent.success("b-1", null, "user", "action", "res"))

        when:
        setTenant("tenant-a")
        def eventsA = logger.query("tenant-a", 100)

        then:
        eventsA.size() == 2
        eventsA.every { it.tenantId() == "tenant-a" }

        when:
        setTenant("tenant-b")
        def eventsB = logger.query("tenant-b", 100)

        then:
        eventsB.size() == 1
        eventsB[0].tenantId() == "tenant-b"
    }

    def "findById returns empty for other tenant's event"() {
        given:
        setTenant("tenant-a")
        logger.log(AuditEvent.success("cross-1", null, "user", "action", "res"))

        when:
        setTenant("tenant-b")
        def result = logger.findById("cross-1")

        then:
        result.isEmpty()
    }

    def "count() counts only current tenant's events"() {
        given:
        setTenant("tenant-a")
        logger.log(AuditEvent.success("cnt-a1", null, "user", "action", "res"))
        logger.log(AuditEvent.success("cnt-a2", null, "user", "action", "res"))

        setTenant("tenant-b")
        logger.log(AuditEvent.success("cnt-b1", null, "user", "action", "res"))

        when:
        setTenant("tenant-a")
        def countA = logger.count("tenant-a")

        then:
        countA == 2

        when:
        setTenant("tenant-b")
        def countB = logger.count("tenant-b")

        then:
        countB == 1
    }
}
