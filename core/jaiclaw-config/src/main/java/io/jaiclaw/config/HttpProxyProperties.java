package io.jaiclaw.config;

/**
 * Proxy configuration for outbound HTTP connections.
 * Bound from {@code jaiclaw.http.proxy.*} in application.yml.
 */
public record HttpProxyProperties(
        String host,
        int port,
        String username,
        String password,
        String nonProxyHosts
) {
    public static final HttpProxyProperties DEFAULT = new HttpProxyProperties(
            null, 0, null, null, null
    );

    public HttpProxyProperties {
        if (nonProxyHosts == null) nonProxyHosts = "";
    }

    /** True if a proxy host is explicitly configured. */
    public boolean isConfigured() {
        return host != null && !host.isBlank() && port > 0;
    }
}
