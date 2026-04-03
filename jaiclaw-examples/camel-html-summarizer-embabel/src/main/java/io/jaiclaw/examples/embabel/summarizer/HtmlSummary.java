package io.jaiclaw.examples.embabel.summarizer;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Goal type for the Embabel GOAP planner. When this object appears on the
 * blackboard, the agent has achieved its goal. Serialized to JSON and returned
 * as the assistant response through the JaiClaw message pipeline.
 */
@JsonClassDescription("Structured summary of an HTML document")
public record HtmlSummary(
        @JsonProperty("summary")
        @JsonPropertyDescription("A concise 2-3 sentence summary of the document")
        String summary,

        @JsonProperty("topics")
        @JsonPropertyDescription("Key topics or themes identified in the document")
        List<String> topics,

        @JsonProperty("sentiment")
        @JsonPropertyDescription("Overall sentiment: positive, neutral, or negative")
        String sentiment,

        @JsonProperty("readingTimeMinutes")
        @JsonPropertyDescription("Estimated reading time in minutes")
        int readingTimeMinutes
) {}
