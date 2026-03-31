package io.jaiclaw.examples.travel;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Weather forecast placed on the blackboard after weather research.
 */
@JsonClassDescription("Weather forecast for the destination during travel dates")
public record WeatherForecast(
        @JsonProperty("destination")
        @JsonPropertyDescription("Destination city or region")
        String destination,

        @JsonProperty("days")
        @JsonPropertyDescription("Daily weather forecasts")
        List<DayForecast> days,

        @JsonProperty("packingAdvice")
        @JsonPropertyDescription("Packing recommendations based on the forecast")
        String packingAdvice
) {

    @JsonClassDescription("Weather forecast for a single day")
    public record DayForecast(
            @JsonProperty("date")
            @JsonPropertyDescription("Date (YYYY-MM-DD)")
            String date,

            @JsonProperty("condition")
            @JsonPropertyDescription("Weather condition (e.g. sunny, partly cloudy, rain)")
            String condition,

            @JsonProperty("highCelsius")
            @JsonPropertyDescription("High temperature in Celsius")
            double highCelsius,

            @JsonProperty("lowCelsius")
            @JsonPropertyDescription("Low temperature in Celsius")
            double lowCelsius,

            @JsonProperty("precipitationPercent")
            @JsonPropertyDescription("Chance of precipitation as percentage")
            int precipitationPercent
    ) {}
}
