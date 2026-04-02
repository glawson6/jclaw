package io.jaiclaw.camel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Camel {@link org.apache.camel.builder.RouteBuilder} as a JaiClaw channel.
 *
 * <p>The annotated route builder should define its inbound route targeting
 * {@code seda:jaiclaw-{channelId}-in} in its {@code configure()} method.
 * The auto-configuration will create the corresponding SEDA consumer and
 * optional outbound bridge routes.
 *
 * <p>Example:
 * <pre>
 * {@literal @}JaiClawChannel(channelId = "s3-ingest", outboundUri = "kafka:results")
 * public class S3IngestRoute extends RouteBuilder {
 *     {@literal @}Override
 *     public void configure() {
 *         from("aws2-s3://docs-inbox")
 *             .to("seda:jaiclaw-s3-ingest-in");
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JaiClawChannel {

    /**
     * Unique channel identifier (required).
     */
    String channelId();

    /**
     * Human-readable display name. Defaults to "Camel: {channelId}".
     */
    String displayName() default "";

    /**
     * Account identifier for session keys. Defaults to "default".
     */
    String accountId() default "";

    /**
     * Camel endpoint URI for outbound messages (e.g. "kafka:results").
     * Mutually exclusive with {@link #outbound()}.
     */
    String outboundUri() default "";

    /**
     * JaiClaw channel name for cross-channel outbound routing (e.g. "telegram").
     * Mutually exclusive with {@link #outboundUri()}.
     */
    String outbound() default "";

    /**
     * When true, each inbound message gets a fresh ephemeral session with no
     * history persistence. Useful for batch processing channels.
     */
    boolean stateless() default false;
}
