package io.jaiclaw.examples.embabel.summarizer;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Intermediate blackboard type produced by the {@code extractContent} action.
 * The GOAP planner uses this as a precondition for the {@code summarize} action.
 */
@JsonClassDescription("Extracted content and analysis from an HTML document")
public record ContentAnalysis(
        @JsonProperty("title")
        @JsonPropertyDescription("Document title or heading")
        String title,

        @JsonProperty("mainPoints")
        @JsonPropertyDescription("Key points extracted from the content")
        List<String> mainPoints,

        @JsonProperty("wordCount")
        @JsonPropertyDescription("Approximate word count of the main content")
        int wordCount,

        @JsonProperty("contentType")
        @JsonPropertyDescription("Type of content: article, documentation, blog, news, etc.")
        String contentType
) {}
