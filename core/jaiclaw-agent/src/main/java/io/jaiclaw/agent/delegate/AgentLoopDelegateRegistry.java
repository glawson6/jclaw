package io.jaiclaw.agent.delegate;

import io.jaiclaw.config.TenantAgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Registry of {@link AgentLoopDelegate} implementations. Resolves the first
 * delegate that can handle a given tenant configuration.
 *
 * <p>Supports both eager (list) and lazy (supplier) delegate resolution. The lazy
 * form is preferred in auto-configuration to avoid ordering issues where the registry
 * is created before all delegate beans have been registered.
 */
public class AgentLoopDelegateRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopDelegateRegistry.class);

    private final Supplier<List<AgentLoopDelegate>> delegatesSupplier;
    private volatile List<AgentLoopDelegate> resolvedDelegates;

    public AgentLoopDelegateRegistry(List<AgentLoopDelegate> delegates) {
        List<AgentLoopDelegate> resolved = delegates != null ? delegates : List.of();
        this.delegatesSupplier = () -> resolved;
        this.resolvedDelegates = resolved;
    }

    /**
     * Lazy constructor — delegates are resolved on first use.
     * This avoids auto-configuration ordering issues where the registry bean
     * is created before delegate beans have been registered.
     */
    public AgentLoopDelegateRegistry(Supplier<List<AgentLoopDelegate>> delegatesSupplier) {
        this.delegatesSupplier = delegatesSupplier;
    }

    private List<AgentLoopDelegate> delegates() {
        if (resolvedDelegates == null) {
            synchronized (this) {
                if (resolvedDelegates == null) {
                    List<AgentLoopDelegate> resolved = delegatesSupplier.get();
                    resolvedDelegates = resolved != null ? resolved : List.of();
                    log.info("AgentLoopDelegateRegistry resolved {} delegate(s): {}",
                            resolvedDelegates.size(),
                            resolvedDelegates.stream()
                                    .map(AgentLoopDelegate::delegateId)
                                    .toList());
                }
            }
        }
        return resolvedDelegates;
    }

    /**
     * Find the first delegate that can handle this tenant config.
     * Returns empty if no delegate is enabled or matches.
     */
    public Optional<AgentLoopDelegate> resolve(TenantAgentConfig config) {
        if (config == null || config.loopDelegate() == null || !config.loopDelegate().enabled()) {
            return Optional.empty();
        }
        return delegates().stream()
                .filter(d -> d.canHandle(config))
                .findFirst();
    }

    public int size() {
        return delegates().size();
    }
}
