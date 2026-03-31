package io.jaiclaw.examples.travel;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Hotel search results placed on the blackboard after hotel research.
 */
@JsonClassDescription("Available hotel options for the trip")
public record HotelOptions(
        @JsonProperty("offers")
        @JsonPropertyDescription("List of hotel offers with details")
        List<HotelOffer> offers,

        @JsonProperty("bestOption")
        @JsonPropertyDescription("Recommended hotel option")
        String bestOption,

        @JsonProperty("estimatedCostPerNight")
        @JsonPropertyDescription("Estimated cost per night in USD")
        double estimatedCostPerNight
) {

    @JsonClassDescription("A single hotel offer")
    public record HotelOffer(
            @JsonProperty("name")
            @JsonPropertyDescription("Hotel name")
            String name,

            @JsonProperty("stars")
            @JsonPropertyDescription("Star rating (1-5)")
            double stars,

            @JsonProperty("pricePerNight")
            @JsonPropertyDescription("Price per night in USD")
            double pricePerNight,

            @JsonProperty("address")
            @JsonPropertyDescription("Hotel address or neighborhood")
            String address,

            @JsonProperty("reviewScore")
            @JsonPropertyDescription("Guest review score (0-10)")
            double reviewScore
    ) {}
}
