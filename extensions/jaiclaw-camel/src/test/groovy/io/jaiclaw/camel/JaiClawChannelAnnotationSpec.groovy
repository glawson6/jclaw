package io.jaiclaw.camel

import org.apache.camel.builder.RouteBuilder
import spock.lang.Specification

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class JaiClawChannelAnnotationSpec extends Specification {

    @JaiClawChannel(channelId = "s3-ingest", displayName = "S3 Ingestion",
            accountId = "acct1", outboundUri = "kafka:results")
    static class FullyAnnotatedRoute extends RouteBuilder {
        @Override
        void configure() {}
    }

    @JaiClawChannel(channelId = "minimal")
    static class MinimalAnnotatedRoute extends RouteBuilder {
        @Override
        void configure() {}
    }

    @JaiClawChannel(channelId = "cross-channel", outbound = "telegram")
    static class CrossChannelRoute extends RouteBuilder {
        @Override
        void configure() {}
    }

    def "annotation is retained at runtime"() {
        expect:
        FullyAnnotatedRoute.getAnnotation(JaiClawChannel) != null
    }

    def "annotation values are accessible at runtime"() {
        given:
        JaiClawChannel annotation = FullyAnnotatedRoute.getAnnotation(JaiClawChannel)

        expect:
        annotation.channelId() == "s3-ingest"
        annotation.displayName() == "S3 Ingestion"
        annotation.accountId() == "acct1"
        annotation.outboundUri() == "kafka:results"
        annotation.outbound() == ""
    }

    def "default values are empty strings"() {
        given:
        JaiClawChannel annotation = MinimalAnnotatedRoute.getAnnotation(JaiClawChannel)

        expect:
        annotation.channelId() == "minimal"
        annotation.displayName() == ""
        annotation.accountId() == ""
        annotation.outboundUri() == ""
        annotation.outbound() == ""
    }

    def "outbound channel name is accessible"() {
        given:
        JaiClawChannel annotation = CrossChannelRoute.getAnnotation(JaiClawChannel)

        expect:
        annotation.channelId() == "cross-channel"
        annotation.outbound() == "telegram"
        annotation.outboundUri() == ""
    }

    def "annotation has RUNTIME retention"() {
        given:
        Retention retention = JaiClawChannel.getAnnotation(Retention)

        expect:
        retention.value() == RetentionPolicy.RUNTIME
    }
}
