package io.jaiclaw.voicecall.manager;

import io.jaiclaw.voicecall.config.VoiceCallProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Evaluates whether an inbound call should be accepted based on configured policy.
 */
public class InboundPolicy {

    private static final Logger log = LoggerFactory.getLogger(InboundPolicy.class);
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    private final VoiceCallProperties.InboundProperties inbound;

    public InboundPolicy(VoiceCallProperties.InboundProperties inbound) {
        this.inbound = inbound;
    }

    /**
     * Determine whether to accept an inbound call from the given number.
     *
     * @param fromNumber caller's phone number
     * @return true if the call should be accepted
     */
    public boolean shouldAcceptInbound(String fromNumber) {
        if (inbound == null) {
            return false;
        }

        String policy = inbound.policy();
        if (policy == null || "disabled".equalsIgnoreCase(policy)) {
            log.debug("Inbound calls disabled by policy");
            return false;
        }

        if ("open".equalsIgnoreCase(policy)) {
            return true;
        }

        if ("allowlist".equalsIgnoreCase(policy)) {
            String normalized = normalizePhoneNumber(fromNumber);
            boolean allowed = inbound.allowedFrom().stream()
                    .map(InboundPolicy::normalizePhoneNumber)
                    .anyMatch(n -> n.equals(normalized));

            if (!allowed) {
                log.info("Inbound call from {} rejected by allowlist", fromNumber);
            }
            return allowed;
        }

        log.warn("Unknown inbound policy: {}", policy);
        return false;
    }

    /**
     * Normalize a phone number to E.164 format for comparison.
     * Strips whitespace, dashes, parentheses.
     */
    static String normalizePhoneNumber(String number) {
        if (number == null) return "";
        String cleaned = number.replaceAll("[\\s\\-().]", "");
        // If it doesn't start with +, assume it's already clean
        if (!cleaned.startsWith("+") && cleaned.length() >= 10) {
            cleaned = "+" + cleaned;
        }
        return cleaned;
    }
}
