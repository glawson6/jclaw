package io.jaiclaw.camel

import io.jaiclaw.channel.ChannelAdapter
import io.jaiclaw.channel.ChannelMessage
import io.jaiclaw.channel.ChannelMessageHandler
import io.jaiclaw.channel.ChannelRegistry
import io.jaiclaw.channel.DeliveryResult
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.impl.DefaultCamelContext
import spock.lang.AutoCleanup
import spock.lang.Specification

class CamelChannelAdapterSpec extends Specification {

    @AutoCleanup("stop")
    CamelContext camelContext = new DefaultCamelContext()

    def setup() {
        camelContext.start()
    }

    private CamelChannelAdapter createAdapter(String channelId, String outboundUri = null,
                                               String inboundUri = null, String outbound = null) {
        def config = new CamelChannelConfig(channelId, null, null, outboundUri, inboundUri, outbound, false)
        def producerTemplate = camelContext.createProducerTemplate()
        return new CamelChannelAdapter(config, producerTemplate, camelContext)
    }

    def "channelId and displayName from config"() {
        given:
        def config = new CamelChannelConfig("s3-ingest", "S3 Ingestion", "acct1", null, null, null, false)
        def adapter = new CamelChannelAdapter(config, camelContext.createProducerTemplate(), camelContext)

        expect:
        adapter.channelId() == "s3-ingest"
        adapter.displayName() == "S3 Ingestion"
    }

    def "inboundEndpoint returns SEDA URI"() {
        given:
        def adapter = createAdapter("kafka-events")

        expect:
        adapter.inboundEndpoint() == "seda:jaiclaw-kafka-events-in"
    }

    def "outboundEndpoint returns SEDA URI"() {
        given:
        def adapter = createAdapter("kafka-events")

        expect:
        adapter.outboundEndpoint() == "seda:jaiclaw-kafka-events-out"
    }

    def "isRunning lifecycle: false before start, true after start, false after stop"() {
        given:
        def adapter = createAdapter("test-ch")

        expect: "not running before start"
        !adapter.isRunning()

        when: "started"
        adapter.start(Mock(ChannelMessageHandler))

        then:
        adapter.isRunning()

        when: "stopped"
        adapter.stop()

        then:
        !adapter.isRunning()
    }

    def "start registers SEDA route and dispatches inbound messages to handler"() {
        given:
        def adapter = createAdapter("test-rt")
        def handler = Mock(ChannelMessageHandler)

        when:
        adapter.start(handler)
        def template = camelContext.createProducerTemplate()
        template.sendBodyAndHeader(adapter.inboundEndpoint(), "Hello agent",
                CamelMessageConverter.HEADER_PEER_ID, "user1")
        Thread.sleep(500)

        then:
        1 * handler.onMessage({ ChannelMessage msg ->
            msg.content() == "Hello agent" &&
            msg.channelId() == "test-rt" &&
            msg.peerId() == "user1"
        })
    }

