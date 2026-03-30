package io.jaiclaw.identity.oauth;

/** Thrown when an OAuth flow fails. */
public class OAuthFlowException extends Exception {
    public OAuthFlowException(String message) { super(message); }
    public OAuthFlowException(String message, Throwable cause) { super(message, cause); }
}
