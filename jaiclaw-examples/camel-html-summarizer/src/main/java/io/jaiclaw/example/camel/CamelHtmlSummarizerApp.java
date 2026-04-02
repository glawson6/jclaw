package io.jaiclaw.example.camel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Demonstrates JaiClaw's Camel SEDA routing with filesystem-based HTML summarization.
 *
 * <p>Drop an HTML file into {@code data/inbox/} — Camel picks it up, the agent
 * summarizes it, and the result is logged to the console.
 */
@SpringBootApplication
public class CamelHtmlSummarizerApp {

    private static final Logger log = LoggerFactory.getLogger(CamelHtmlSummarizerApp.class);

    public static void main(String[] args) {
        SpringApplication.run(CamelHtmlSummarizerApp.class, args);
    }

    @Bean
    ApplicationRunner aiProviderStartupLogger(Environment env, ChatModel chatModel) {
        return args -> {
            String provider = env.getProperty("spring.ai.model.chat", "anthropic");
            String model = switch (provider) {
                case "anthropic" -> env.getProperty("spring.ai.anthropic.chat.options.model", "claude-haiku-4-5-20251001");
                case "openai" -> env.getProperty("spring.ai.openai.chat.options.model", "gpt-4o");
                case "minimax" -> env.getProperty("spring.ai.minimax.chat.options.model", "M2-her");
                case "ollama" -> env.getProperty("spring.ai.ollama.chat.options.model", "llama3");
                default -> "unknown";
            };
            log.info("AI Provider: {} | Model: {} | ChatModel: {}", provider, model, chatModel.getClass().getSimpleName());
        };
    }
}
