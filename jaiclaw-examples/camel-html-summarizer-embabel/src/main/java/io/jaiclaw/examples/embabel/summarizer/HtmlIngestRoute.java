package io.jaiclaw.examples.embabel.summarizer;

import io.jaiclaw.camel.CamelMessageConverter;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Inbound route: watches the inbox directory for HTML files, reads their
 * content, and forwards to the SEDA inbound queue for Embabel agent processing.
 *
 * <p>Processed files are moved to {@code {inbox}/.done/} to avoid re-processing.
 */
@Configuration
public class HtmlIngestRoute extends RouteBuilder {

    @Value("${app.inbox:target/data/inbox}")
    private String inbox;

    @Override
    public void configure() {
        fromF("file:%s?include=.*\\.html&move=.done/${file:name}&readLock=changed", inbox)
                .routeId("html-file-ingest")
                .setHeader(CamelMessageConverter.HEADER_PEER_ID,
                        simple("${file:name.noext}"))
                .to("seda:jaiclaw-embabel-html-summarizer-in");
    }
}
