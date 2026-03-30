package io.jaiclaw.identity.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates PKCE (Proof Key for Code Exchange) challenge pairs for OAuth flows.
 */
public final class PkceGenerator {

    private PkceGenerator() {}

    /**
     * PKCE verifier + S256 challenge pair.
     */
    public record PkceChallenge(String verifier, String challenge) {}

    /**
     * Generate a new PKCE challenge pair.
     * <ul>
     *   <li>Verifier: 64 hex characters (32 random bytes)</li>
     *   <li>Challenge: Base64url-encoded SHA-256 of the verifier</li>
     * </ul>
     */
    public static PkceChallenge generate() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String verifier = HexFormat.of().formatHex(random);

        try {
            byte[] sha256 = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.UTF_8));
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256);
            return new PkceChallenge(verifier, challenge);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Generate a random state parameter for CSRF protection. */
    public static String generateState() {
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        return HexFormat.of().formatHex(random);
    }
}
