package io.jaiclaw.examples.travel;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Goal condition — the final assembled trip plan.
 */
@JsonClassDescription("Complete trip plan with flights, hotels, activities, weather, and itinerary")
public record TripPlan(
        @JsonProperty("summary")
        @JsonPropertyDescription("Trip summary")
        String summary,

        @JsonProperty("totalCost")
        @JsonPropertyDescription("Total estimated cost in USD")
        double totalCost,

        @JsonProperty("withinBudget")
        @JsonPropertyDescription("Whether the plan is within the requested budget")
        boolean withinBudget,

        @JsonProperty("itinerary")
        @JsonPropertyDescription("Day-by-day itinerary")
        String itinerary,

        @JsonProperty("flightSummary")
        @JsonPropertyDescription("Summary of selected flight")
        String flightSummary,

        @JsonProperty("hotelSummary")
        @JsonPropertyDescription("Summary of selected hotel")
        String hotelSummary,

        @JsonProperty("packingList")
        @JsonPropertyDescription("Packing list based on weather and activities")
        String packingList
) {

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String summary;
        private double totalCost;
        private boolean withinBudget;
        private String itinerary;
        private String flightSummary;
        private String hotelSummary;
        private String packingList;

        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder totalCost(double totalCost) { this.totalCost = totalCost; return this; }
        public Builder withinBudget(boolean withinBudget) { this.withinBudget = withinBudget; return this; }
        public Builder itinerary(String itinerary) { this.itinerary = itinerary; return this; }
        public Builder flightSummary(String flightSummary) { this.flightSummary = flightSummary; return this; }
        public Builder hotelSummary(String hotelSummary) { this.hotelSummary = hotelSummary; return this; }
        public Builder packingList(String packingList) { this.packingList = packingList; return this; }

        public TripPlan build() {
            return new TripPlan(summary, totalCost, withinBudget, itinerary,
                    flightSummary, hotelSummary, packingList);
        }
    }
}
