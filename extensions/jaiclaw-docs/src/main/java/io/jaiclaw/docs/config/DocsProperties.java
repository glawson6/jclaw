package io.jaiclaw.docs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jaiclaw.docs")
public record DocsProperties(
        boolean enabled
) {
    public DocsProperties() {
        this(false);
    }
}
