package io.jaiclaw.example.camel.pdffiller;

import io.jaiclaw.core.artifact.ArtifactStatus;
import io.jaiclaw.core.artifact.ArtifactStore;
import io.jaiclaw.core.artifact.StoredArtifact;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/**
 * Outbound route: consumes the agent's response after tool-based PDF filling.
 *
 * <p>The agent uses {@code pdf_fill_form} to write the filled PDF directly to
 * the outbox. This route reads the output file and stores it in the ArtifactStore
 * for retrieval via the REST API.
 */
@Configuration
public class PdfOutputRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(PdfOutputRoute.class);

    private final ArtifactStore artifactStore;
    private final String outbox;

    public PdfOutputRoute(
            ArtifactStore artifactStore,
            @Value("${app.outbox:target/data/outbox}") String outbox) {
        this.artifactStore = artifactStore;
        this.outbox = outbox;
    }

    @Override
    public void configure() {
        from("seda:jaiclaw-pdf-filler-out")
                .routeId("pdf-output")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String peerId = exchange.getIn().getHeader("JaiClawPeerId", String.class);
                        String body = exchange.getIn().getBody(String.class);
                        processAgentResponse(peerId, body);
                    }
                });
    }

    private void processAgentResponse(String peerId, String agentResponse) {
        try {
            Path expectedOutput = Path.of(outbox).resolve(peerId + ".pdf");

            if (Files.exists(expectedOutput)) {
                byte[] pdfBytes = Files.readAllBytes(expectedOutput);
                StoredArtifact artifact = new StoredArtifact(
                        peerId, pdfBytes, "application/pdf",
                        peerId + ".pdf", ArtifactStatus.COMPLETED, null,
                        Instant.now(), Map.of());
                artifactStore.save(artifact);
                log.info("Stored filled PDF for [{}] ({} bytes) -> {}",
                        peerId, pdfBytes.length, expectedOutput);
            } else {
                // Agent may have reported an error or used a different path
                artifactStore.updateStatus(peerId, ArtifactStatus.FAILED,
                        "Output PDF not found at expected path: " + expectedOutput
                                + ". Agent response: " + truncate(agentResponse, 500));
                log.error("Output PDF not found for [{}] at {}", peerId, expectedOutput);
            }
        } catch (Exception e) {
            artifactStore.updateStatus(peerId, ArtifactStatus.FAILED, e.getMessage());
            log.error("Error processing agent response for [{}]", peerId, e);
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
