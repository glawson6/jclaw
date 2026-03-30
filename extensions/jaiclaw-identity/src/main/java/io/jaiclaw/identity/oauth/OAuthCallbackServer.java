package io.jaiclaw.identity.oauth;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Lightweight loopback HTTP server that receives an OAuth authorization code callback.
 * Binds to {@code 127.0.0.1:{port}}, handles a single GET, then shuts down.
 */
public class OAuthCallbackServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OAuthCallbackServer.class);

    public record OAuthCallbackResult(String code, String state) {}

    private final HttpServer server;
    private final CompletableFuture<OAuthCallbackResult> resultFuture = new CompletableFuture<>();
    private final Duration timeout;

    /**
     * Start the callback server.
     *
     * @param port          port to bind on 127.0.0.1
     * @param path          path to listen on (e.g. "/oauth-callback")
     * @param expectedState the expected state parameter for CSRF validation
     * @param timeout       maximum time to wait for the callback
     */
    public OAuthCallbackServer(int port, String path, String expectedState, Duration timeout) throws IOException {
        this.timeout = timeout;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        server.createContext(path, exchange -> {
            try {
                URI requestUri = exchange.getRequestURI();
                String query = requestUri.getQuery();
                String code = extractParam(query, "code");
                String state = extractParam(query, "state");
                String error = extractParam(query, "error");

                if (error != null) {
                    sendResponse(exchange, 400, errorHtml(error));
                    resultFuture.completeExceptionally(
                            new OAuthFlowException("OAuth error: " + error));
                    return;
                }

                if (state == null || !state.equals(expectedState)) {
                    sendResponse(exchange, 400, errorHtml("Invalid state parameter (CSRF check failed)"));
                    resultFuture.completeExceptionally(
                            new OAuthFlowException("CSRF state mismatch"));
                    return;
                }

                if (code == null || code.isBlank()) {
                    sendResponse(exchange, 400, errorHtml("Missing authorization code"));
                    resultFuture.completeExceptionally(
                            new OAuthFlowException("Missing authorization code"));
                    return;
                }

                sendResponse(exchange, 200, successHtml());
                resultFuture.complete(new OAuthCallbackResult(code, state));
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        });

        server.start();
        log.debug("OAuth callback server started on 127.0.0.1:{}{}", port, path);
    }

    /**
     * Block until the callback is received or timeout.
     *
     * @return the callback result with authorization code and state
     * @throws TimeoutException     if no callback received within timeout
     * @throws OAuthFlowException   if the callback contained an error
     */
    public OAuthCallbackResult awaitCallback() throws TimeoutException, OAuthFlowException {
        try {
            return resultFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof OAuthFlowException ofe) throw ofe;
            throw new OAuthFlowException("Callback failed: " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuthFlowException("Interrupted while waiting for OAuth callback", e);
        }
    }

    @Override
    public void close() {
        server.stop(1);
        log.debug("OAuth callback server stopped");
    }

    private void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int status, String html)
            throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String extractParam(String query, String name) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String successHtml() {
        return """
                <!DOCTYPE html>
                <html><head><title>Login Successful</title></head>
                <body style="font-family:sans-serif;text-align:center;padding:3em">
                <h1>Login Successful</h1>
                <p>You can close this window and return to JaiClaw.</p>
                </body></html>
                """;
    }

    private String errorHtml(String error) {
        return """
                <!DOCTYPE html>
                <html><head><title>Login Failed</title></head>
                <body style="font-family:sans-serif;text-align:center;padding:3em">
                <h1>Login Failed</h1>
                <p>%s</p>
                <p>Please try again.</p>
                </body></html>
                """.formatted(error);
    }
}
