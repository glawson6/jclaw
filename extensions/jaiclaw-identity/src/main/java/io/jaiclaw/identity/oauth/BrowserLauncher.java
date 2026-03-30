package io.jaiclaw.identity.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Platform-specific URL opener for OAuth browser flows.
 * Uses {@code open} (macOS), {@code xdg-open} (Linux), or {@code Desktop.browse()} fallback.
 */
public final class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    private BrowserLauncher() {}

    /**
     * Open a URL in the user's default browser.
     *
     * @param url the URL to open
     * @return true if the browser was launched successfully
     */
    public static boolean open(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                return true;
            } else if (os.contains("linux")) {
                new ProcessBuilder("xdg-open", url).start();
                return true;
            } else {
                // Windows or other — try Desktop API
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(URI.create(url));
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to open browser: {}", e.getMessage());
        }
        return false;
    }
}
