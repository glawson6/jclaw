package io.jaiclaw.voicecall.manager;

import io.jaiclaw.voicecall.model.*;
import io.jaiclaw.voicecall.store.CallStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Processes normalized call events: deduplication, state transitions,
 * transcript management, and call auto-registration.
 */
public class CallEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(CallEventProcessor.class);

    private final Map<String, CallRecord> activeCalls;
    private final Map<String, String> providerCallIdMap;
    private final Set<String> processedDedupeKeys;
    private final Set<String> rejectedProviderCallIds;
    private final CallStore callStore;
    private final InboundPolicy inboundPolicy;
    private final Consumer<CallRecord> onCallAnswered;

    public CallEventProcessor(Map<String, CallRecord> activeCalls,
                              Map<String, String> providerCallIdMap,
                              CallStore callStore,
                              InboundPolicy inboundPolicy,
                              Consumer<CallRecord> onCallAnswered) {
        this.activeCalls = activeCalls;
        this.providerCallIdMap = providerCallIdMap;
        this.processedDedupeKeys = ConcurrentHashMap.newKeySet();
        this.rejectedProviderCallIds = ConcurrentHashMap.newKeySet();
        this.callStore = callStore;
        this.inboundPolicy = inboundPolicy;
        this.onCallAnswered = onCallAnswered;
    }

    /**
     * Process a single normalized event. Returns true if the event was handled.
     */
    public boolean processEvent(NormalizedEvent event) {
        // Deduplicate
        String dedupeKey = event.dedupeKey();
        if (dedupeKey != null && !processedDedupeKeys.add(dedupeKey)) {
            log.debug("Duplicate event skipped: {}", dedupeKey);
            return false;
        }

        // Reject events for previously rejected calls
        if (rejectedProviderCallIds.contains(event.providerCallId())) {
            return false;
        }

        // Resolve callId: check provider mapping first, then use event's callId
        String callId = resolveCallId(event);
        CallRecord call = activeCalls.get(callId);

        // Auto-register untracked calls (inbound or externally initiated)
        if (call == null && event instanceof NormalizedEvent.CallInitiated initiated) {
            call = autoRegisterCall(initiated);
            if (call == null) {
                return false; // Rejected by policy
            }
        }

        if (call == null) {
            log.debug("Event for unknown call {}, skipping", callId);
            return false;
        }

        // Skip already-processed events per call
        if (call.hasProcessedEvent(event.id())) {
            return false;
        }
        call.markEventProcessed(event.id());

        // Update provider call ID mapping
        if (event.providerCallId() != null && !event.providerCallId().isBlank()) {
            call.setProviderCallId(event.providerCallId());
            providerCallIdMap.put(event.providerCallId(), callId);
        }

        // Apply event-specific logic
        applyEvent(call, event);

        // Persist
        callStore.persist(call);

        return true;
    }

    private void applyEvent(CallRecord call, NormalizedEvent event) {
        switch (event) {
            case NormalizedEvent.CallInitiated e -> {
                CallLifecycle.transitionState(call, CallState.INITIATED);
            }
            case NormalizedEvent.CallRinging e -> {
                CallLifecycle.transitionState(call, CallState.RINGING);
            }
            case NormalizedEvent.CallAnswered e -> {
                CallLifecycle.transitionState(call, CallState.ANSWERED);
                call.setAnsweredAt(e.timestamp());
                if (onCallAnswered != null) {
                    onCallAnswered.accept(call);
                }
            }
            case NormalizedEvent.CallActive e -> {
                CallLifecycle.transitionState(call, CallState.ACTIVE);
            }
            case NormalizedEvent.CallSpeaking e -> {
                CallLifecycle.transitionState(call, CallState.SPEAKING);
                call.addTranscriptEntry(TranscriptEntry.Speaker.BOT, e.text());
            }
            case NormalizedEvent.CallSpeech e -> {
                if (e.isFinal()) {
                    call.addTranscriptEntry(TranscriptEntry.Speaker.USER, e.transcript());
                }
            }
            case NormalizedEvent.CallSilence e -> {
                log.debug("Silence detected on call {}: {}ms", call.getCallId(), e.durationMs());
            }
            case NormalizedEvent.CallDtmf e -> {
                log.debug("DTMF on call {}: {}", call.getCallId(), e.digits());
            }
            case NormalizedEvent.CallEnded e -> {
                finalizeCall(call, e.reason(), e.timestamp());
            }
            case NormalizedEvent.CallError e -> {
                log.error("Call error on {}: {} (retryable={})", call.getCallId(), e.error(), e.retryable());
                if (!e.retryable()) {
                    finalizeCall(call, EndReason.ERROR, e.timestamp());
                }
            }
        }
    }

    private CallRecord autoRegisterCall(NormalizedEvent.CallInitiated event) {
        // Check inbound policy
        if (event.direction() == CallDirection.INBOUND) {
            if (!inboundPolicy.shouldAcceptInbound(event.from())) {
                rejectedProviderCallIds.add(event.providerCallId());
                log.info("Rejected inbound call from {}", event.from());
                return null;
            }
        }

        CallRecord call = new CallRecord(
                event.callId(), "twilio", event.direction(),
                event.from(), event.to(), CallMode.CONVERSATION);
        call.setProviderCallId(event.providerCallId());

        activeCalls.put(call.getCallId(), call);
        if (event.providerCallId() != null) {
            providerCallIdMap.put(event.providerCallId(), call.getCallId());
        }

        log.info("Auto-registered {} call: {}", event.direction(), call.getCallId());
        return call;
    }

    private void finalizeCall(CallRecord call, EndReason reason, Instant endedAt) {
        call.setEndedAt(endedAt);
        call.setEndReason(reason);
        CallState terminalState = mapEndReasonToState(reason);
        CallLifecycle.transitionState(call, terminalState);

        activeCalls.remove(call.getCallId());
        if (call.getProviderCallId() != null) {
            providerCallIdMap.remove(call.getProviderCallId());
        }

        log.info("Call {} finalized: state={}, reason={}", call.getCallId(), call.getState(), reason);
    }

    private String resolveCallId(NormalizedEvent event) {
        // Check provider mapping first
        if (event.providerCallId() != null) {
            String mapped = providerCallIdMap.get(event.providerCallId());
            if (mapped != null) return mapped;
        }
        return event.callId();
    }

    private CallState mapEndReasonToState(EndReason reason) {
        return switch (reason) {
            case USER_HANGUP -> CallState.HANGUP_USER;
            case BOT_HANGUP -> CallState.HANGUP_BOT;
            case COMPLETED -> CallState.COMPLETED;
            case TIMEOUT, MAX_DURATION, SILENCE_TIMEOUT -> CallState.TIMEOUT;
            case NO_ANSWER -> CallState.NO_ANSWER;
            case BUSY -> CallState.BUSY;
            case VOICEMAIL -> CallState.VOICEMAIL;
            case ERROR, NETWORK_ERROR -> CallState.ERROR;
            case REJECTED -> CallState.FAILED;
            case UNKNOWN -> CallState.COMPLETED;
        };
    }
}
