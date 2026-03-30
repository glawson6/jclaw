package io.jaiclaw.identity.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Reads generic passwords from the macOS Keychain via the {@code security} command.
 * No-op on non-macOS platforms.
 */
public class KeychainReader {

    private static final Logger log = LoggerFactory.getLogger(KeychainReader.class);
    private static final boolean IS_MACOS = System.getProperty("os.name", "").toLowerCase().contains("mac");

    /**
     * Read a generic password from the macOS Keychain.
     *
     * @param service the keychain service name
     * @param account the keychain account name
     * @return the password value, or empty if not found or not on macOS
     */
    public Optional<String> readGenericPassword(String service, String account) {
        if (!IS_MACOS) return Optional.empty();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "security", "find-generic-password",
                    "-s", service,
                    "-a", account,
                    "-w"
            );
            pb.redirectErrorStream(false);
            Process process = pb.start();

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.debug("Keychain read timed out for service '{}'", service);
                return Optional.empty();
            }

            if (process.exitValue() != 0) {
                // Exit code 44 = item not found (normal)
                log.debug("Keychain item not found: service='{}', account='{}'", service, account);
                return Optional.empty();
            }

            String value = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.debug("Failed to read keychain: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
