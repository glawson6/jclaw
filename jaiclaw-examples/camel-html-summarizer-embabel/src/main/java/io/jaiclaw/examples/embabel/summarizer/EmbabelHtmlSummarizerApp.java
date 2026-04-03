package io.jaiclaw.examples.embabel.summarizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Demonstrates JaiClaw + Embabel GOAP integration via Camel SEDA routing.
 *
 * <p>Drop an HTML file into {@code target/data/inbox/} — Camel picks it up,
 * the Embabel GOAP planner chains {@code extractContent → summarize}, and the
 * structured {@link HtmlSummary} JSON is logged to the console.
 */
@SpringBootApplication
public class EmbabelHtmlSummarizerApp {

    private static final Logger log = LoggerFactory.getLogger(EmbabelHtmlSummarizerApp.class);

    public static void main(String[] args) {
        SpringApplication.run(EmbabelHtmlSummarizerApp.class, args);
    }

    @Bean
    ApplicationRunner startupBanner(Environment env) {
        return args -> {
            String inbox = env.getProperty("app.inbox", "target/data/inbox");
            log.info("Embabel HTML Summarizer ready — drop .html files into: {}", inbox);
        };
    }
}
