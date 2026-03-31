package io.jaiclaw.core.model;

public record TokenUsage(
        int inputTokens,
        int outputTokens,
        int cacheReadTokens,
        int cacheWriteTokens
) {

    public static final TokenUsage ZERO = new TokenUsage(0, 0, 0, 0);

    public int totalTokens() {
        return inputTokens + outputTokens;
    }

    public TokenUsage add(TokenUsage other) {
        return new TokenUsage(
                inputTokens + other.inputTokens,
                outputTokens + other.outputTokens,
                cacheReadTokens + other.cacheReadTokens,
                cacheWriteTokens + other.cacheWriteTokens
        );
    }
}
