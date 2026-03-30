package io.jaiclaw.memory

import io.jaiclaw.core.tenant.DefaultTenantContext
import io.jaiclaw.core.tenant.TenantContextHolder
import io.jaiclaw.core.tenant.TenantGuard
import io.jaiclaw.core.tenant.TenantMode
import io.jaiclaw.core.tenant.TenantProperties
import spock.lang.Specification

class InMemorySearchManagerMultiTenantSpec extends Specification {

    def multiGuard = new TenantGuard(new TenantProperties(TenantMode.MULTI, "default"))
    def manager = new InMemorySearchManager(multiGuard)

    def setTenant(String id) {
        TenantContextHolder.set(new DefaultTenantContext(id, id))
    }

    def cleanup() {
        TenantContextHolder.clear()
    }

    def "index for tenant A, search as tenant B returns empty"() {
        given:
        setTenant("tenant-a")
        manager.addEntry("notes/prospect.md", "recruiting prospect evaluation notes")

        when:
        setTenant("tenant-b")
        def results = manager.search("recruiting", new MemorySearchOptions(10, 0.0, null))

        then:
        results.isEmpty()
    }

    def "fail-closed: MULTI mode with no tenant context returns empty"() {
        given:
        setTenant("tenant-a")
        manager.addEntry("notes/data.md", "important data for tenant")
        TenantContextHolder.clear()

        when:
        def results = manager.search("important", new MemorySearchOptions(10, 0.0, null))

        then:
        // In MULTI mode without tenant context, requireTenantIfMulti() throws
        thrown(IllegalStateException)
    }

    def "each tenant sees only their own entries"() {
        given:
        setTenant("tenant-a")
        manager.addEntry("a-notes/scouting.md", "basketball scouting report for player")
        manager.addEntry("a-notes/eval.md", "basketball evaluation metrics")

        setTenant("tenant-b")
        manager.addEntry("b-notes/plan.md", "basketball training plan")

        when:
        setTenant("tenant-a")
        def resultsA = manager.search("basketball", new MemorySearchOptions(10, 0.0, null))

        then:
        resultsA.size() == 2

        when:
        setTenant("tenant-b")
        def resultsB = manager.search("basketball", new MemorySearchOptions(10, 0.0, null))

        then:
        resultsB.size() == 1
    }
}
