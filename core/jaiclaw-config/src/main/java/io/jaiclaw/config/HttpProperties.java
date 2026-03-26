package io.jaiclaw.config;

/**
 * HTTP client configuration.
 * Bound from {@code jaiclaw.http.*} in application.yml.
 */
public record HttpProperties(
        HttpProxyProperties proxy
) {
    public static final HttpProperties DEFAULT = new HttpProperties(HttpProxyProperties.DEFAULT);

    public HttpProperties {
        if (proxy == null) proxy = HttpProxyProperties.DEFAULT;
    }
}
