package io.jaiclaw.voicecall.manager;

import io.jaiclaw.voicecall.model.CallRecord;
import io.jaiclaw.voicecall.model.CallState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State machine for call lifecycle transitions.
 * Enforces valid state progressions and handles conversation mode cycling.
 */
public class CallLifecycle {

    private static final Logger log = LoggerFactory.getLogger(CallLifecycle.class);

    private CallLifecycle() {}

    /**
     * Attempt a state transition. Returns true if the transition was applied.
     */
    public static boolean transitionState(CallRecord call, CallState newState) {
        CallState current = call.getState();

        // No-op for same state
        if (current == newState) {
            return false;
        }

        // Already terminal — no transitions allowed
        if (current.isTerminal()) {
            log.debug("Ignoring transition {} -> {} for terminated call {}", current, newState, call.getCallId());
            return false;
        }

        // Terminal states are always reachable from non-terminal
        if (newState.isTerminal()) {
            call.setState(newState);
            return true;
        }

        // Validate forward progression
        if (isValidTransition(current, newState)) {
            call.setState(newState);
            return true;
        }

        log.warn("Invalid state transition {} -> {} for call {}", current, newState, call.getCallId());
        return false;
    }

    /**
     * Check if transitioning from current to next is valid.
     */
    static boolean isValidTransition(CallState current, CallState next) {
        return switch (current) {
            case INITIATED -> next == CallState.RINGING || next == CallState.ANSWERED
                    || next == CallState.ACTIVE;
            case RINGING -> next == CallState.ANSWERED || next == CallState.ACTIVE;
            case ANSWERED -> next == CallState.ACTIVE || next == CallState.SPEAKING
                    || next == CallState.LISTENING;
            case ACTIVE -> next == CallState.SPEAKING || next == CallState.LISTENING;
            // Conversation cycling: speaking <-> listening
            case SPEAKING -> next == CallState.LISTENING || next == CallState.ACTIVE;
            case LISTENING -> next == CallState.SPEAKING || next == CallState.ACTIVE;
            default -> false;
        };
    }
}
