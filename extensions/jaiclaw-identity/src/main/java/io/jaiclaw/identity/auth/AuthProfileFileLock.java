package io.jaiclaw.identity.auth;

import io.jaiclaw.core.auth.AuthProfileConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * File-based advisory lock for auth profile store operations.
 * Uses a separate lock file ({@code auth-profiles.json.lock}) with exponential backoff retry.
 */
public final class AuthProfileFileLock {

    private static final Logger log = LoggerFactory.getLogger(AuthProfileFileLock.class);

    private AuthProfileFileLock() {}

    /**
     * Execute an action under an exclusive file lock.
     *
     * @param lockFile path to the lock file
     * @param action   action to execute while holding the lock
     * @param <T>      return type
     * @return result of the action
     * @throws AuthProfileLockException if the lock cannot be acquired
     */
    public static <T> T withLock(Path lockFile, Supplier<T> action) {
        try {
            Files.createDirectories(lockFile.getParent());
        } catch (IOException e) {
            throw new AuthProfileLockException("Failed to create lock directory: " + lockFile.getParent(), e);
        }

        cleanStaleLock(lockFile);

        for (int attempt = 0; attempt <= AuthProfileConstants.LOCK_MAX_RETRIES; attempt++) {
            try (FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                FileLock lock = channel.tryLock();
                if (lock != null) {
                    try {
                        return action.get();
                    } finally {
                        lock.release();
                    }
                }
            } catch (OverlappingFileLockException e) {
                // Another thread in this JVM holds the lock — retry
            } catch (IOException e) {
                throw new AuthProfileLockException("Lock I/O error on " + lockFile, e);
            }

            if (attempt < AuthProfileConstants.LOCK_MAX_RETRIES) {
                long delay = computeBackoff(attempt);
                log.debug("Lock busy on {}, retrying in {}ms (attempt {}/{})",
                        lockFile, delay, attempt + 1, AuthProfileConstants.LOCK_MAX_RETRIES);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AuthProfileLockException("Interrupted while waiting for lock: " + lockFile, ie);
                }
            }
        }

        throw new AuthProfileLockException(
                "Failed to acquire lock after " + AuthProfileConstants.LOCK_MAX_RETRIES + " retries: " + lockFile);
    }

    /**
     * Execute a void action under an exclusive file lock.
     */
    public static void withLock(Path lockFile, Runnable action) {
        withLock(lockFile, () -> {
            action.run();
            return null;
        });
    }

    private static long computeBackoff(int attempt) {
        long delay = AuthProfileConstants.LOCK_BASE_DELAY_MS * (1L << attempt);
        delay = Math.min(delay, AuthProfileConstants.LOCK_MAX_DELAY_MS);
        // Add jitter: 50-150% of computed delay
        long jitter = ThreadLocalRandom.current().nextLong(delay / 2, delay + delay / 2 + 1);
        return jitter;
    }

    private static void cleanStaleLock(Path lockFile) {
        try {
            if (Files.exists(lockFile)) {
                long lastModified = Files.getLastModifiedTime(lockFile).toMillis();
                long age = System.currentTimeMillis() - lastModified;
                if (age > AuthProfileConstants.LOCK_STALE_TIMEOUT_MS) {
                    log.warn("Removing stale lock file (age {}ms): {}", age, lockFile);
                    Files.deleteIfExists(lockFile);
                }
            }
        } catch (IOException e) {
            log.debug("Could not check/clean stale lock file: {}", lockFile, e);
        }
    }

    /**
     * Exception thrown when a file lock cannot be acquired.
     */
    public static class AuthProfileLockException extends RuntimeException {
        public AuthProfileLockException(String message) {
            super(message);
        }

        public AuthProfileLockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
