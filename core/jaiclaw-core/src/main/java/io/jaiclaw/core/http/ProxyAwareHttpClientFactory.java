package io.jaiclaw.core.http;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Factory for creating {@link HttpClient} instances with proxy support.
 * <p>
 * Supports two proxy configuration sources:
 * <ol>
 *   <li>Explicit configuration via {@link #configure(ProxyConfig)}</li>
 *   <li>Auto-detection from {@code HTTPS_PROXY} / {@code HTTP_PROXY} / {@code NO_PROXY} environment variables</li>
 * </ol>
 * Explicit configuration takes precedence over environment variables.
 * <p>
 * This is a pure-Java utility with no Spring dependency.
 */
public final class ProxyAwareHttpClientFactory {

    /** Proxy connection details. */
    public record ProxyConfig(String host, int port, String username, String password) {}

    private static volatile ProxyConfig globalConfig;

    private ProxyAwareHttpClientFactory() {}

    /** Explicitly configure proxy. Takes precedence over env vars. */
    public static void configure(ProxyConfig config) {
        globalConfig = config;
    }

    /** Clear explicit configuration. */
    public static void reset() {
        globalConfig = null;
    }

    /** Resolve proxy: explicit config &gt; HTTPS_PROXY &gt; HTTP_PROXY env var. */
    public static ProxyConfig resolveProxy() {
        ProxyConfig cfg = globalConfig;
        if (cfg != null && cfg.host() != null && !cfg.host().isBlank()) {
            return cfg;
        }
        // Auto-detect from environment variables
        String proxyUrl = System.getenv("HTTPS_PROXY");
        if (proxyUrl == null || proxyUrl.isBlank()) proxyUrl = System.getenv("https_proxy");
        if (proxyUrl == null || proxyUrl.isBlank()) proxyUrl = System.getenv("HTTP_PROXY");
        if (proxyUrl == null || proxyUrl.isBlank()) proxyUrl = System.getenv("http_proxy");
        if (proxyUrl != null && !proxyUrl.isBlank()) {
            return parseProxyUrl(proxyUrl);
        }
        return null;
    }

    /** Parse proxy URL like {@code http://user:pass@host:port}. */
    static ProxyConfig parseProxyUrl(String proxyUrl) {
        String url = proxyUrl;
        if (url.startsWith("http://")) url = url.substring(7);
        else if (url.startsWith("https://")) url = url.substring(8);
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        String username = null;
        String password = null;
        int atIdx = url.lastIndexOf('@');
        if (atIdx != -1) {
            String userInfo = url.substring(0, atIdx);
            url = url.substring(atIdx + 1);
            int colonIdx = userInfo.indexOf(':');
            if (colonIdx != -1) {
                username = userInfo.substring(0, colonIdx);
                password = userInfo.substring(colonIdx + 1);
            } else {
                username = userInfo;
            }
        }

        int colonIdx = url.lastIndexOf(':');
        String host;
        int port = 8080;
        if (colonIdx != -1) {
            host = url.substring(0, colonIdx);
            port = Integer.parseInt(url.substring(colonIdx + 1));
        } else {
            host = url;
        }
        return new ProxyConfig(host, port, username, password);
    }

    /** Create a new {@link HttpClient.Builder} with proxy configured if available. */
    public static HttpClient.Builder newBuilder() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        ProxyConfig cfg = resolveProxy();
        if (cfg != null) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(cfg.host(), cfg.port())));
            if (cfg.username() != null && !cfg.username().isBlank()) {
                builder.authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                cfg.username(),
                                cfg.password() != null ? cfg.password().toCharArray() : new char[0]);
                    }
                });
            }
        }
        return builder;
    }

    /** Create a default {@link HttpClient} with proxy if available. */
    public static HttpClient create() {
        return newBuilder().build();
    }

    /** Create an {@link HttpClient} with proxy, redirect following, and 10s connect timeout. */
    public static HttpClient createWithDefaults() {
        return newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
}
