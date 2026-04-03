package io.jaiclaw.examples.embabel.summarizer;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.common.ai.model.DefaultModelSelectionCriteria;
import com.embabel.common.ai.model.LlmOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embabel GOAP agent that summarizes HTML content in two steps.
 *
 * <p>The GOAP planner automatically chains the actions based on type flow:
 * <ol>
 *   <li>{@code extractContent}: String (raw HTML) → {@link ContentAnalysis}</li>
 *   <li>{@code summarize}: {@link ContentAnalysis} → {@link HtmlSummary} (goal)</li>
 * </ol>
 *
 * <p>Both actions use the LLM via {@link OperationContext#ai()} to produce
 * structured JSON objects, giving typed output instead of free-form text.
 */
@Agent(description = "Summarizes HTML content into a structured summary with key topics")
public class HtmlSummaryAgent {

    private static final Logger log = LoggerFactory.getLogger(HtmlSummaryAgent.class);

    /**
     * Max tokens for LLM output. Must be set explicitly because Embabel's
     * AnthropicOptionsConverter defaults to 8192 when LlmOptions.maxTokens is null,
     * ignoring the model YAML's max_tokens.
     *
     * <p>MiniMax reasoning models produce chain-of-thought in a "thinking" content
     * block that is filtered by {@link MiniMaxThinkingFilter}. This value controls
     * the non-thinking output budget.
     */
    private static final int MAX_TOKENS = 4096;

    private static LlmOptions defaultLlmOptions() {
        LlmOptions options = new LlmOptions(DefaultModelSelectionCriteria.INSTANCE);
        options.setMaxTokens(MAX_TOKENS);
        return options;
    }

    @Action(description = "Extract and analyze the main content from raw HTML")
    public ContentAnalysis extractContent(String htmlContent, OperationContext context) {
        log.debug("Entering ContentAnalysis extractContent..");
        return context.ai()
                .withLlm(defaultLlmOptions())
                .createObject(
                        """
                        Extract the main content from this HTML. Ignore navigation, ads, footers.

                        IMPORTANT: Respond with ONLY a JSON object, no other text. Example format:
                        {"title":"...","mainPoints":["point1","point2"],"wordCount":100,"contentType":"article"}

                        HTML:
                        """ + htmlContent,
                        ContentAnalysis.class);
    }

    @Action(description = "Generate a concise structured summary from the content analysis")
    @AchievesGoal(description = "HTML content has been summarized with key topics identified")
    public HtmlSummary summarize(ContentAnalysis analysis, OperationContext context) {

        log.debug("Entering HtmlSummary summarize..");
        return context.ai()
                .withLlm(defaultLlmOptions())
                .createObject(
                        """
                        Summarize this content analysis.

                        Title: %s
                        Content type: %s
                        Word count: %d
                        Main points: %s

                        IMPORTANT: Respond with ONLY a JSON object, no other text. Example format:
                        {"summary":"2-3 sentences","topics":["topic1","topic2"],"sentiment":"positive","readingTimeMinutes":1}
                        """.formatted(
                                analysis.title(),
                                analysis.contentType(),
                                analysis.wordCount(),
                                String.join("; ", analysis.mainPoints())),
                        HtmlSummary.class);
    }
}
