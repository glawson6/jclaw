package io.jaiclaw.examples.travel;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Activity search results placed on the blackboard after activity research.
 */
@JsonClassDescription("Available activities and experiences at the destination")
public record ActivityOptions(
        @JsonProperty("activities")
        @JsonPropertyDescription("List of available activities and experiences")
        List<Activity> activities,

        @JsonProperty("totalEstimatedCost")
        @JsonPropertyDescription("Total estimated cost for all recommended activities in USD")
        double totalEstimatedCost
) {

    @JsonClassDescription("A single activity or experience at the destination")
    public record Activity(
            @JsonProperty("name")
            @JsonPropertyDescription("Activity name")
            String name,

            @JsonProperty("category")
            @JsonPropertyDescription("Category such as cultural, food, outdoor, nightlife")
            String category,

            @JsonProperty("description")
            @JsonPropertyDescription("Brief description of the activity")
            String description,

            @JsonProperty("price")
            @JsonPropertyDescription("Price per person in USD")
            double price,

            @JsonProperty("duration")
            @JsonPropertyDescription("Estimated duration (e.g. '2 hours', 'half day')")
            String duration
    ) {}
}
