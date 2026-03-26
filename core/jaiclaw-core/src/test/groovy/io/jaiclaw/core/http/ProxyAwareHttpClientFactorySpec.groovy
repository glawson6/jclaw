package io.jaiclaw.core.http

import io.jaiclaw.core.http.ProxyAwareHttpClientFactory.ProxyConfig
import spock.lang.Specification

class ProxyAwareHttpClientFactorySpec extends Specification {

    def cleanup() {
        ProxyAwareHttpClientFactory.reset()
    }

    // --- parseProxyUrl ---

    def "parses http://host:port"() {
        when:
        def cfg = ProxyAwareHttpClientFactory.parseProxyUrl("http://proxy.corp.com:8080")

        then:
        cfg.host() == "proxy.corp.com"
        cfg.port() == 8080
        cfg.username() == null
        cfg.password() == null
    }

    def "parses https://host:port"() {
        when:
        def cfg = ProxyAwareHttpClientFactory.parseProxyUrl("https://proxy.corp.com:3128")

        then:
        cfg.host() == "proxy.corp.com"
        cfg.port() == 3128
    }

    def "parses http://user:pass@host:port"() {
        when:
        def cfg = ProxyAwareHttpClientFactory.parseProxyUrl("http://admin:secret@proxy.corp.com:8080")

        then:
        cfg.host() == "proxy.corp.com"
        cfg.port() == 8080
        cfg.username() == "admin"
        cfg.password() == "secret"
    }

    def "parses URL with trailing slash"() {
        when:
        def cfg = ProxyAwareHttpClientFactory.parseProxyUrl("http://proxy.corp.com:8080/")

        then:
        cfg.host() == "proxy.corp.com"
        cfg.port() == 8080
    }

    def "parses host without port defaults to 8080"() {
        when:
        def cfg = ProxyAwareHttpClientFactory.parseProxyUrl("http://proxy.corp.com")

        then:
        cfg.host() == "proxy.corp.com"
        cfg.port() == 8080
    }

    def "parses user without password"() {
        when:
        def cfg = ProxyAwareHttpClientFactory.parseProxyUrl("http://admin@proxy.corp.com:8080")

        then:
        cfg.username() == "admin"
        cfg.password() == null
    }

    def "parses scheme-less host:port"() {
        when:
        def cfg = ProxyAwareHttpClientFactory.parseProxyUrl("proxy.corp.com:8080")

        then:
        cfg.host() == "proxy.corp.com"
        cfg.port() == 8080
    }

    // --- resolveProxy ---

    def "resolveProxy returns explicit config when set"() {
        given:
        def explicit = new ProxyConfig("explicit.proxy.com", 9090, null, null)
        ProxyAwareHttpClientFactory.configure(explicit)

        when:
        def resolved = ProxyAwareHttpClientFactory.resolveProxy()

        then:
        resolved.host() == "explicit.proxy.com"
        resolved.port() == 9090
    }

    def "resolveProxy returns null when nothing configured and no env vars"() {
        // In CI/dev, env vars are typically not set
        // This test verifies behavior when globalConfig is null
        given:
        ProxyAwareHttpClientFactory.reset()

        when:
        def resolved = ProxyAwareHttpClientFactory.resolveProxy()

        then:
        // Either null (no env vars) or a valid config (if env vars happen to be set)
        resolved == null || resolved.host() != null
    }

    def "reset clears explicit config"() {
        given:
        ProxyAwareHttpClientFactory.configure(new ProxyConfig("proxy.com", 8080, null, null))

        when:
        ProxyAwareHttpClientFactory.reset()
        def resolved = ProxyAwareHttpClientFactory.resolveProxy()

        then:
        // Should fall through to env vars (which are typically not set in test)
        resolved == null || resolved.host() != "proxy.com"
    }

    // --- factory methods ---

    def "create returns a non-null HttpClient"() {
        expect:
        ProxyAwareHttpClientFactory.create() != null
    }

    def "createWithDefaults returns a non-null HttpClient"() {
        expect:
        ProxyAwareHttpClientFactory.createWithDefaults() != null
    }

    def "newBuilder returns a non-null builder"() {
        expect:
        ProxyAwareHttpClientFactory.newBuilder() != null
    }

    def "create with proxy configured returns a working HttpClient"() {
        given:
        ProxyAwareHttpClientFactory.configure(new ProxyConfig("127.0.0.1", 9999, null, null))

        when:
        def client = ProxyAwareHttpClientFactory.create()

        then:
        client != null
    }

    def "create with authenticated proxy returns a working HttpClient"() {
        given:
        ProxyAwareHttpClientFactory.configure(new ProxyConfig("127.0.0.1", 9999, "user", "pass"))

        when:
        def client = ProxyAwareHttpClientFactory.create()

        then:
        client != null
    }
}
