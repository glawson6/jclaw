package io.jaiclaw.voicecall.config;

import io.jaiclaw.voicecall.model.CallMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for the voice-call extension.
 * Bind via {@code jaiclaw.voice-call.*} in application.yml or environment.
 */
@ConfigurationProperties(prefix = "jaiclaw.voice-call")
public record VoiceCallProperties(
        boolean enabled,
        String provider,
        TwilioProperties twilio,
        ServeProperties serve,
        StreamingProperties streaming,
        OutboundProperties outbound,
        InboundProperties inbound
) {
    public VoiceCallProperties {
        if (provider == null) provider = "twilio";
        if (twilio == null) twilio = new TwilioProperties(null, null);
        if (serve == null) serve = new ServeProperties("/voice/webhook", null);
        if (streaming == null) streaming = new StreamingProperties(null, null, 800, 0.5, 30);
        if (outbound == null) outbound = new OutboundProperties(null, null, CallMode.CONVERSATION, 600, 30);
        if (inbound == null) inbound = new InboundProperties("disabled", null, null, List.of());
    }

    public VoiceCallProperties() {
        this(false, "twilio", null, null, null, null, null);
    }

    /**
     * Twilio-specific configuration.
     *
     * @param accountSid Twilio account SID
     * @param authToken  Twilio auth token
     */
    public record TwilioProperties(
            String accountSid,
            String authToken
    ) {}

    /**
     * Webhook server configuration.
     *
     * @param webhookPath path prefix for webhook endpoints
     * @param publicUrl   public URL for callback URLs (no tunnel management)
     */
    public record ServeProperties(
            String webhookPath,
            String publicUrl
    ) {
        public ServeProperties {
            if (webhookPath == null) webhookPath = "/voice/webhook";
        }
    }

    /**
     * Media streaming configuration for real-time STT.
     *
     * @param sttModel          OpenAI Realtime STT model
     * @param openaiApiKey      API key for OpenAI Realtime (falls back to main key)
     * @param vadSilenceDurationMs VAD silence duration in ms
     * @param vadThreshold      VAD threshold (0.0-1.0)
     * @param preStartTimeoutSec timeout for stream auth before disconnecting
     */
    public record StreamingProperties(
            String sttModel,
            String openaiApiKey,
            int vadSilenceDurationMs,
            double vadThreshold,
            int preStartTimeoutSec
    ) {
        public StreamingProperties {
            if (sttModel == null) sttModel = "gpt-4o-transcribe";
            if (vadSilenceDurationMs <= 0) vadSilenceDurationMs = 800;
            if (vadThreshold <= 0) vadThreshold = 0.5;
            if (preStartTimeoutSec <= 0) preStartTimeoutSec = 30;
        }
    }

    /**
     * Outbound call defaults.
     *
     * @param fromNumber       default caller ID (E.164)
     * @param toNumber         default recipient (E.164)
     * @param defaultMode      default call mode
     * @param maxDurationSec   maximum call duration in seconds
     * @param silenceTimeoutSec hang up after this many seconds of silence
     */
    public record OutboundProperties(
            String fromNumber,
            String toNumber,
            CallMode defaultMode,
            int maxDurationSec,
            int silenceTimeoutSec
    ) {
        public OutboundProperties {
            if (defaultMode == null) defaultMode = CallMode.CONVERSATION;
            if (maxDurationSec <= 0) maxDurationSec = 600;
            if (silenceTimeoutSec <= 0) silenceTimeoutSec = 30;
        }
    }

    /**
     * Inbound call configuration.
     *
     * @param policy       inbound acceptance policy: disabled, open, allowlist
     * @param greeting     greeting message for inbound calls
     * @param voice        TTS voice for greetings
     * @param allowedFrom  list of allowed caller numbers (E.164) when policy=allowlist
     */
    public record InboundProperties(
            String policy,
            String greeting,
            String voice,
            List<String> allowedFrom
    ) {
        public InboundProperties {
            if (policy == null) policy = "disabled";
            if (allowedFrom == null) allowedFrom = List.of();
        }
    }
}
