package io.jaiclaw.examples.embabel.summarizer;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Outbound route: consumes structured summaries from the SEDA outbound queue
 * and logs the JSON output.
 */
@Configuration
public class SummaryLoggerRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(SummaryLoggerRoute.class);

    @Override
    public void configure() {
        from("seda:jaiclaw-embabel-html-summarizer-out")
                .routeId("summary-logger")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) {
                        String peerId = exchange.getIn().getHeader("JaiClawPeerId", String.class);
                        String body = exchange.getIn().getBody(String.class);
                        log.info("\n===== STRUCTURED SUMMARY [{}] =====\n{}\n===================================",
                                peerId, body);
                    }
                });
    }
}
