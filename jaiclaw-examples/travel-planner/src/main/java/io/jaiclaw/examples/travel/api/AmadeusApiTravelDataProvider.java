package io.jaiclaw.examples.travel.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jaiclaw.core.http.ProxyAwareHttpClientFactory;
import io.jaiclaw.examples.travel.ActivityOptions;
import io.jaiclaw.examples.travel.FlightOptions;
import io.jaiclaw.examples.travel.HotelOptions;
import io.jaiclaw.examples.travel.WeatherForecast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Live Amadeus Self-Service API provider (placeholder).
 * Registered as a bean by {@link TravelDataProviderConfiguration} when
 * the {@code live-api} profile is active.
 *
 * <p>Requires:
 * <ul>
 *   <li>{@code TRAVEL_AMADEUS_API_KEY} — Amadeus API key</li>
 *   <li>{@code TRAVEL_AMADEUS_API_SECRET} — Amadeus API secret</li>
 * </ul>
 *
 * <p>Free tier: 500 calls/month. Sign up at
 * <a href="https://developers.amadeus.com">developers.amadeus.com</a>.
 *
 * <p>This implementation shows the full HTTP call pattern with query param
 * construction, OAuth2 token exchange, and JSON response parsing. Method
 * bodies are structurally complete but throw {@link UnsupportedOperationException}
 * to indicate they need the actual API integration filled in.
 */
public class AmadeusApiTravelDataProvider implements TravelDataProvider {

    private static final Logger log = LoggerFactory.getLogger(AmadeusApiTravelDataProvider.class);

    private static final String BASE_URL = "https://api.amadeus.com";
    private static final String TOKEN_URL = BASE_URL + "/v1/security/oauth2/token";
    private static final String FLIGHT_OFFERS_URL = BASE_URL + "/v2/shopping/flight-offers";
    private static final String HOTEL_SEARCH_URL = BASE_URL + "/v1/reference-data/locations/hotels/by-city";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiSecret;

    private volatile String accessToken;
    private volatile long tokenExpiresAt;

