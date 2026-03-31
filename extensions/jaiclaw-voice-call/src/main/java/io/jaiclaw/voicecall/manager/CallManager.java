package io.jaiclaw.voicecall.manager;

import io.jaiclaw.voicecall.config.VoiceCallProperties;
import io.jaiclaw.voicecall.model.*;
import io.jaiclaw.voicecall.store.CallStore;
import io.jaiclaw.voicecall.telephony.TelephonyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Thread-safe call lifecycle manager. Coordinates telephony providers, event processing,
 * and transcript collection.
 */
public class CallManager {

    private static final Logger log = LoggerFactory.getLogger(CallManager.class);

    private final TelephonyProvider telephonyProvider;
    private final CallStore callStore;
    private final VoiceCallProperties properties;
    private final CallEventProcessor eventProcessor;

    // Active call state (thread-safe)
    private final ConcurrentHashMap<String, CallRecord> activeCalls = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> providerCallIdMap = new ConcurrentHashMap<>();

    // Per-call transcript waiters for conversation mode
    private final ConcurrentHashMap<String, TranscriptWaiter> transcriptWaiters = new ConcurrentHashMap<>();

    // Max-duration timers per call
    private final ConcurrentHashMap<String, ScheduledFuture<?>> maxDurationTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    // Callback for answered calls
    private Consumer<CallRecord> onCallAnswered;

