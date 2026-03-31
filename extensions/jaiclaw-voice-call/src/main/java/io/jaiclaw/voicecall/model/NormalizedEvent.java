package io.jaiclaw.voicecall.model;

import java.time.Instant;

/**
 * Sealed interface representing provider-agnostic call events.
 * Each event carries deduplication metadata and a call reference.
 */
public sealed interface NormalizedEvent {

    String id();
    String dedupeKey();
    String callId();
    String providerCallId();
    Instant timestamp();

    record CallInitiated(String id, String dedupeKey, String callId, String providerCallId,
                         Instant timestamp, String from, String to, CallDirection direction) implements NormalizedEvent {}

    record CallRinging(String id, String dedupeKey, String callId, String providerCallId,
                       Instant timestamp) implements NormalizedEvent {}

    record CallAnswered(String id, String dedupeKey, String callId, String providerCallId,
                        Instant timestamp) implements NormalizedEvent {}

    record CallActive(String id, String dedupeKey, String callId, String providerCallId,
                      Instant timestamp) implements NormalizedEvent {}

    record CallSpeaking(String id, String dedupeKey, String callId, String providerCallId,
                        Instant timestamp, String text) implements NormalizedEvent {}

    record CallSpeech(String id, String dedupeKey, String callId, String providerCallId,
                      Instant timestamp, String transcript, boolean isFinal,
                      double confidence) implements NormalizedEvent {}

    record CallSilence(String id, String dedupeKey, String callId, String providerCallId,
                       Instant timestamp, long durationMs) implements NormalizedEvent {}

    record CallDtmf(String id, String dedupeKey, String callId, String providerCallId,
                    Instant timestamp, String digits) implements NormalizedEvent {}

    record CallEnded(String id, String dedupeKey, String callId, String providerCallId,
                     Instant timestamp, EndReason reason) implements NormalizedEvent {}

    record CallError(String id, String dedupeKey, String callId, String providerCallId,
                     Instant timestamp, String error, boolean retryable) implements NormalizedEvent {}
}
