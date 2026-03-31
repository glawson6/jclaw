package io.jaiclaw.examples.travel;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Initial input representing a travel planning request.
 */
@JsonClassDescription("A travel planning request with origin, destination, and dates")
public record TravelRequest(
        @JsonProperty("origin")
        @JsonPropertyDescription("Departure city or airport code")
        String origin,

        @JsonProperty("destination")
        @JsonPropertyDescription("Travel destination city or region")
        String destination,

        @JsonProperty("departureDate")
        @JsonPropertyDescription("Departure date (YYYY-MM-DD)")
        String departureDate,

        @JsonProperty("returnDate")
        @JsonPropertyDescription("Return date (YYYY-MM-DD)")
        String returnDate,

        @JsonProperty("budget")
        @JsonPropertyDescription("Total budget in USD")
        double budget,

        @JsonProperty("travelers")
        @JsonPropertyDescription("Number of travelers")
        int travelers
) {

    /**
     * Backward-compatible constructor — defaults origin to "JFK".
     */
    public TravelRequest(String destination, String departureDate, String returnDate,
                          double budget, int travelers) {
        this("JFK", destination, departureDate, returnDate, budget, travelers);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String origin = "JFK";
        private String destination;
        private String departureDate;
        private String returnDate;
        private double budget;
        private int travelers;

        public Builder origin(String origin) { this.origin = origin; return this; }
        public Builder destination(String destination) { this.destination = destination; return this; }
        public Builder departureDate(String departureDate) { this.departureDate = departureDate; return this; }
        public Builder returnDate(String returnDate) { this.returnDate = returnDate; return this; }
        public Builder budget(double budget) { this.budget = budget; return this; }
        public Builder travelers(int travelers) { this.travelers = travelers; return this; }

        public TravelRequest build() {
            return new TravelRequest(origin, destination, departureDate, returnDate, budget, travelers);
        }
    }
}
