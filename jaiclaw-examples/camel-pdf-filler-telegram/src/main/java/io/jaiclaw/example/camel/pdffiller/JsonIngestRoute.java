package io.jaiclaw.example.camel.pdffiller;

import io.jaiclaw.camel.CamelMessageConverter;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Inbound route: watches the configured inbox directory for JSON files and
 * forwards them to the SEDA inbound queue for agent processing.
 *
 * <p>The agent uses the {@code pdf-form-filler} skill and its tools
 * ({@code pdf_read_fields}, {@code pdf_fill_form}) to inspect the template,
 * map fields, and produce the filled PDF autonomously. For ambiguous mappings,
 * the stateful Telegram channel allows human-in-the-loop clarification.
 */
@Configuration
public class JsonIngestRoute extends RouteBuilder {

    private final String inbox;
    private final String outbox;
    private final TemplateManager templateManager;

    public JsonIngestRoute(
            @Value("${app.inbox:target/data/inbox}") String inbox,
            @Value("${app.outbox:target/data/outbox}") String outbox,
            TemplateManager templateManager) {
        this.inbox = inbox;
        this.outbox = outbox;
        this.templateManager = templateManager;
    }

    @Override
    public void configure() {
        fromF("file:%s?include=.*\\.json&move=.done/${file:name}&readLock=changed", inbox)
                .routeId("json-file-ingest")
                .setHeader(CamelMessageConverter.HEADER_PEER_ID,
                        simple("${file:name.noext}"))
                .process(exchange -> {
                    String peerId = exchange.getIn().getHeader(
                            CamelMessageConverter.HEADER_PEER_ID, String.class);
                    String jsonBody = exchange.getIn().getBody(String.class);
                    String templatePath = templateManager.getTemplatePath();
                    String outputPath = outbox + "/" + peerId + ".pdf";
                    String message = "Fill the PDF template at " + templatePath
                            + " with the following data and write the output to "
                            + outputPath + ":\n\n" + jsonBody;
                    exchange.getIn().setBody(message);
                })
                .to("seda:jaiclaw-pdf-filler-telegram-in");
    }
}
