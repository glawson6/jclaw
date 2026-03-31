package io.jaiclaw.voicecall.model;

import java.util.Map;

/**
 * Context from an incoming telephony webhook request.
 *
 * @param headers       HTTP headers (case-insensitive keys recommended)
 * @param rawBody       raw request body as a string
 * @param url           full request URL as received
 * @param method        HTTP method (POST, GET, etc.)
 * @param queryParams   query string parameters
 * @param remoteAddress remote IP address of the caller
 */
public record WebhookContext(
        Map<String, String> headers,
        String rawBody,
        String url,
        String method,
        Map<String, String> queryParams,
        String remoteAddress
) {
    public WebhookContext {
        if (headers == null) headers = Map.of();
        if (queryParams == null) queryParams = Map.of();
    }
}
