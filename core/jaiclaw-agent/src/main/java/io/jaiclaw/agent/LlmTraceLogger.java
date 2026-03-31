package io.jaiclaw.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dedicated TRACE logger for full LLM request/response content.
 * Controlled independently via {@code logging.level.io.jaiclaw.agent.LlmTraceLogger=TRACE}.
 */
public final class LlmTraceLogger {

    private static final Logger log = LoggerFactory.getLogger(LlmTraceLogger.class);

    private LlmTraceLogger() {}

    public static boolean isEnabled() {
        return log.isTraceEnabled();
    }

    public static void logRequest(String systemPrompt, List<Message> history,
                                  String userInput, Integer promptTokens) {
        if (!log.isTraceEnabled()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("[system] ").append(systemPrompt).append('\n');
        for (Message msg : history) {
            sb.append('[').append(msg.getMessageType()).append("] ").append(msg.getText()).append('\n');
        }
        sb.append("[user] ").append(userInput);

        String tokenInfo = promptTokens != null ? " (" + String.format("%,d", promptTokens) + " tokens)" : "";
        log.trace("LLM request{}:\n{}", tokenInfo, sb);
    }

    public static void logResponse(String content, Integer completionTokens) {
        if (!log.isTraceEnabled()) return;

        String tokenInfo = completionTokens != null ? " (" + String.format("%,d", completionTokens) + " tokens)" : "";
        log.trace("LLM response{}:\n{}", tokenInfo, content);
    }

    public static void logIteration(int iteration, List<Message> messages,
                                    String output, Integer promptTokens, Integer completionTokens) {
        if (!log.isTraceEnabled()) return;

        String messagesText = messages.stream()
                .map(m -> "[" + m.getMessageType() + "] " + m.getText())
                .collect(Collectors.joining("\n"));

        String requestTokenInfo = promptTokens != null ? " (" + String.format("%,d", promptTokens) + " tokens)" : "";
        String responseTokenInfo = completionTokens != null ? " (" + String.format("%,d", completionTokens) + " tokens)" : "";

        log.trace("LLM call iteration {} request{}:\n{}", iteration, requestTokenInfo, messagesText);
        log.trace("LLM call iteration {} response{}:\n{}", iteration, responseTokenInfo, output);
    }
}
