package io.jaiclaw.examples.voicecall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Telephony demo — outbound appointment reminder calls and inbound
 * customer service via Twilio.
 *
 * <p>Demonstrates the voice-call extension:
 * <ul>
 *   <li>{@code jaiclaw-voice-call} — Twilio telephony, webhooks, media streaming, MCP tools</li>
 *   <li>{@code jaiclaw-voice} — TTS/STT provider layer</li>
 *   <li>Custom appointment tools the agent uses during calls</li>
 *   <li>A skill that teaches the agent how to handle phone conversations</li>
 * </ul>
 *
 * <h3>Configuration (environment variables)</h3>
 * <pre>
 * TWILIO_ACCOUNT_SID   — Twilio Account SID
 * TWILIO_AUTH_TOKEN     — Twilio Auth Token
 * TWILIO_PHONE_NUMBER   — Your Twilio phone number (E.164)
 * PUBLIC_URL            — Publicly reachable URL for webhooks (e.g. https://abc.ngrok.io)
 * ANTHROPIC_API_KEY     — Anthropic API key
 * OPENAI_API_KEY        — OpenAI API key (optional, for real-time STT)
 * </pre>
 *
 * <h3>Twilio Setup</h3>
 * <ol>
 *   <li>Set your Twilio phone number's Voice webhook to {@code $PUBLIC_URL/voice/webhook} (POST)</li>
 *   <li>For local development, use ngrok: {@code ngrok http 8080}</li>
 * </ol>
 */
@SpringBootApplication
public class VoiceCallDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceCallDemoApplication.class, args);
    }
}
