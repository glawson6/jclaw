package io.jaiclaw.examples.oauthprovider;

import io.jaiclaw.identity.auth.AuthProfileResolver;
import io.jaiclaw.identity.auth.AuthProfileStoreManager;
import io.jaiclaw.identity.auth.ResolvedCredential;
import io.jaiclaw.identity.oauth.OAuthFlowException;
import io.jaiclaw.identity.oauth.OAuthFlowManager;
import io.jaiclaw.identity.oauth.OAuthFlowResult;
import io.jaiclaw.identity.oauth.OAuthProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST controller that exposes OAuth login endpoints.
 * After a successful login, the credential is stored and made available
 * to the {@link io.jaiclaw.agent.tenant.TenantChatModelFactory} so the
 * agent can start using the LLM.
 */
@RestController
@RequestMapping("/api/oauth")
public class OAuthLoginController {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginController.class);

    private final OAuthFlowManager oauthFlowManager;
    private final AuthProfileResolver authProfileResolver;
    private final AtomicReference<ResolvedCredential> activeCredential;

    public OAuthLoginController(OAuthFlowManager oauthFlowManager,
                                AuthProfileResolver authProfileResolver,
                                AtomicReference<ResolvedCredential> activeCredential) {
        this.oauthFlowManager = oauthFlowManager;
        this.authProfileResolver = authProfileResolver;
        this.activeCredential = activeCredential;
    }

    /**
     * List available OAuth providers.
     */
    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, String>>> listProviders() {
        List<Map<String, String>> result = new ArrayList<>();
        for (String id : oauthFlowManager.listProviders()) {
            oauthFlowManager.getProvider(id).ifPresent(config -> {
                result.add(Map.of(
                        "providerId", config.providerId(),
                        "flowType", config.flowType().name()
                ));
            });
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Trigger an OAuth login flow. For PKCE providers this opens a browser;
     * for device code providers this returns a verification URL and code.
     *
     * <p>On success, the credential is stored in the auth profile store
     * and activated as the LLM API key.
     */
    @PostMapping("/login/{providerId}")
    public ResponseEntity<Map<String, Object>> login(@PathVariable String providerId) {
        log.info("OAuth login requested for provider: {}", providerId);
        StringBuilder output = new StringBuilder();

        try {
            OAuthFlowResult result = oauthFlowManager.login(
                    providerId,
                    OAuthProviderDemoConfig.AGENT_DIR,
                    msg -> {
                        output.append(msg).append("\n");
                        log.info("[OAuth] {}", msg);
                    },
                    () -> {
                        // Remote/VPS mode — in production, this would prompt
                        // via a UI. For this demo, local browser flow is expected.
                        log.warn("Remote mode detected — paste-back not supported in this demo");
                        return "";
                    }
            );

            // Build the profile ID the same way OAuthFlowManager stores it
            String profileId = providerId + ":" + (result.email() != null ? result.email() : "default");

            // Resolve the credential (this also verifies it's usable)
            ResolvedCredential resolved = authProfileResolver.resolve(profileId,
                    OAuthProviderDemoConfig.AGENT_DIR);

            // Activate it — the TenantChatModelFactory will now use this token
            activeCredential.set(resolved);
            log.info("OAuth credential activated for provider '{}' ({})", providerId, resolved.email());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "authenticated");
            response.put("provider", providerId);
            response.put("email", result.email());
            response.put("message", "OAuth login successful. The agent can now use the LLM. "
                    + "Send messages to POST /api/chat.");
            return ResponseEntity.ok(response);

        } catch (OAuthFlowException e) {
            log.error("OAuth login failed for {}: {}", providerId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "provider", providerId,
                    "error", e.getMessage(),
                    "output", output.toString()
            ));
        } catch (AuthProfileResolver.CredentialResolutionException e) {
            log.error("Credential resolution failed after OAuth: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "provider", providerId,
                    "error", "OAuth succeeded but credential resolution failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Check current authentication status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        ResolvedCredential cred = activeCredential.get();
        if (cred == null) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", false,
                    "message", "No credential active. Call POST /api/oauth/login/{providerId} to authenticate."
            ));
        }
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "provider", cred.provider(),
                "email", cred.email() != null ? cred.email() : "n/a"
        ));
    }
}
