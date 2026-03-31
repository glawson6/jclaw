package io.jaiclaw.examples.travel.api;

import io.jaiclaw.examples.travel.ActivityOptions;
import io.jaiclaw.examples.travel.FlightOptions;
import io.jaiclaw.examples.travel.HotelOptions;
import io.jaiclaw.examples.travel.WeatherForecast;

/**
 * SPI for travel data retrieval — swappable implementations
 * allow using hardcoded stubs (default) or live APIs.
 */
public interface TravelDataProvider {

    FlightOptions searchFlights(String origin, String destination,
                                String departureDate, String returnDate,
                                int travelers, double budget);

    HotelOptions searchHotels(String destination,
                              String checkIn, String checkOut,
                              int guests, double budget);

    ActivityOptions searchActivities(String destination,
                                     String startDate, String endDate,
                                     int travelers, double budget);

    WeatherForecast getWeather(String destination,
                               String startDate, String endDate);
}
