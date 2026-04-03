package io.jaiclaw.example.camel.summarizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Demonstrates JaiClaw's Camel SEDA routing with Telegram delivery.
 *
 * <p>Drop an HTML file into {@code data/inbox/} — Camel picks it up, the agent
 * summarizes it, and the result is sent to a configured Telegram chat.
 */
@SpringBootApplication
public class CamelHtmlSummarizerTelegramApp {

    private static final Logger log = LoggerFactory.getLogger(CamelHtmlSummarizerTelegramApp.class);

    public static void main(String[] args) {
        SpringApplication.run(CamelHtmlSummarizerTelegramApp.class, args);
    }

    @Bean
    ApplicationRunner startupLogger(
            @Value("${app.inbox:target/data/inbox}") String inbox,
            @Value("${app.telegram.chat-id}") String chatId) {
        return args -> log.info("HTML Summarizer started — inbox: {} | Telegram chat: {}", inbox, chatId);
    }
}
