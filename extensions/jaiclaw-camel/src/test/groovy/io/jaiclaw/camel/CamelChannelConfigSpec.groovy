package io.jaiclaw.camel

import spock.lang.Specification

class CamelChannelConfigSpec extends Specification {

    def "rejects null channelId"() {
        when:
        new CamelChannelConfig(null, "Display", "acct", "direct:out", null, null, false)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects blank channelId"() {
        when:
        new CamelChannelConfig("  ", "Display", "acct", "direct:out", null, null, false)

        then:
        thrown(IllegalArgumentException)
    }

    def "defaults displayName when null"() {
        when:
        def config = new CamelChannelConfig("s3-ingest", null, "acct", null, null, null, false)

        then:
        config.displayName() == "Camel: s3-ingest"
    }

    def "defaults displayName when blank"() {
        when:
        def config = new CamelChannelConfig("kafka", "  ", "acct", null, null, null, false)

        then:
        config.displayName() == "Camel: kafka"
    }

    def "defaults accountId when null"() {
        when:
        def config = new CamelChannelConfig("s3", "S3", null, null, null, null, false)

        then:
        config.accountId() == "default"
    }

    def "defaults accountId when blank"() {
        when:
        def config = new CamelChannelConfig("s3", "S3", "  ", null, null, null, false)

        then:
        config.accountId() == "default"
    }

    def "preserves all values when fully specified"() {
        when:
        def config = new CamelChannelConfig("kafka", "Kafka Events", "tenant-1", "kafka:output-topic", "aws2-s3://inbox", null, false)

        then:
        config.channelId() == "kafka"
        config.displayName() == "Kafka Events"
        config.accountId() == "tenant-1"
        config.outboundUri() == "kafka:output-topic"
        config.inboundUri() == "aws2-s3://inbox"
        config.outbound() == null
    }

    def "allows null outboundUri for inbound-only channels"() {
        when:
        def config = new CamelChannelConfig("s3-watch", "S3 Watcher", "default", null, null, null, false)

        then:
        config.outboundUri() == null
    }

    def "accepts inboundUri"() {
        when:
        def config = new CamelChannelConfig("s3-ingest", null, null, null, "aws2-s3://docs-inbox", null, false)

        then:
        config.inboundUri() == "aws2-s3://docs-inbox"
    }

    def "accepts outbound channel name"() {
        when:
        def config = new CamelChannelConfig("kafka-analyze", null, null, null, null, "telegram", false)

        then:
        config.outbound() == "telegram"
    }

    def "rejects both outboundUri and outbound simultaneously"() {
        when:
        new CamelChannelConfig("bad", null, null, "kafka:output", null, "telegram", false)

        then:
        thrown(IllegalArgumentException)
    }

    def "allows both outboundUri and outbound null"() {
        when:
        def config = new CamelChannelConfig("inbound-only", null, null, null, null, null, false)

        then:
        config.outboundUri() == null
        config.outbound() == null
    }

    def "stateless defaults to false with canonical constructor"() {
        when:
        def config = new CamelChannelConfig("ch1", null, null, null, null, null, false)

        then:
        !config.stateless()
    }

    def "stateless can be set true with canonical constructor"() {
        when:
        def config = new CamelChannelConfig("ch1", null, null, null, null, null, true)

        then:
        config.stateless()
    }

    def "stateless false with canonical constructor"() {
        when:
        def config = new CamelChannelConfig("ch1", null, null, null, null, null, false)

        then:
        !config.stateless()
    }
}
