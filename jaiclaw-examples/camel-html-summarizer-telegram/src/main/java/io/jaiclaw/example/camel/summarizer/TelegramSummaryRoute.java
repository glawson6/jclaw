package io.jaiclaw.example.camel.summarizer;

import io.jaiclaw.channel.ChannelAdapter;
import io.jaiclaw.channel.ChannelMessage;
import io.jaiclaw.channel.ChannelRegistry;
import io.jaiclaw.channel.DeliveryResult;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Outbound route: consumes agent summaries from the SEDA outbound queue
 * and sends them to a configured Telegram chat via the Telegram channel adapter.
 *
 * <p>Because this route consumes from {@code seda:jaiclaw-html-summarizer-telegram-out},
 * the auto-configuration detects it and skips the default logger fallback.
 */
@Configuration
public class TelegramSummaryRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(TelegramSummaryRoute.class);

    private final ChannelRegistry channelRegistry;
    private final String chatId;

    public TelegramSummaryRoute(
            ChannelRegistry channelRegistry,
            @Value("${app.telegram.chat-id}") String chatId) {
        this.channelRegistry = channelRegistry;
        this.chatId = chatId;
    }

    @Override
    public void configure() {
        from("seda:jaiclaw-html-summarizer-telegram-out")
                .routeId("telegram-summary-sender")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) {
                        String peerId = exchange.getIn().getHeader("JaiClawPeerId", String.class);
                        String body = exchange.getIn().getBody(String.class);

                        String filename = peerId != null ? peerId : "unknown";
                        String content = "**" + filename + "**\n\n" + body;

                        ChannelAdapter telegramAdapter = channelRegistry.get("telegram")
                                .orElseThrow(() -> new IllegalStateException(
                                        "Telegram adapter not found in ChannelRegistry"));

                        ChannelMessage outbound = ChannelMessage.outbound(
                                UUID.randomUUID().toString(),
                                "telegram",
                                "camel-summarizer",
                                chatId,
                                content);

                        DeliveryResult result = telegramAdapter.sendMessage(outbound);
                        if (result instanceof DeliveryResult.Success) {
                            log.info("Sent summary for [{}] to Telegram chat {}", filename, chatId);
                        } else if (result instanceof DeliveryResult.Failure failure) {
                            log.error("Failed to send summary for [{}] to Telegram: {}",
                                    filename, failure.message());
                        }
                    }
                });
    }
}
