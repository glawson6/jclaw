package io.jaiclaw.camel;

/**
 * Functional interface decoupling the Camel adapter from the gateway module.
 * Implementations synchronously handle a message and return the agent response.
 */
@FunctionalInterface
public interface GatewayServiceAccessor {

    /**
     * Handle a message synchronously and return the agent response.
     *
     * @param channelId the channel identifier
     * @param accountId the account identifier
     * @param peerId    the peer (sender) identifier
     * @param content   the message content
     * @return the agent response text
     */
    String handleSync(String channelId, String accountId, String peerId, String content);
}
