package io.jaiclaw.config

import spock.lang.Specification

class HttpProxyPropertiesSpec extends Specification {

    def "DEFAULT has no proxy configured"() {
        expect:
        !HttpProxyProperties.DEFAULT.isConfigured()
        HttpProxyProperties.DEFAULT.host() == null
        HttpProxyProperties.DEFAULT.port() == 0
        HttpProxyProperties.DEFAULT.username() == null
        HttpProxyProperties.DEFAULT.password() == null
        HttpProxyProperties.DEFAULT.nonProxyHosts() == ""
    }

    def "isConfigured returns true when host and port are set"() {
        when:
        def props = new HttpProxyProperties("proxy.corp.com", 8080, null, null, null)

        then:
        props.isConfigured()
    }

    def "isConfigured returns false when host is blank"() {
        expect:
        !new HttpProxyProperties("", 8080, null, null, null).isConfigured()
        !new HttpProxyProperties("  ", 8080, null, null, null).isConfigured()
    }

    def "isConfigured returns false when port is zero"() {
        expect:
        !new HttpProxyProperties("proxy.corp.com", 0, null, null, null).isConfigured()
    }

    def "compact constructor defaults null nonProxyHosts to empty string"() {
        when:
        def props = new HttpProxyProperties("proxy.corp.com", 8080, "user", "pass", null)

        then:
        props.nonProxyHosts() == ""
    }

    def "preserves non-null nonProxyHosts"() {
        when:
        def props = new HttpProxyProperties("proxy.corp.com", 8080, null, null, "localhost,127.0.0.1")

        then:
        props.nonProxyHosts() == "localhost,127.0.0.1"
    }

    def "HttpProperties defaults proxy to DEFAULT"() {
        expect:
        HttpProperties.DEFAULT.proxy() == HttpProxyProperties.DEFAULT
    }

    def "HttpProperties compact constructor defaults null proxy"() {
        when:
        def props = new HttpProperties(null)

        then:
        props.proxy() == HttpProxyProperties.DEFAULT
    }
}
