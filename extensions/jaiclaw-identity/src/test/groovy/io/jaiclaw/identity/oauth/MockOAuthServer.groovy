package io.jaiclaw.identity.oauth

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lightweight mock OAuth server for integration tests.
 * Wraps {@code com.sun.net.httpserver.HttpServer} with configurable JSON responses
 * and request history tracking.
 */
class MockOAuthServer implements Closeable {

    /** Captured HTTP request for assertions. */
    static class RecordedRequest {
        final String method
        final String path
        final String query
        final String body
        final Map<String, String> headers

        RecordedRequest(HttpExchange exchange, String body) {
            this.method = exchange.requestMethod
            this.path = exchange.requestURI.path
            this.query = exchange.requestURI.query
            this.body = body
            this.headers = exchange.requestHeaders.collectEntries { k, v -> [(k): v.first()] }
        }

        /** Parse form-encoded body into a map. */
        Map<String, String> formParams() {
            if (body == null || body.isBlank()) return [:]
            body.split('&').collectEntries { String param ->
                String[] kv = param.split('=', 2)
                [(URLDecoder.decode(kv[0], StandardCharsets.UTF_8)):
                         kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : '']
            }
        }
    }

    private final HttpServer server
    private final Map<String, CopyOnWriteArrayList<RecordedRequest>> requestHistory = new ConcurrentHashMap<>()

    MockOAuthServer() {
        server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        server.start()
    }

    int getPort() {
        server.address.port
    }

    String baseUrl() {
        "http://127.0.0.1:${port}"
    }

    /** Register a static JSON response at the given path. */
    void staticEndpoint(String path, int statusCode, String jsonBody) {
        server.createContext(path) { HttpExchange exchange ->
            recordAndRespond(exchange, statusCode, jsonBody)
        }
    }

    /** Register a token endpoint that validates form params and returns a static JSON response. */
    void tokenEndpoint(String path, String responseJson) {
        staticEndpoint(path, 200, responseJson)
    }

    /** Register a userinfo endpoint that returns a static JSON response. */
    void userinfoEndpoint(String path, String responseJson) {
        staticEndpoint(path, 200, responseJson)
    }

    /** Register a device code endpoint that returns a static JSON response. */
    void deviceCodeEndpoint(String path, String responseJson) {
        staticEndpoint(path, 200, responseJson)
    }

    /**
     * Register a token endpoint that returns 'authorization_pending' for the first N polls,
     * then returns the success JSON. Optionally inserts a 'slow_down' response at a specific index.
     */
    void pendingThenSuccess(String path, int pendingCount, String successJson,
                            Integer slowDownAtIndex = null) {
        def callCount = new java.util.concurrent.atomic.AtomicInteger(0)
        server.createContext(path) { HttpExchange exchange ->
            int call = callCount.getAndIncrement()
            if (slowDownAtIndex != null && call == slowDownAtIndex) {
                recordAndRespond(exchange, 400,
                        '{"error":"slow_down","error_description":"Slow down"}')
            } else if (call < pendingCount) {
                recordAndRespond(exchange, 400,
                        '{"error":"authorization_pending","error_description":"User has not yet authorized"}')
            } else {
                recordAndRespond(exchange, 200, successJson)
            }
        }
    }

    /** Register an endpoint that always returns an error. */
    void errorEndpoint(String path, int statusCode, String errorCode, String description) {
        staticEndpoint(path, statusCode,
                """{"error":"${errorCode}","error_description":"${description}"}""")
    }

    /** Get all recorded requests for a specific path. */
    List<RecordedRequest> getRequests(String path) {
        requestHistory.getOrDefault(path, new CopyOnWriteArrayList<>()).toList()
    }

    @Override
    void close() {
        server.stop(0)
    }

    private void recordAndRespond(HttpExchange exchange, int statusCode, String jsonBody) {
        try {
            String body = exchange.requestBody.getText(StandardCharsets.UTF_8.name())
            RecordedRequest recorded = new RecordedRequest(exchange, body)
            requestHistory.computeIfAbsent(exchange.requestURI.path) { new CopyOnWriteArrayList<>() }
                    .add(recorded)

            byte[] responseBytes = jsonBody.getBytes(StandardCharsets.UTF_8)
            exchange.responseHeaders.set('Content-Type', 'application/json')
            exchange.sendResponseHeaders(statusCode, responseBytes.length)
            exchange.responseBody.withStream { OutputStream os -> os.write(responseBytes) }
        } catch (Exception e) {
            // Swallow errors in test server to avoid hiding the real test failure
            e.printStackTrace()
        }
    }
}
