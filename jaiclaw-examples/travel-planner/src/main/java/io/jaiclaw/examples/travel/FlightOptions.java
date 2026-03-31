package io.jaiclaw.examples.travel;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Flight search results placed on the blackboard after flight research.
 */
@JsonClassDescription("Available flight options for the trip")
public record FlightOptions(
        @JsonProperty("offers")
        @JsonPropertyDescription("List of flight offers with details")
        List<FlightOffer> offers,

        @JsonProperty("bestOption")
        @JsonPropertyDescription("Recommended flight option")
        String bestOption,

        @JsonProperty("estimatedCost")
        @JsonPropertyDescription("Estimated flight cost per person in USD")
        double estimatedCost
) {

    @JsonClassDescription("A single flight offer")
    public record FlightOffer(
            @JsonProperty("airline")
            @JsonPropertyDescription("Airline name")
            String airline,

            @JsonProperty("flightNumber")
            @JsonPropertyDescription("Flight number")
            String flightNumber,

            @JsonProperty("departureAirport")
            @JsonPropertyDescription("Departure airport code")
            String departureAirport,

            @JsonProperty("arrivalAirport")
            @JsonPropertyDescription("Arrival airport code")
            String arrivalAirport,

            @JsonProperty("departureTime")
            @JsonPropertyDescription("Departure date and time")
            String departureTime,

            @JsonProperty("arrivalTime")
            @JsonPropertyDescription("Arrival date and time")
            String arrivalTime,

            @JsonProperty("pricePerPerson")
            @JsonPropertyDescription("Price per person in USD")
            double pricePerPerson,

            @JsonProperty("stops")
            @JsonPropertyDescription("Number of stops (0 = direct)")
            int stops
    ) {}
}
