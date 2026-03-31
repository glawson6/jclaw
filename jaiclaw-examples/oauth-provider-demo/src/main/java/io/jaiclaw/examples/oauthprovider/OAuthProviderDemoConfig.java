package io.jaiclaw.examples.oauthprovider;

import io.jaiclaw.agent.tenant.DefaultTenantChatModelFactory;
import io.jaiclaw.agent.tenant.TenantChatModelFactory;
import io.jaiclaw.config.JaiClawProperties;
import io.jaiclaw.identity.auth.*;
import io.jaiclaw.identity.oauth.OAuthFlowManager;
import io.jaiclaw.identity.oauth.OAuthProviderConfig;
import io.jaiclaw.identity.oauth.provider.*;
import io.jaiclaw.identity.secret.SecretRefResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wires the jaiclaw-identity OAuth infrastructure and overrides the default
 * {@link TenantChatModelFactory} to create {@link OpenAiChatModel} instances
 * using an OAuth-obtained access token instead of a static API key.
 *
 * <p>The agent cannot operate until the user completes an OAuth login via
 * the REST endpoint exposed by {@link OAuthLoginController}.
 */
@Configuration
public class OAuthProviderDemoConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuthProviderDemoConfig.class);

    static final Path STATE_DIR = Paths.get(
            System.getProperty("user.home"), ".jaiclaw");
    static final Path AGENT_DIR = STATE_DIR.resolve("agents/default/agent");

    // --- jaiclaw-identity bean wiring ---

    @Bean
    AuthProfileStoreManager authProfileStoreManager() {
        return new AuthProfileStoreManager(STATE_DIR);
    }

    @Bean
    OAuthFlowManager oauthFlowManager(AuthProfileStoreManager storeManager) {
        Map<String, OAuthProviderConfig> providers = new LinkedHashMap<>();
        providers.put("openai-codex", OpenAiCodexOAuthProvider.config());
        providers.put("google-gemini-cli", GoogleGeminiOAuthProvider.config());
        providers.put("chutes", ChutesOAuthProvider.config());
        providers.put("qwen-portal", QwenDeviceCodeProvider.config());
        providers.put("minimax-portal", MiniMaxDeviceCodeProvider.config());
        return new OAuthFlowManager(providers, storeManager);
    }

    @Bean
    ProviderTokenRefresherRegistry tokenRefresherRegistry() {
        return new ProviderTokenRefresherRegistry(List.of(
                new GenericOAuthTokenRefresher(GoogleGeminiOAuthProvider.config()),
                new GenericOAuthTokenRefresher(ChutesOAuthProvider.config())
        ));
    }

    @Bean
    AuthProfileResolver authProfileResolver(AuthProfileStoreManager storeManager,
                                            ProviderTokenRefresherRegistry registry) {
        return new AuthProfileResolver(storeManager, new SecretRefResolver(), registry);
    }

    // --- Credential holder: written by OAuthLoginController, read by model factory ---

    @Bean
    AtomicReference<ResolvedCredential> activeCredential() {
        return new AtomicReference<>(null);
    }

    // --- Custom TenantChatModelFactory that uses the OAuth credential ---

    /**
     * Overrides the default factory. Creates an {@link OpenAiChatModel} using
     * the OAuth access token stored in {@link #activeCredential()}.
     *
     * <p>If no credential has been set yet, the factory throws so the agent
     * knows it cannot operate until login completes.
     */
    @Bean
    TenantChatModelFactory tenantChatModelFactory(JaiClawProperties properties,
                                                  AtomicReference<ResolvedCredential> activeCredential) {
        return new DefaultTenantChatModelFactory(properties.models(), request -> {
            ResolvedCredential cred = activeCredential.get();
            if (cred == null) {
                throw new IllegalStateException(
                        "No OAuth credential available. Call POST /api/oauth/login/{providerId} first.");
            }

            log.info("Creating OpenAI ChatModel with OAuth credential for provider '{}' ({})",
                    cred.provider(), cred.email());

            // Build OpenAiApi with the OAuth access token as the API key
            OpenAiApi api = OpenAiApi.builder()
                    .apiKey(cred.apiKey())
                    .build();

            String model = request.modelId() != null ? request.modelId() : "gpt-4o";
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(model)
                    .temperature(request.temperature())
                    .maxTokens(request.maxTokens())
                    .build();

            return OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(options)
                    .build();
        });
    }
}
