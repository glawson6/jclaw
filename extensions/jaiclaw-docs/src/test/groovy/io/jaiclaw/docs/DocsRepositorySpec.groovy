package io.jaiclaw.docs

import spock.lang.Specification

class DocsRepositorySpec extends Specification {

    def "findAll returns all entries"() {
        given:
        def entries = [
            new DocsEntry("docs://arch", "architecture", "text/markdown", "# Architecture", ["architecture"]),
            new DocsEntry("docs://ops", "operations", "text/markdown", "# Operations", ["operations"])
        ]
        def repo = new DocsRepository(entries)

        expect:
        repo.findAll().size() == 2
        repo.size() == 2
    }

    def "findByUri returns matching entry"() {
        given:
        def entries = [
            new DocsEntry("docs://arch", "architecture", "text/markdown", "# Architecture", ["architecture"])
        ]
        def repo = new DocsRepository(entries)

        expect:
        repo.findByUri("docs://arch").isPresent()
        repo.findByUri("docs://arch").get().name() == "architecture"
    }

    def "findByUri returns empty for unknown URI"() {
        given:
        def repo = new DocsRepository([])

        expect:
        repo.findByUri("docs://nonexistent").isEmpty()
    }

    def "search returns results matching content"() {
        given:
        def entries = [
            new DocsEntry("docs://arch", "architecture", "text/markdown",
                "JaiClaw architecture uses multi-tenancy and virtual threads", ["architecture"]),
            new DocsEntry("docs://ops", "operations", "text/markdown",
                "Operations guide for deploying JaiClaw", ["operations"]),
            new DocsEntry("docs://tenant", "multi tenancy", "text/markdown",
                "Multi-tenancy configuration and tenant isolation", ["multi", "tenancy"])
        ]
        def repo = new DocsRepository(entries)

        when:
        def results = repo.search("multi-tenancy", 5)

        then:
        results.size() >= 1
        results.any { it.uri() == "docs://tenant" }
    }

    def "search respects maxResults"() {
        given:
        def entries = (1..10).collect {
            new DocsEntry("docs://doc$it", "doc $it", "text/markdown", "common keyword here", ["doc"])
        }
        def repo = new DocsRepository(entries)

        when:
        def results = repo.search("keyword", 3)

        then:
        results.size() == 3
    }

    def "search returns empty for blank query"() {
        given:
        def entries = [
            new DocsEntry("docs://arch", "architecture", "text/markdown", "content", ["arch"])
        ]
        def repo = new DocsRepository(entries)

        expect:
        repo.search("", 5).isEmpty()
        repo.search(null, 5).isEmpty()
        repo.search("   ", 5).isEmpty()
    }

    def "search ranks name matches higher than content matches"() {
        given:
        def entries = [
            new DocsEntry("docs://auth", "authentication", "text/markdown",
                "Some unrelated content", ["authentication"]),
            new DocsEntry("docs://other", "other doc", "text/markdown",
                "This mentions authentication briefly in the content", ["other"])
        ]
        def repo = new DocsRepository(entries)

        when:
        def results = repo.search("authentication", 5)

        then:
        results.size() == 2
        results[0].uri() == "docs://auth"
    }

    def "search returns no results when nothing matches"() {
        given:
        def entries = [
            new DocsEntry("docs://arch", "architecture", "text/markdown", "content here", ["arch"])
        ]
        def repo = new DocsRepository(entries)

        expect:
        repo.search("zzzznonexistent", 5).isEmpty()
    }

    def "classpath loading works when run from test context"() {
        given: "default constructor scans classpath:docs/**/*.md"
        def repo = new DocsRepository()

        expect: "at least some docs are loaded from the classpath (bundled in src/main/resources)"
        repo.size() > 0
    }
}
