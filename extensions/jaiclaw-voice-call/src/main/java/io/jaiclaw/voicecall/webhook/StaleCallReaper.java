package io.jaiclaw.voicecall.webhook;

import io.jaiclaw.voicecall.manager.CallManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically cleans up stale calls that have exceeded their maximum duration.
 */
public class StaleCallReaper {

    private static final Logger log = LoggerFactory.getLogger(StaleCallReaper.class);

    private final CallManager callManager;

    public StaleCallReaper(CallManager callManager) {
        this.callManager = callManager;
    }

    /**
     * Run every 60 seconds to check for stale calls.
     */
    @Scheduled(fixedRate = 60_000)
    public void reap() {
        try {
            callManager.reapStaleCalls();
        } catch (Exception e) {
            log.error("Error during stale call reaping: {}", e.getMessage());
        }
    }
}
