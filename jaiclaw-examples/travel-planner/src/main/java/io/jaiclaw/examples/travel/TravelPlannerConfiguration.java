package io.jaiclaw.examples.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jaiclaw.examples.travel.api.AmadeusApiTravelDataProvider;
import io.jaiclaw.examples.travel.api.StubTravelDataProvider;
import io.jaiclaw.examples.travel.api.TravelDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Composes all travel planner beans via explicit {@code @Bean} factory methods.
 *
 * <ul>
 *   <li>{@link TravelDataProvider} — stub (default) or Amadeus ({@code live-api} profile)</li>
 *   <li>{@link TravelPlannerPlugin} — registers 4 tools for the JaiClaw tool loop</li>
 * </ul>
 */
@Configuration
public class TravelPlannerConfiguration {

    @Bean
    @Profile("!live-api")
    TravelDataProvider stubTravelDataProvider() {
        return new StubTravelDataProvider();
    }

    @Bean
    @Profile("live-api")
    TravelDataProvider amadeusApiTravelDataProvider(
            ObjectMapper objectMapper,
            @Value("${travel.amadeus.api-key}") String apiKey,
            @Value("${travel.amadeus.api-secret}") String apiSecret) {
        return new AmadeusApiTravelDataProvider(objectMapper, apiKey, apiSecret);
    }

    @Bean
    TravelPlannerPlugin travelPlannerPlugin(TravelDataProvider provider, ObjectMapper objectMapper) {
        return new TravelPlannerPlugin(provider, objectMapper);
    }
}
