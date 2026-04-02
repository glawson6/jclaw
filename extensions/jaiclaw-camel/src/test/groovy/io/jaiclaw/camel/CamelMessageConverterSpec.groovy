package io.jaiclaw.camel

import io.jaiclaw.channel.ChannelMessage
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import spock.lang.Specification

class CamelMessageConverterSpec extends Specification {

    CamelContext camelContext = new DefaultCamelContext()

    def "toChannelMessage extracts body as content"() {
        given:
        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody("Hello from Camel")

        when:
        ChannelMessage message = CamelMessageConverter.toChannelMessage(exchange, "s3", "acct1")

        then:
        message.content() == "Hello from Camel"
        message.channelId() == "s3"
        message.accountId() == "acct1"
        message.direction() == ChannelMessage.Direction.INBOUND
    }

    def "toChannelMessage uses peerId header with anonymous default"() {
        given:
        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody("test")

        expect: "anonymous when no header"
        CamelMessageConverter.toChannelMessage(exchange, "ch", "acct").peerId() == "anonymous"

        when: "header is set"
        exchange.getIn().setHeader(CamelMessageConverter.HEADER_PEER_ID, "user42")

        then:
        CamelMessageConverter.toChannelMessage(exchange, "ch", "acct").peerId() == "user42"
    }

    def "toChannelMessage copies all Camel headers into platformData"() {
        given:
        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody("data")
        exchange.getIn().setHeader("CamelAwsS3Key", "reports/jan.csv")
        exchange.getIn().setHeader("CamelAwsS3BucketName", "my-bucket")

        when:
        ChannelMessage message = CamelMessageConverter.toChannelMessage(exchange, "s3", "acct")

        then:
        message.platformData().get("CamelAwsS3Key") == "reports/jan.csv"
        message.platformData().get("CamelAwsS3BucketName") == "my-bucket"
    }

    def "toChannelMessage carries PipelineEnvelope in platformData"() {
        given:
        Exchange exchange = new DefaultExchange(camelContext)
        exchange.getIn().setBody("pipeline msg")
        def envelope = new PipelineEnvelope("p1", "c1", 0, 3, "reply-ch", "peer1", [])
        exchange.getIn().setHeader(CamelMessageConverter.HEADER_PIPELINE, envelope)

        when:
        ChannelMessage message = CamelMessageConverter.toChannelMessage(exchange, "kafka", "acct")

        then:
        message.platformData().get("pipeline") instanceof PipelineEnvelope
        PipelineEnvelope carried = (PipelineEnvelope) message.platformData().get("pipeline")
        carried.pipelineId() == "p1"
        carried.totalStages() == 3
    }

    def "toExchange sets body and standard headers"() {
        given:
        ChannelMessage message = ChannelMessage.inbound("msg-1", "kafka", "acct1", "user42", "Hello", Map.of())

        when:
        Exchange exchange = CamelMessageConverter.toExchange(message, camelContext)

        then:
        exchange.getIn().getBody(String.class) == "Hello"
        exchange.getIn().getHeader(CamelMessageConverter.HEADER_CHANNEL_ID) == "kafka"
        exchange.getIn().getHeader(CamelMessageConverter.HEADER_PEER_ID) == "user42"
        exchange.getIn().getHeader(CamelMessageConverter.HEADER_SESSION_KEY) == "default:kafka:acct1:user42"
    }

    def "toExchange forwards PipelineEnvelope from platformData"() {
        given:
        def envelope = new PipelineEnvelope("p1", "c1", 1, 3, null, null, ["out0"])
        ChannelMessage message = ChannelMessage.inbound("msg-1", "s3", "acct", "user", "data", Map.of("pipeline", envelope))

        when:
        Exchange exchange = CamelMessageConverter.toExchange(message, camelContext)

        then:
        exchange.getIn().getHeader(CamelMessageConverter.HEADER_PIPELINE) instanceof PipelineEnvelope
        PipelineEnvelope carried = (PipelineEnvelope) exchange.getIn().getHeader(CamelMessageConverter.HEADER_PIPELINE)
        carried.stageIndex() == 1
        carried.stageOutputs() == ["out0"]
    }

    def "round-trip preserves key fields"() {
        given:
        ChannelMessage original = ChannelMessage.inbound("id-1", "kafka", "acct", "peer", "round-trip", Map.of())

        when:
        Exchange exchange = CamelMessageConverter.toExchange(original, camelContext)
        ChannelMessage roundTripped = CamelMessageConverter.toChannelMessage(exchange, "kafka", "acct")

        then:
        roundTripped.content() == "round-trip"
        roundTripped.channelId() == "kafka"
        roundTripped.accountId() == "acct"
        roundTripped.peerId() == "peer"
    }
}
