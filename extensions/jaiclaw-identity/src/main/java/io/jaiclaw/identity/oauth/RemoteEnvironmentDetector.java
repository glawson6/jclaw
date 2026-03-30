package io.jaiclaw.identity.oauth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects whether the current environment is remote/headless (SSH, VPS, Codespaces, etc.).
 * Used to decide between browser-based OAuth and manual URL paste flow.
 */
public final class RemoteEnvironmentDetector {

    private RemoteEnvironmentDetector() {}

    /**
     * Returns {@code true} if the current environment is remote/headless
     * (no local browser available for OAuth redirect).
     */
    public static boolean isRemote() {
        // SSH session
        if (System.getenv("SSH_CLIENT") != null) return true;
        if (System.getenv("SSH_TTY") != null) return true;
        if (System.getenv("SSH_CONNECTION") != null) return true;

        // Dev containers
        if (System.getenv("REMOTE_CONTAINERS") != null) return true;
        if (System.getenv("CODESPACES") != null) return true;

        // Headless Linux (not WSL)
        if (isHeadlessLinux()) return true;

        return false;
    }

    private static boolean isHeadlessLinux() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) return false;
        if (isWsl()) return false;
        return System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null;
    }

    private static boolean isWsl() {
        try {
            Path procVersion = Path.of("/proc/version");
            if (Files.exists(procVersion)) {
                String content = Files.readString(procVersion).toLowerCase();
                return content.contains("microsoft") || content.contains("wsl");
            }
        } catch (IOException ignored) {}
        return false;
    }
}
