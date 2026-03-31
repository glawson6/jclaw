package io.jaiclaw.examples.oauthprovider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OAuth Provider Demo — authenticates with an AI provider via OAuth
 * before the agent can operate. The LLM API key is obtained through
 * a browser-based PKCE or device code flow, not from an environment variable.
 *
 * <h3>Supported providers</h3>
 * <ul>
 *   <li><b>OpenAI Codex</b> — PKCE authorization code flow (browser)</li>
 *   <li><b>Google Gemini</b> — PKCE authorization code flow (browser)</li>
 *   <li><b>Chutes</b> — PKCE authorization code flow (browser)</li>
 *   <li><b>Qwen</b> — RFC 8628 device code flow (headless)</li>
 *   <li><b>MiniMax</b> — RFC 8628 device code flow (headless)</li>
 * </ul>
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>App starts — no LLM credentials available</li>
 *   <li>User calls {@code POST /api/oauth/login/{providerId}} to trigger OAuth</li>
 *   <li>Browser opens (or device code is shown); user authorizes</li>
 *   <li>Token is stored in {@code ~/.jaiclaw/agents/default/agent/auth-profiles.json}</li>
 *   <li>A new {@code ChatModel} is created with the OAuth access token as the API key</li>
 *   <li>Now {@code POST /api/chat} works — the agent uses the OAuth-acquired credential</li>
 * </ol>
 *
 * @see OAuthProviderDemoConfig
 */
@SpringBootApplication
public class OAuthProviderDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(OAuthProviderDemoApplication.class, args);
    }
}
