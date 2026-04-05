package io.jaiclaw.example.camel.pdffiller;

import io.jaiclaw.channel.ChannelAdapter;
import io.jaiclaw.channel.ChannelMessage;
import io.jaiclaw.channel.ChannelRegistry;
import io.jaiclaw.channel.DeliveryResult;
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
import java.util.UUID;

/**
 * Outbound route: consumes the agent's response after tool-based PDF filling
 * and sends a notification to Telegram.
 *
 * <p>The agent uses {@code pdf_fill_form} to write the filled PDF directly.
 * This route reads the output file, stores it in ArtifactStore, and notifies
 * the Telegram user. The stateful channel preserves conversation context for
 * human-in-the-loop clarification when the agent needs it.
 */
@Configuration
public class TelegramPdfOutputRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(TelegramPdfOutputRoute.class);

    private final ArtifactStore artifactStore;
    private final ChannelRegistry channelRegistry;
    private final String outbox;
    private final String chatId;

    public TelegramPdfOutputRoute(
            ArtifactStore artifactStore,
            ChannelRegistry channelRegistry,
            @Value("${app.outbox:target/data/outbox}") String outbox,
            @Value("${app.telegram.chat-id}") String chatId) {
        this.artifactStore = artifactStore;
        this.channelRegistry = channelRegistry;
        this.outbox = outbox;
        this.chatId = chatId;
    }

    @Override
    public void configure() {
        from("seda:jaiclaw-pdf-filler-telegram-out")
                .routeId("telegram-pdf-output")
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
                sendToTelegram("PDF filled for [" + peerId + "] (" + pdfBytes.length + " bytes).");
            } else {
                artifactStore.updateStatus(peerId, ArtifactStatus.FAILED,
                        "Output PDF not found at expected path: " + expectedOutput);
                sendToTelegram("PDF fill failed for [" + peerId
                        + "]: output file not found. Agent response: "
                        + truncate(agentResponse, 300));
                log.error("Output PDF not found for [{}] at {}", peerId, expectedOutput);
            }
        } catch (Exception e) {
            artifactStore.updateStatus(peerId, ArtifactStatus.FAILED, e.getMessage());
            log.error("Error processing agent response for [{}]", peerId, e);
        }
    }

    private void sendToTelegram(String content) {
        ChannelAdapter telegramAdapter = channelRegistry.get("telegram")
                .orElseThrow(() -> new IllegalStateException(
                        "Telegram adapter not found in ChannelRegistry"));

        ChannelMessage outbound = ChannelMessage.outbound(
                UUID.randomUUID().toString(),
                "telegram",
                "camel-pdf-filler",
                chatId,
                content);

        DeliveryResult result = telegramAdapter.sendMessage(outbound);
        if (result instanceof DeliveryResult.Failure failure) {
            log.error("Failed to send message to Telegram: {}", failure.message());
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
