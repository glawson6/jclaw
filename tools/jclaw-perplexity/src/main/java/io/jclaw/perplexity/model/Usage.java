package io.jclaw.perplexity.model;

public record Usage(int promptTokens, int completionTokens, int totalTokens) {}