    public AmadeusApiTravelDataProvider(
            ObjectMapper objectMapper,
            String apiKey,
            String apiSecret) {
        this.httpClient = ProxyAwareHttpClientFactory.createWithDefaults();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public FlightOptions searchFlights(String origin, String destination,
                                        String departureDate, String returnDate,
                                        int travelers, double budget) {
        log.info("[AMADEUS] Searching flights: {} → {} ({} to {})", origin, destination, departureDate, returnDate);

        // TODO: Implement actual Amadeus API call.
        // The code below shows the complete pattern — fill in response mapping.
        //
        // String token = getAccessToken();
        // String url = FLIGHT_OFFERS_URL
        //         + "?originLocationCode=" + encode(origin)
        //         + "&destinationLocationCode=" + encode(destination)
        //         + "&departureDate=" + encode(departureDate)
        //         + "&returnDate=" + encode(returnDate)
        //         + "&adults=" + travelers
        //         + "&max=5"
        //         + "&currencyCode=USD";
        //
        // HttpRequest request = HttpRequest.newBuilder()
        //         .uri(URI.create(url))
        //         .header("Authorization", "Bearer " + token)
        //         .header("Accept", "application/json")
        //         .GET()
        //         .build();
        //
        // try {
        //     HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        //     if (response.statusCode() != 200) {
        //         throw new RuntimeException("Amadeus API error: " + response.statusCode());
        //     }
        //     JsonNode root = objectMapper.readTree(response.body());
        //     JsonNode data = root.get("data");
        //     List<FlightOffer> offers = new ArrayList<>();
        //     for (JsonNode offerNode : data) {
        //         String airline = offerNode.at("/validatingAirlineCodes/0").asText();
        //         double price = offerNode.at("/price/total").asDouble();
        //         JsonNode segment = offerNode.at("/itineraries/0/segments/0");
        //         offers.add(new FlightOffer(
        //                 airline,
        //                 segment.at("/carrierCode").asText() + " " + segment.at("/number").asText(),
        //                 segment.at("/departure/iataCode").asText(),
        //                 segment.at("/arrival/iataCode").asText(),
        //                 segment.at("/departure/at").asText(),
        //                 segment.at("/arrival/at").asText(),
        //                 price / travelers,
        //                 offerNode.at("/itineraries/0/segments").size() - 1
        //         ));
        //     }
        //     FlightOffer best = offers.getFirst();
        //     return new FlightOptions(offers, best.airline() + " " + best.flightNumber(), best.pricePerPerson());
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to search flights via Amadeus", e);
        // }

        throw new UnsupportedOperationException(
                "Amadeus flight search not yet implemented. "
                        + "Activate the default profile (no live-api) to use stub data, "
                        + "or fill in the TODO above.");
    }

    @Override
    public HotelOptions searchHotels(String destination, String checkIn, String checkOut,
                                      int guests, double budget) {
        log.info("[AMADEUS] Searching hotels: {} ({} to {})", destination, checkIn, checkOut);

        // TODO: Implement actual Amadeus API call.
        // String token = getAccessToken();
        // String url = HOTEL_SEARCH_URL
        //         + "?cityCode=" + encode(destination)
        //         + "&radius=25&radiusUnit=KM";
        //
        // HttpRequest request = HttpRequest.newBuilder()
        //         .uri(URI.create(url))
        //         .header("Authorization", "Bearer " + token)
        //         .header("Accept", "application/json")
        //         .GET()
        //         .build();
        //
        // try {
        //     HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        //     JsonNode root = objectMapper.readTree(response.body());
        //     List<HotelOffer> offers = new ArrayList<>();
        //     for (JsonNode hotel : root.get("data")) {
        //         offers.add(new HotelOffer(
        //                 hotel.at("/name").asText(),
        //                 hotel.at("/rating").asDouble(3.0),
        //                 0.0, // price requires a separate hotel-offers call
        //                 hotel.at("/address/lines/0").asText(""),
        //                 0.0  // review score not in this endpoint
        //         ));
        //     }
        //     return new HotelOptions(offers, offers.getFirst().name(), 0.0);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to search hotels via Amadeus", e);
        // }

        throw new UnsupportedOperationException(
                "Amadeus hotel search not yet implemented. Use stub data profile.");
    }

    @Override
    public ActivityOptions searchActivities(String destination, String startDate, String endDate,
                                             int travelers, double budget) {
        // Amadeus Tours & Activities API is in beta.
        throw new UnsupportedOperationException(
                "Amadeus activities search not yet implemented. Use stub data profile.");
    }

    @Override
    public WeatherForecast getWeather(String destination, String startDate, String endDate) {
        // Amadeus does not provide weather data. Consider using OpenWeatherMap or WeatherAPI.
        throw new UnsupportedOperationException(
                "Weather forecast requires a separate API (e.g. OpenWeatherMap). Use stub data profile.");
    }

    /**
     * Obtain an OAuth2 access token via the Amadeus client_credentials flow.
     * Tokens are cached and refreshed when expired.
     */
    @SuppressWarnings("unused") // Referenced in TODO comments above
    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return accessToken;
        }

        String body = "grant_type=client_credentials"
                + "&client_id=" + encode(apiKey)
                + "&client_secret=" + encode(apiSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Amadeus token request failed: " + response.statusCode()
                        + " — " + response.body());
            }
            JsonNode json = objectMapper.readTree(response.body());
            accessToken = json.get("access_token").asText();
            int expiresIn = json.get("expires_in").asInt();
            tokenExpiresAt = System.currentTimeMillis() + (expiresIn - 30) * 1000L;
            log.info("[AMADEUS] Access token obtained, expires in {}s", expiresIn);
            return accessToken;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while getting Amadeus token", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Amadeus access token", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
