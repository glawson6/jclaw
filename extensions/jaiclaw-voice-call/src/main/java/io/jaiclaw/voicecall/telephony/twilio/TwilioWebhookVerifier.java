package io.jaiclaw.voicecall.telephony.twilio;

import io.jaiclaw.voicecall.model.WebhookContext;
import io.jaiclaw.voicecall.telephony.TelephonyProvider.WebhookVerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Verifies Twilio webhook signatures using HMAC-SHA1.
 * Supports both v1 (X-Twilio-Signature) validation with reverse-proxy URL reconstruction.
 */
public class TwilioWebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(TwilioWebhookVerifier.class);

    private final String authToken;

    public TwilioWebhookVerifier(String authToken) {
        this.authToken = authToken;
    }

    /**
     * Verify a Twilio webhook request.
     */
    public WebhookVerificationResult verify(WebhookContext ctx) {
        if (authToken == null || authToken.isBlank()) {
            return WebhookVerificationResult.failure("No auth token configured");
        }

        String signature = ctx.headers().get("x-twilio-signature");
        if (signature == null) {
            signature = ctx.headers().get("X-Twilio-Signature");
        }
        if (signature == null || signature.isBlank()) {
            return WebhookVerificationResult.failure("Missing X-Twilio-Signature header");
        }

        // Reconstruct the URL + sorted POST params for HMAC computation
        String url = reconstructUrl(ctx);
        String dataToSign = url + buildSortedParams(ctx.rawBody());

        String expectedSignature = computeHmacSha1(dataToSign);
        if (expectedSignature == null) {
            return WebhookVerificationResult.failure("HMAC computation failed");
        }

        if (MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            return WebhookVerificationResult.success(signature);
        }

        log.warn("Twilio signature mismatch for URL: {}", url);
        return WebhookVerificationResult.failure("Signature mismatch");
    }

    /**
     * Reconstruct the webhook URL, accounting for reverse proxies that may change scheme/host.
     */
    private String reconstructUrl(WebhookContext ctx) {
        String url = ctx.url();

        // Check for X-Forwarded-Proto and X-Forwarded-Host headers
        String forwardedProto = ctx.headers().getOrDefault("x-forwarded-proto",
                ctx.headers().get("X-Forwarded-Proto"));
        String forwardedHost = ctx.headers().getOrDefault("x-forwarded-host",
                ctx.headers().get("X-Forwarded-Host"));

        if (forwardedProto != null && forwardedHost != null && url != null) {
            try {
                java.net.URI uri = java.net.URI.create(url);
                url = forwardedProto + "://" + forwardedHost + uri.getPath();
                if (uri.getQuery() != null) {
                    url += "?" + uri.getQuery();
                }
            } catch (Exception e) {
                log.debug("Failed to reconstruct URL with forwarded headers: {}", e.getMessage());
            }
        }

        return url != null ? url : "";
    }

    /**
     * Build sorted POST parameter string for signature validation.
     * Twilio signs by appending sorted key=value pairs to the URL.
     */
    private String buildSortedParams(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "";
        }

        TreeMap<String, String> sorted = new TreeMap<>();
        for (String pair : rawBody.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            sorted.put(key, value);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            sb.append(entry.getKey()).append(entry.getValue());
        }
        return sb.toString();
    }

    private String computeHmacSha1(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            log.error("HMAC-SHA1 computation failed: {}", e.getMessage());
            return null;
        }
    }
}
