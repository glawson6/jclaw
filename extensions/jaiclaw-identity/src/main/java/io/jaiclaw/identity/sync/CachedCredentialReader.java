package io.jaiclaw.identity.sync;

import io.jaiclaw.core.auth.AuthProfileConstants;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Wraps a credential reader with an in-process TTL cache.
 *
 * @param <T> the credential type
 */
public class CachedCredentialReader<T> {

    private final Supplier<Optional<T>> reader;
    private final long ttlMs;
    private volatile T cached;
    private volatile long readAt;

    public CachedCredentialReader(Supplier<Optional<T>> reader) {
        this(reader, AuthProfileConstants.EXTERNAL_CLI_SYNC_TTL_MS);
    }

    public CachedCredentialReader(Supplier<Optional<T>> reader, long ttlMs) {
        this.reader = reader;
        this.ttlMs = ttlMs;
    }

    /** Read from cache or delegate to the underlying reader. */
    public Optional<T> read() {
        long now = System.currentTimeMillis();
        if (cached != null && (now - readAt) < ttlMs) {
            return Optional.of(cached);
        }
        Optional<T> result = reader.get();
        result.ifPresent(value -> {
            this.cached = value;
            this.readAt = now;
        });
        return result;
    }

    /** Invalidate the cache. */
    public void invalidate() {
        this.cached = null;
        this.readAt = 0;
    }
}
