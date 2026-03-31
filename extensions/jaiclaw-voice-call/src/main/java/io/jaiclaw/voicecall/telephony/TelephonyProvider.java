package io.jaiclaw.voicecall.telephony;

import io.jaiclaw.voicecall.model.NormalizedEvent;
import io.jaiclaw.voicecall.model.WebhookContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SPI for telephony providers (Twilio, Telnyx, Plivo, etc.).
 * Implementations handle provider-specific webhook parsing, call control, and media.
 */
public interface TelephonyProvider {

    /**
     * Provider identifier (e.g. "twilio", "telnyx").
     */
    String name();

    /**
     * Verify the authenticity of an incoming webhook request.
     *
     * @return verification result with ok/reason
     */
    WebhookVerificationResult verifyWebhook(WebhookContext ctx);

    /**
     * Parse a webhook request into normalized events.
     */
    WebhookParseResult parseWebhookEvent(WebhookContext ctx);

    /**
     * Initiate an outbound call.
     */
    CompletableFuture<InitiateCallResult> initiateCall(InitiateCallInput input);

    /**
     * Hang up an active call.
     */
    CompletableFuture<Void> hangupCall(HangupCallInput input);

    /**
     * Play TTS audio on an active call.
     */
    CompletableFuture<Void> playTts(PlayTtsInput input);

    /**
     * Start listening for speech on an active call.
     */
    CompletableFuture<Void> startListening(StartListeningInput input);

    /**
     * Stop listening for speech.
     */
    CompletableFuture<Void> stopListening(StopListeningInput input);

    /**
     * Query the current status of a call from the provider.
     */
    CompletableFuture<CallStatusResult> getCallStatus(GetCallStatusInput input);

    // --- Nested input/output records ---

    record InitiateCallInput(
            String callId,
            String from,
            String to,
            String webhookUrl,
            String clientState
    ) {}

    record InitiateCallResult(
            String providerCallId,
            String status
    ) {}

    record HangupCallInput(
            String callId,
            String providerCallId,
            String reason
    ) {}

    record PlayTtsInput(
            String callId,
            String providerCallId,
            String text,
            String voice,
            String locale
    ) {}

    record StartListeningInput(
            String callId,
            String providerCallId,
            String language,
            String turnToken
    ) {}

    record StopListeningInput(
            String callId,
            String providerCallId
    ) {}

    record GetCallStatusInput(
            String providerCallId
    ) {}

    record CallStatusResult(
            String status,
            boolean isTerminal,
            boolean isUnknown
    ) {}

    record WebhookVerificationResult(
            boolean ok,
            String reason,
            boolean isReplay,
            String verifiedRequestKey
    ) {
        public static WebhookVerificationResult success() {
            return new WebhookVerificationResult(true, null, false, null);
        }

        public static WebhookVerificationResult success(String requestKey) {
            return new WebhookVerificationResult(true, null, false, requestKey);
        }

        public static WebhookVerificationResult failure(String reason) {
            return new WebhookVerificationResult(false, reason, false, null);
        }

        public static WebhookVerificationResult replay(String reason) {
            return new WebhookVerificationResult(false, reason, true, null);
        }
    }

    record WebhookParseResult(
            List<NormalizedEvent> events,
            String providerResponseBody,
            Map<String, String> providerResponseHeaders,
            int statusCode
    ) {
        public WebhookParseResult {
            if (events == null) events = List.of();
            if (providerResponseHeaders == null) providerResponseHeaders = Map.of();
            if (statusCode <= 0) statusCode = 200;
        }

        public static WebhookParseResult of(List<NormalizedEvent> events) {
            return new WebhookParseResult(events, null, Map.of(), 200);
        }

        public static WebhookParseResult withTwiml(List<NormalizedEvent> events, String twiml) {
            return new WebhookParseResult(events, twiml,
                    Map.of("Content-Type", "application/xml"), 200);
        }
    }
}
