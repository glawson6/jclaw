package io.jaiclaw.voicecall.model;

import java.time.Instant;

/**
 * A single entry in a call's transcript.
 *
 * @param timestamp when this utterance occurred
 * @param speaker   who spoke (BOT or USER)
 * @param text      the spoken or transcribed text
 * @param isFinal   whether the transcription is final (false for interim results)
 */
public record TranscriptEntry(
        Instant timestamp,
        Speaker speaker,
        String text,
        boolean isFinal
) {
    public enum Speaker {
        BOT,
        USER
    }
}
