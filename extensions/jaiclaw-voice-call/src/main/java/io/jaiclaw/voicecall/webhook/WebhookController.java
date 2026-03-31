package io.jaiclaw.voicecall.webhook;

import io.jaiclaw.voicecall.manager.CallManager;
import io.jaiclaw.voicecall.model.NormalizedEvent;
import io.jaiclaw.voicecall.model.WebhookContext;
import io.jaiclaw.voicecall.telephony.TelephonyProvider;
import io.jaiclaw.voicecall.telephony.TelephonyProvider.WebhookParseResult;
import io.jaiclaw.voicecall.telephony.TelephonyProvider.WebhookVerificationResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring MVC controller that handles incoming telephony webhooks.
 * Verifies signatures, parses events, and delegates to the CallManager.
 */
@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private static final int MAX_IN_FLIGHT = 50;

    private final TelephonyProvider telephonyProvider;
    private final CallManager callManager;
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public WebhookController(TelephonyProvider telephonyProvider, CallManager callManager) {
        this.telephonyProvider = telephonyProvider;
        this.callManager = callManager;
    }

    @PostMapping(value = "${jaiclaw.voice-call.serve.webhook-path:/voice/webhook}",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) {
        // Rate limiting
        if (inFlight.incrementAndGet() > MAX_IN_FLIGHT) {
            inFlight.decrementAndGet();
            log.warn("Too many in-flight webhook requests ({}), rejecting", MAX_IN_FLIGHT);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many requests");
        }

        try {
            WebhookContext ctx = buildWebhookContext(request);

            // Verify webhook signature
            WebhookVerificationResult verification = telephonyProvider.verifyWebhook(ctx);
            if (!verification.ok()) {
                log.warn("Webhook verification failed: {}", verification.reason());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Webhook verification failed");
            }

            // Parse events
            WebhookParseResult parseResult = telephonyProvider.parseWebhookEvent(ctx);

            // Process each event
            for (NormalizedEvent event : parseResult.events()) {
                try {
                    callManager.processEvent(event);
                } catch (Exception e) {
                    log.error("Failed to process event {}: {}", event.id(), e.getMessage());
                }
            }

            // Return provider-specific response (e.g., TwiML)
            if (parseResult.providerResponseBody() != null) {
                var builder = ResponseEntity.status(parseResult.statusCode());
                parseResult.providerResponseHeaders().forEach(builder::header);
                return builder.body(parseResult.providerResponseBody());
            }

            return ResponseEntity.ok("");
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error");
        } finally {
            inFlight.decrementAndGet();
        }
    }

    private WebhookContext buildWebhookContext(HttpServletRequest request) throws IOException {
        // Collect headers
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name.toLowerCase(), request.getHeader(name));
        }

        // Read body
        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // Build URL
        String url = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            url += "?" + request.getQueryString();
        }

        // Collect query params
        Map<String, String> queryParams = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                queryParams.put(key, values[0]);
            }
        });

        return new WebhookContext(headers, rawBody, url, request.getMethod(),
                queryParams, request.getRemoteAddr());
    }
}
