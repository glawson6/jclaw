package io.jaiclaw.camel;

import io.jaiclaw.channel.ChannelMessage;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Camel {@link Processor} that invokes a JaiClaw agent synchronously via {@link GatewayServiceAccessor}.
 *
 * <p>Intended for manual use in user-defined Camel route configurations. Not auto-wired
 * because that would require a compile-time dependency on the gateway module.
 */
public class AgentProcessor implements Processor {

    private final GatewayServiceAccessor gateway;
    private final String channelId;
    private final String accountId;

    public AgentProcessor(GatewayServiceAccessor gateway, String channelId, String accountId) {
        this.gateway = gateway;
        this.channelId = channelId;
        this.accountId = accountId;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        ChannelMessage message = CamelMessageConverter.toChannelMessage(exchange, channelId, accountId);
        String response = gateway.handleSync(channelId, accountId, message.peerId(), message.content());
        exchange.getIn().setBody(response);

        // Advance pipeline envelope if present
        Object pipeline = exchange.getIn().getHeader(CamelMessageConverter.HEADER_PIPELINE);
        if (pipeline instanceof PipelineEnvelope envelope) {
            exchange.getIn().setHeader(CamelMessageConverter.HEADER_PIPELINE, envelope.nextStage(response));
        }
    }
}
