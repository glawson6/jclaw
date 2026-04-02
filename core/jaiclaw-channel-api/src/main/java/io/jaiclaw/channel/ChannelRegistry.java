package io.jaiclaw.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of available channel adapters. The gateway uses this to route
 * outbound messages and to start/stop all adapters during lifecycle events.
 */
public class ChannelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ChannelRegistry.class);

    private final Map<String, ChannelAdapter> adapters = new ConcurrentHashMap<>();
    private final Set<String> statelessChannels = ConcurrentHashMap.newKeySet();

    public void register(ChannelAdapter adapter) {
        var previous = adapters.putIfAbsent(adapter.channelId(), adapter);
        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate channel adapter for: " + adapter.channelId());
        }
        log.info("Registered channel adapter: {} ({})", adapter.channelId(), adapter.displayName());
    }

    public Optional<ChannelAdapter> get(String channelId) {
        return Optional.ofNullable(adapters.get(channelId));
    }

    public Collection<ChannelAdapter> all() {
        return Collections.unmodifiableCollection(adapters.values());
    }

    public Set<String> channelIds() {
        return Collections.unmodifiableSet(adapters.keySet());
    }

    public int size() {
        return adapters.size();
    }

    public boolean contains(String channelId) {
        return adapters.containsKey(channelId);
    }

    public void unregister(String channelId) {
        var adapter = adapters.remove(channelId);
        if (adapter != null && adapter.isRunning()) {
            adapter.stop();
        }
    }

    /**
     * Mark a channel as stateless via configuration (property-driven).
     * Stateless channels create ephemeral sessions with no history persistence.
     */
    public void markStateless(String channelId) {
        statelessChannels.add(channelId);
        log.info("Marked channel as stateless: {}", channelId);
    }

    /**
     * Check whether a channel is stateless. Returns true if either:
     * <ul>
     *   <li>The channel was marked stateless via {@link #markStateless(String)}</li>
     *   <li>The adapter's {@link ChannelAdapter#isStateless()} returns true</li>
     * </ul>
     */
    public boolean isStateless(String channelId) {
        if (statelessChannels.contains(channelId)) {
            return true;
        }
        ChannelAdapter adapter = adapters.get(channelId);
        return adapter != null && adapter.isStateless();
    }

    /**
     * Start all registered adapters with the given message handler.
     */
    public void startAll(ChannelMessageHandler handler) {
        adapters.values().forEach(adapter -> {
            try {
                adapter.start(handler);
                log.info("Started channel adapter: {}", adapter.channelId());
            } catch (Exception e) {
                log.error("Failed to start channel adapter: {}", adapter.channelId(), e);
            }
        });
    }

    /**
     * Stop all running adapters.
     */
    public void stopAll() {
        adapters.values().forEach(adapter -> {
            try {
                if (adapter.isRunning()) {
                    adapter.stop();
                    log.info("Stopped channel adapter: {}", adapter.channelId());
                }
            } catch (Exception e) {
                log.error("Failed to stop channel adapter: {}", adapter.channelId(), e);
            }
        });
    }
}
