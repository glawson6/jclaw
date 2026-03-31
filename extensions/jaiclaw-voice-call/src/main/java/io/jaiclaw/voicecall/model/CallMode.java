package io.jaiclaw.voicecall.model;

/**
 * Operational mode for a voice call.
 * <p>
 * NOTIFY: one-way message delivery followed by auto-hangup.
 * CONVERSATION: interactive multi-turn voice exchange.
 */
public enum CallMode {
    NOTIFY,
    CONVERSATION
}
