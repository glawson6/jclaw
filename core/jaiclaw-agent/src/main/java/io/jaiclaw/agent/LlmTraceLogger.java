package io.jaiclaw.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dedicated TRACE logger for full LLM request/response content.
 * Controlled independently via {@code logging.level.io.jaiclaw.agent.LlmTraceLogger=TRACE}.
 *
 * <p>Breaks down the LLM request into three components: system prompt, tools, and user input
 * (including conversation history). Each component shows its estimated token count (~chars/4)
 * and the provider's actual total is shown for comparison.
 */
public final class LlmTraceLogger {

    private static final Logger log = LoggerFactory.getLogger(LlmTraceLogger.class);

    private LlmTraceLogger() {}

    public static boolean isEnabled() {
        return log.isTraceEnabled();
    }

    /**
     * Logs the full LLM request broken down into system prompt, tools, and user input.
     */
    public static void logRequest(String systemPrompt, List<Message> history,
                                  String userInput, Collection<? extends ToolCallback> tools,
                                  Integer providerTotalTokens) {
        if (!log.isTraceEnabled()) return;

        // 1. System prompt
        int systemEstimate = estimateTokens(systemPrompt);
        log.trace("LLM request — system prompt (~{} tokens):\n{}", systemEstimate, systemPrompt);

        // 2. Tools
        StringBuilder toolsSb = new StringBuilder();
        int toolsCharCount = 0;
        if (tools != null && !tools.isEmpty()) {
            for (ToolCallback tool : tools) {
                var def = tool.getToolDefinition();
                String toolEntry = def.name() + ": " + def.description() + "\n  schema: " + def.inputSchema();
                toolsSb.append(toolEntry).append('\n');
                // Provider sends name + description + full JSON schema
                toolsCharCount += def.name().length() + def.description().length() + def.inputSchema().length();
            }
        }
        int toolsEstimate = estimateTokens(toolsCharCount);
        log.trace("LLM request — tools ({} tools, ~{} tokens):\n{}",
                tools != null ? tools.size() : 0, toolsEstimate, toolsSb);

        // 3. Conversation (history + current user input)
        StringBuilder convSb = new StringBuilder();
        for (Message msg : history) {
            convSb.append('[').append(msg.getMessageType()).append("] ").append(msg.getText()).append('\n');
        }
        convSb.append("[user] ").append(userInput);
        int convEstimate = estimateTokens(convSb.length());
        log.trace("LLM request — conversation (~{} tokens):\n{}", convEstimate, convSb);

        // 4. Summary line
        int estimatedTotal = systemEstimate + toolsEstimate + convEstimate;
        String providerInfo = providerTotalTokens != null
                ? String.format(", provider total: %,d", providerTotalTokens)
                : "";
        log.trace("LLM request — token breakdown: system ~{} + tools ~{} + conversation ~{} = ~{} estimated{}",
                systemEstimate, toolsEstimate, convEstimate, estimatedTotal, providerInfo);
    }

    public static void logResponse(String content, Integer completionTokens) {
        if (!log.isTraceEnabled()) return;

        String tokenInfo = completionTokens != null ? " (" + String.format("%,d", completionTokens) + " tokens)" : "";
        log.trace("LLM response{}:\n{}", tokenInfo, content);
    }

    /**
     * Logs a single iteration of the explicit tool loop with component breakdown.
     */
    public static void logIteration(int iteration, List<Message> messages,
                                    String output, Collection<? extends ToolCallback> tools,
                                    Integer providerInputTokens, Integer providerOutputTokens) {
        if (!log.isTraceEnabled()) return;

        // Separate system messages from conversation
        String systemContent = messages.stream()
                .filter(m -> "SYSTEM".equals(m.getMessageType().name()))
                .map(Message::getText)
                .collect(Collectors.joining("\n"));
        List<Message> nonSystem = messages.stream()
                .filter(m -> !"SYSTEM".equals(m.getMessageType().name()))
                .toList();

        int systemEstimate = estimateTokens(systemContent);
        if (!systemContent.isEmpty()) {
            log.trace("LLM iteration {} — system prompt (~{} tokens):\n{}", iteration, systemEstimate, systemContent);
        }

        // Tools
        int toolsCharCount = 0;
        StringBuilder toolsSb = new StringBuilder();
        if (tools != null && !tools.isEmpty()) {
            for (ToolCallback tool : tools) {
                var def = tool.getToolDefinition();
                String toolEntry = def.name() + ": " + def.description() + "\n  schema: " + def.inputSchema();
                toolsSb.append(toolEntry).append('\n');
                toolsCharCount += def.name().length() + def.description().length() + def.inputSchema().length();
            }
        }
        int toolsEstimate = estimateTokens(toolsCharCount);
        log.trace("LLM iteration {} — tools ({} tools, ~{} tokens):\n{}",
                iteration, tools != null ? tools.size() : 0, toolsEstimate, toolsSb);

        // Conversation
        String convText = nonSystem.stream()
                .map(m -> "[" + m.getMessageType() + "] " + m.getText())
                .collect(Collectors.joining("\n"));
        int convEstimate = estimateTokens(convText);
        log.trace("LLM iteration {} — conversation (~{} tokens):\n{}", iteration, convEstimate, convText);

        // Summary
        int estimatedTotal = systemEstimate + toolsEstimate + convEstimate;
        String providerInfo = providerInputTokens != null
                ? String.format(", provider total: %,d", providerInputTokens)
                : "";
        log.trace("LLM iteration {} — token breakdown: system ~{} + tools ~{} + conversation ~{} = ~{} estimated{}",
                iteration, systemEstimate, toolsEstimate, convEstimate, estimatedTotal, providerInfo);

        // Response
        String responseTokenInfo = providerOutputTokens != null
                ? " (" + String.format("%,d", providerOutputTokens) + " tokens)"
                : "";
        log.trace("LLM iteration {} response{}:\n{}", iteration, responseTokenInfo, output);
    }

    /**
     * Estimates token count from text length. Uses chars/4 as a rough approximation
     * that works reasonably well across English text and JSON schema content.
     */
    static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return estimateTokens(text.length());
    }

    static int estimateTokens(int charCount) {
        return Math.max(1, (charCount + 2) / 4);  // round to nearest, minimum 1
    }
}
