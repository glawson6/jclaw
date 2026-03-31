package io.jaiclaw.voicecall.telephony.twilio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * REST client for the Twilio API. Uses Basic Auth (accountSid:authToken).
 */
public class TwilioApiClient {

    private static final Logger log = LoggerFactory.getLogger(TwilioApiClient.class);
    private static final String TWILIO_API_BASE = "https://api.twilio.com/2010-04-01/Accounts/";

    private final String accountSid;
    private final String authToken;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TwilioApiClient(String accountSid, String authToken) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create an outbound call.
     *
     * @param from       caller ID (E.164)
     * @param to         recipient (E.164)
     * @param webhookUrl URL Twilio should call when the call connects
     * @return JSON response from Twilio
     */
    public JsonNode createCall(String from, String to, String webhookUrl) {
        String url = baseUrl() + "/Calls.json";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("From", from);
        body.add("To", to);
        body.add("Url", webhookUrl);
        body.add("StatusCallback", webhookUrl);
        body.add("StatusCallbackEvent", "initiated ringing answered completed");

        return postForm(url, body);
    }

    /**
     * Hang up (update) an active call.
     */
    public void hangupCall(String callSid) {
        String url = baseUrl() + "/Calls/" + callSid + ".json";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("Status", "completed");

        postForm(url, body);
    }

    /**
     * Get the current status of a call.
     */
    public JsonNode getCallStatus(String callSid) {
        String url = baseUrl() + "/Calls/" + callSid + ".json";

        HttpHeaders headers = authHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, JsonNode.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Twilio API error fetching call {}: {} {}", callSid,
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Twilio API error: " + e.getStatusCode());
        }
    }

    private JsonNode postForm(String url, MultiValueMap<String, String> body) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Twilio API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Twilio API error: " + e.getStatusCode());
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = accountSid + ":" + authToken;
        String encoded = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }

    private String baseUrl() {
        return TWILIO_API_BASE + accountSid;
    }
}
