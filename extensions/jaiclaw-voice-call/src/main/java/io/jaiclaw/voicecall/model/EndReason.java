package io.jaiclaw.voicecall.model;

/**
 * Reason a call ended. Mirrors OpenClaw's EndReasonSchema.
 */
public enum EndReason {
    USER_HANGUP,
    BOT_HANGUP,
    COMPLETED,
    TIMEOUT,
    ERROR,
    NO_ANSWER,
    BUSY,
    REJECTED,
    VOICEMAIL,
    NETWORK_ERROR,
    MAX_DURATION,
    SILENCE_TIMEOUT,
    UNKNOWN
}
