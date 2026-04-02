package io.jaiclaw.camel;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Spring Boot configuration properties for Camel channel integration.
 *
 * <p>Example:
 * <pre>
 * jaiclaw:
 *   camel:
 *     channels:
 *       - channelId: s3-ingest
 *         displayName: "S3 Ingestion"
 *         outboundUri: "s3://my-bucket?prefix=output/"
 * </pre>
 */
@ConfigurationProperties(prefix = "jaiclaw.camel")
public record CamelChannelProperties(
        List<CamelChannelConfig> channels
) {
    public CamelChannelProperties {
        if (channels == null) {
            channels = List.of();
        }
    }
}
