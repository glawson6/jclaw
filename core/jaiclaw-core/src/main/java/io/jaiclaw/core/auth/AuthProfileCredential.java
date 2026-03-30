package io.jaiclaw.core.auth;

/**
 * Sealed credential hierarchy matching OpenClaw's "type" discriminator.
 * <p>
 * Three variants:
 * <ul>
 *   <li>{@link ApiKeyCredential} — static API key (inline or via {@link SecretRef})</li>
 *   <li>{@link TokenCredential} — static bearer/PAT token, optionally with expiry</li>
 *   <li>{@link OAuthCredential} — refreshable OAuth access + refresh tokens</li>
 * </ul>
 */
public sealed interface AuthProfileCredential
        permits ApiKeyCredential, TokenCredential, OAuthCredential {

    /** Provider identifier (e.g. "anthropic", "openai-codex", "chutes"). */
    String provider();

    /** Email associated with this credential (nullable). */
    String email();
}
