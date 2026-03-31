package io.jaiclaw.voicecall.model;

/**
 * Lifecycle states of a voice call. Terminal states indicate the call has ended.
 */
public enum CallState {
    INITIATED,
    RINGING,
    ANSWERED,
    ACTIVE,
    SPEAKING,
    LISTENING,
    COMPLETED,
    HANGUP_USER,
    HANGUP_BOT,
    TIMEOUT,
    ERROR,
    FAILED,
    NO_ANSWER,
    BUSY,
    VOICEMAIL;

    /**
     * Returns true if this state represents a terminal (ended) call.
     */
    public boolean isTerminal() {
        return switch (this) {
            case COMPLETED, HANGUP_USER, HANGUP_BOT, TIMEOUT, ERROR, FAILED,
                 NO_ANSWER, BUSY, VOICEMAIL -> true;
            default -> false;
        };
    }
}