    public CallManager(TelephonyProvider telephonyProvider,
                       CallStore callStore,
                       VoiceCallProperties properties) {
        this.telephonyProvider = telephonyProvider;
        this.callStore = callStore;
        this.properties = properties;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "call-manager-scheduler");
            t.setDaemon(true);
            return t;
        });

        InboundPolicy inboundPolicy = new InboundPolicy(
                properties.inbound() != null ? properties.inbound() : null);

        this.eventProcessor = new CallEventProcessor(
                activeCalls, providerCallIdMap, callStore, inboundPolicy, this::handleCallAnswered);

        // Load active calls from store for recovery
        Map<String, CallRecord> recovered = callStore.loadActiveCalls();
        activeCalls.putAll(recovered);
        recovered.values().forEach(call -> {
            if (call.getProviderCallId() != null) {
                providerCallIdMap.put(call.getProviderCallId(), call.getCallId());
            }
        });
        if (!recovered.isEmpty()) {
            log.info("Recovered {} active calls from store", recovered.size());
        }
    }

    /**
     * Set the callback for when a call is answered.
     */
    public void setOnCallAnswered(Consumer<CallRecord> callback) {
        this.onCallAnswered = callback;
    }

    /**
     * Initiate an outbound call.
     */
    public CompletableFuture<CallRecord> initiateCall(String to, String message, CallMode mode) {
        String callId = UUID.randomUUID().toString();
        String from = properties.outbound() != null ? properties.outbound().fromNumber() : null;
        if (from == null || from.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No fromNumber configured for outbound calls"));
        }

        CallRecord call = new CallRecord(callId, telephonyProvider.name(),
                CallDirection.OUTBOUND, from, to, mode);
        activeCalls.put(callId, call);
        callStore.persist(call);

        return telephonyProvider.initiateCall(new TelephonyProvider.InitiateCallInput(
                callId, from, to, buildWebhookUrl(callId), null
        )).thenApply(result -> {
            call.setProviderCallId(result.providerCallId());
            providerCallIdMap.put(result.providerCallId(), callId);
            CallLifecycle.transitionState(call, CallState.RINGING);
            callStore.persist(call);
            startMaxDurationTimer(callId);
            log.info("Call initiated: callId={}, to={}, mode={}", callId, to, mode);
            return call;
        }).exceptionally(ex -> {
            log.error("Failed to initiate call to {}: {}", to, ex.getMessage());
            call.setState(CallState.FAILED);
            call.setEndedAt(Instant.now());
            call.setEndReason(EndReason.ERROR);
            activeCalls.remove(callId);
            callStore.persist(call);
            throw new CompletionException(ex);
        });
    }

    /**
     * Speak to the user and wait for their response (conversation mode).
     */
    public CompletableFuture<String> speak(String callId, String message) {
        CallRecord call = activeCalls.get(callId);
        if (call == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("No active call with id: " + callId));
        }

        call.addTranscriptEntry(TranscriptEntry.Speaker.BOT, message);
        CallLifecycle.transitionState(call, CallState.SPEAKING);

        return telephonyProvider.playTts(new TelephonyProvider.PlayTtsInput(
                callId, call.getProviderCallId(), message, null, null
        )).thenCompose(v -> {
            // After speaking, start listening for user response
            return waitForTranscript(callId);
        });
    }

    /**
     * Speak to the user without waiting for a response.
     */
    public CompletableFuture<Void> speakNoWait(String callId, String message) {
        CallRecord call = activeCalls.get(callId);
        if (call == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("No active call with id: " + callId));
        }

        call.addTranscriptEntry(TranscriptEntry.Speaker.BOT, message);
        CallLifecycle.transitionState(call, CallState.SPEAKING);
        callStore.persist(call);

        return telephonyProvider.playTts(new TelephonyProvider.PlayTtsInput(
                callId, call.getProviderCallId(), message, null, null));
    }

    /**
     * End an active call.
     */
    public CompletableFuture<Void> endCall(String callId) {
        CallRecord call = activeCalls.get(callId);
        if (call == null) {
            return CompletableFuture.completedFuture(null);
        }

        return telephonyProvider.hangupCall(new TelephonyProvider.HangupCallInput(
                callId, call.getProviderCallId(), "bot_hangup"
        )).thenRun(() -> {
            finalizeCall(callId, EndReason.BOT_HANGUP);
        });
    }

    /**
     * Process an incoming webhook event.
     */
    public void processEvent(NormalizedEvent event) {
        eventProcessor.processEvent(event);

        // Check if this event resolves a transcript waiter
        if (event instanceof NormalizedEvent.CallSpeech speech && speech.isFinal()) {
            String callId = resolveCallId(event);
            TranscriptWaiter waiter = transcriptWaiters.remove(callId);
            if (waiter != null) {
                waiter.resolve(speech.transcript());
            }
        }
    }

    /**
     * Get an active call by ID.
     */
    public Optional<CallRecord> getCall(String callId) {
        return Optional.ofNullable(activeCalls.get(callId));
    }

    /**
     * Get all active calls.
     */
    public Collection<CallRecord> getActiveCalls() {
        return Collections.unmodifiableCollection(activeCalls.values());
    }

    /**
     * Get call history.
     */
    public List<CallRecord> getHistory(int limit) {
        return callStore.getHistory(limit);
    }

    /**
     * Clean up stale calls that exceed max duration.
     */
    public void reapStaleCalls() {
        Instant cutoff = Instant.now().minusSeconds(
                properties.outbound() != null ? properties.outbound().maxDurationSec() * 2L : 1200);

        List<String> staleIds = activeCalls.entrySet().stream()
                .filter(e -> e.getValue().getStartedAt().isBefore(cutoff))
                .map(Map.Entry::getKey)
                .toList();

        for (String callId : staleIds) {
            log.warn("Reaping stale call: {}", callId);
            finalizeCall(callId, EndReason.TIMEOUT);
        }
    }

    /**
     * Shutdown the scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    // --- Internal ---

    private CompletableFuture<String> waitForTranscript(String callId) {
        CallRecord call = activeCalls.get(callId);
        if (call == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Call not active: " + callId));
        }

        String turnToken = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();

        // Start listening
        telephonyProvider.startListening(new TelephonyProvider.StartListeningInput(
                callId, call.getProviderCallId(), null, turnToken));
        CallLifecycle.transitionState(call, CallState.LISTENING);

        // Set up timeout
        int timeoutSec = properties.outbound() != null ? properties.outbound().silenceTimeoutSec() : 30;
        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            TranscriptWaiter waiter = transcriptWaiters.remove(callId);
            if (waiter != null) {
                waiter.timeout();
            }
        }, timeoutSec, TimeUnit.SECONDS);

        transcriptWaiters.put(callId, new TranscriptWaiter(future, timeoutFuture, turnToken));

        return future;
    }

    private void startMaxDurationTimer(String callId) {
        int maxSec = properties.outbound() != null ? properties.outbound().maxDurationSec() : 600;
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            log.warn("Call {} reached max duration ({} seconds)", callId, maxSec);
            endCall(callId);
        }, maxSec, TimeUnit.SECONDS);
        maxDurationTimers.put(callId, timer);
    }

    private void finalizeCall(String callId, EndReason reason) {
        CallRecord call = activeCalls.remove(callId);
        if (call == null) return;

        call.setEndedAt(Instant.now());
        call.setEndReason(reason);

        CallState terminalState = switch (reason) {
            case USER_HANGUP -> CallState.HANGUP_USER;
            case BOT_HANGUP -> CallState.HANGUP_BOT;
            case TIMEOUT, MAX_DURATION, SILENCE_TIMEOUT -> CallState.TIMEOUT;
            default -> CallState.COMPLETED;
        };
        call.setState(terminalState);

        if (call.getProviderCallId() != null) {
            providerCallIdMap.remove(call.getProviderCallId());
        }

        // Cancel timers
        ScheduledFuture<?> timer = maxDurationTimers.remove(callId);
        if (timer != null) timer.cancel(false);

        // Reject any pending transcript waiter
        TranscriptWaiter waiter = transcriptWaiters.remove(callId);
        if (waiter != null) waiter.cancel();

        callStore.persist(call);
        log.info("Call finalized: callId={}, state={}, reason={}", callId, terminalState, reason);
    }

    private void handleCallAnswered(CallRecord call) {
        if (onCallAnswered != null) {
            onCallAnswered.accept(call);
        }
    }

    private String resolveCallId(NormalizedEvent event) {
        if (event.providerCallId() != null) {
            String mapped = providerCallIdMap.get(event.providerCallId());
            if (mapped != null) return mapped;
        }
        return event.callId();
    }

    private String buildWebhookUrl(String callId) {
        String publicUrl = properties.serve() != null ? properties.serve().publicUrl() : "";
        String path = properties.serve() != null ? properties.serve().webhookPath() : "/voice/webhook";
        return publicUrl + path + "?callId=" + callId;
    }

    // --- Transcript waiter helper ---

    private static class TranscriptWaiter {
        private final CompletableFuture<String> future;
        private final ScheduledFuture<?> timeoutFuture;
        private final String turnToken;

        TranscriptWaiter(CompletableFuture<String> future, ScheduledFuture<?> timeoutFuture,
                         String turnToken) {
            this.future = future;
            this.timeoutFuture = timeoutFuture;
            this.turnToken = turnToken;
        }

        void resolve(String transcript) {
            timeoutFuture.cancel(false);
            future.complete(transcript);
        }

        void timeout() {
            future.complete(""); // Empty transcript on timeout
        }

        void cancel() {
            timeoutFuture.cancel(false);
            future.cancel(false);
        }
    }
}
