package io.jaiclaw.identity.auth;

/**
 * Thrown when an OAuth token refresh fails.
 */
public class TokenRefreshException extends Exception {

    private final String provider;

    public TokenRefreshException(String provider, String message) {
        super(message);
        this.provider = provider;
    }

    public TokenRefreshException(String provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public String provider() {
        return provider;
    }
}
