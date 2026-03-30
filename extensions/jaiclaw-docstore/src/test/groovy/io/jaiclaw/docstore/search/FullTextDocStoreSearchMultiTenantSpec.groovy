package io.jaiclaw.docstore.search

import io.jaiclaw.core.tenant.DefaultTenantContext
import io.jaiclaw.core.tenant.TenantContextHolder
import io.jaiclaw.core.tenant.TenantGuard
import io.jaiclaw.core.tenant.TenantMode
import io.jaiclaw.core.tenant.TenantProperties
import io.jaiclaw.docstore.model.DocStoreEntry
import spock.lang.Specification

class FullTextDocStoreSearchMultiTenantSpec extends Specification {

    def multiGuard = new TenantGuard(new TenantProperties(TenantMode.MULTI, "default"))
    def search = new FullTextDocStoreSearch(multiGuard)

    def setTenant(String id) {
        TenantContextHolder.set(new DefaultTenantContext(id, id))
    }

    def cleanup() {
        TenantContextHolder.clear()
    }

    private DocStoreEntry makeEntry(String id, String tenantId, String description) {
        DocStoreEntry.builder()
                .id(id)
                .entryType(DocStoreEntry.EntryType.FILE)
                .filename("file-${id}.txt")
                .description(description)
                .tenantId(tenantId)
                .build()
    }

    def "index as tenant A, search as tenant B returns empty"() {
        given:
        setTenant("tenant-a")
        search.index(makeEntry("doc-1", "tenant-a", "recruiting prospect evaluation"))

        when:
        setTenant("tenant-b")
        def results = search.search("recruiting", DocStoreSearchOptions.DEFAULT)

        then:
        results.isEmpty()
    }

    def "index for both tenants, each sees only their own results"() {
        given:
        setTenant("tenant-a")
        search.index(makeEntry("a-doc-1", "tenant-a", "basketball scouting report"))
        search.index(makeEntry("a-doc-2", "tenant-a", "basketball player evaluation"))

        setTenant("tenant-b")
        search.index(makeEntry("b-doc-1", "tenant-b", "basketball training program"))

        when:
        setTenant("tenant-a")
        def resultsA = search.search("basketball", DocStoreSearchOptions.DEFAULT)

        then:
        resultsA.size() == 2
        resultsA.every { it.entry().tenantId() == "tenant-a" }

        when:
        setTenant("tenant-b")
        def resultsB = search.search("basketball", DocStoreSearchOptions.DEFAULT)

        then:
        resultsB.size() == 1
        resultsB[0].entry().tenantId() == "tenant-b"
    }
}