    def "sendMessage always sends to SEDA outbound queue"() {
        given:
        def adapter = createAdapter("out-ch")
        adapter.start(Mock(ChannelMessageHandler))

        // Add a mock endpoint consuming from the outbound SEDA
        def mockUri = "mock:output-${System.nanoTime()}"
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() {
                from(adapter.outboundEndpoint())
                    .routeId("test-consumer-out")
                    .to(mockUri)
            }
        })

        def mockEndpoint = camelContext.getEndpoint(mockUri, MockEndpoint)
        mockEndpoint.expectedMessageCount(1)

        def message = ChannelMessage.outbound("msg-1", "out-ch", "acct", "peer1", "response text")

        when:
        DeliveryResult result = adapter.sendMessage(message)

        then:
        result instanceof DeliveryResult.Success
        ((DeliveryResult.Success) result).platformMessageId() == "msg-1"

        and:
        mockEndpoint.assertIsSatisfied(2000)
    }

    def "sendMessage succeeds even without outbound consumer configured"() {
        given: "adapter with no outbound bridge — SEDA queue always accepts"
        def adapter = createAdapter("no-out")
        adapter.start(Mock(ChannelMessageHandler))
        def message = ChannelMessage.outbound("msg-1", "no-out", "acct", "peer1", "text")

        when:
        DeliveryResult result = adapter.sendMessage(message)

        then:
        result instanceof DeliveryResult.Success
    }

    def "createInboundBridge creates route from source to SEDA in"() {
        given:
        def adapter = createAdapter("bridge-in")
        adapter.start(Mock(ChannelMessageHandler))

        def handler = Mock(ChannelMessageHandler)
        // Replace handler to capture the bridged message
        adapter.start(handler)

        def sourceUri = "direct:test-source-${System.nanoTime()}"

        when:
        adapter.createInboundBridge(sourceUri)
        def template = camelContext.createProducerTemplate()
        template.sendBodyAndHeader(sourceUri, "bridged message",
                CamelMessageConverter.HEADER_PEER_ID, "bridge-user")
        Thread.sleep(500)

        then:
        1 * handler.onMessage({ ChannelMessage msg ->
            msg.content() == "bridged message" &&
            msg.peerId() == "bridge-user"
        })
    }

    def "createOutboundUriBridge creates route from SEDA out to URI"() {
        given:
        def adapter = createAdapter("bridge-out-uri")
        adapter.start(Mock(ChannelMessageHandler))

        def mockUri = "mock:bridge-output-${System.nanoTime()}"
        adapter.createOutboundUriBridge(mockUri)

        def mockEndpoint = camelContext.getEndpoint(mockUri, MockEndpoint)
        mockEndpoint.expectedMessageCount(1)

        def message = ChannelMessage.outbound("msg-1", "bridge-out-uri", "acct", "peer1", "bridged response")

        when:
        adapter.sendMessage(message)

        then:
        mockEndpoint.assertIsSatisfied(2000)
    }

    def "createOutboundChannelBridge routes to target channel adapter"() {
        given:
        def adapter = createAdapter("bridge-ch")
        adapter.start(Mock(ChannelMessageHandler))

        def targetAdapter = Mock(ChannelAdapter) {
            channelId() >> "telegram"
            sendMessage(_) >> new DeliveryResult.Success("tg-1")
        }
        def registry = new ChannelRegistry()
        registry.register(targetAdapter)

        adapter.createOutboundChannelBridge(registry, "telegram")

        def message = ChannelMessage.outbound("msg-1", "bridge-ch", "acct", "peer1", "forward to telegram")

        when:
        adapter.sendMessage(message)
        Thread.sleep(500)

        then:
        1 * targetAdapter.sendMessage({ ChannelMessage msg ->
            msg.content() == "forward to telegram" &&
            msg.channelId() == "telegram"
        })
    }

    def "createLoggerFallback creates route from SEDA out to log"() {
        given:
        def adapter = createAdapter("log-ch")
        adapter.start(Mock(ChannelMessageHandler))

        when:
        adapter.createLoggerFallback()

        then: "route is created successfully"
        camelContext.getRoute("jaiclaw-log-log-ch") != null
    }

    def "stop removes all route types"() {
        given:
        def adapter = createAdapter("stop-all")
        adapter.start(Mock(ChannelMessageHandler))
        adapter.createInboundBridge("direct:stop-all-source")
        adapter.createLoggerFallback()

        expect:
        camelContext.getRoute("jaiclaw-in-stop-all") != null
        camelContext.getRoute("jaiclaw-in-bridge-stop-all") != null
        camelContext.getRoute("jaiclaw-log-stop-all") != null

        when:
        adapter.stop()

        then:
        camelContext.getRoute("jaiclaw-in-stop-all") == null
        camelContext.getRoute("jaiclaw-in-bridge-stop-all") == null
        camelContext.getRoute("jaiclaw-log-stop-all") == null
        !adapter.isRunning()
    }

    def "stop is safe to call before start"() {
        given:
        def adapter = createAdapter("never-started")

        when:
        adapter.stop()

        then:
        noExceptionThrown()
        !adapter.isRunning()
    }

    def "isStateless delegates to config stateless flag"() {
        given:
        def statelessConfig = new CamelChannelConfig("pipe", null, null, null, null, null, true)
        def statefulConfig = new CamelChannelConfig("chat", null, null, null, null, null, false)
        def statelessAdapter = new CamelChannelAdapter(statelessConfig, camelContext.createProducerTemplate(), camelContext)
        def statefulAdapter = new CamelChannelAdapter(statefulConfig, camelContext.createProducerTemplate(), camelContext)

        expect:
        statelessAdapter.isStateless()
        !statefulAdapter.isStateless()
    }

    def "isStateless defaults to false"() {
        given:
        def adapter = createAdapter("default-ch")

        expect:
        !adapter.isStateless()
    }
}
