package com.klabis.sync.application;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Wraps a call to a {@link com.klabis.sync.domain.SynchronizationAdapter} with the
 * named {@code sync-adapter} Resilience4j retry and circuit breaker instances
 * (design.md D10, D11), both configured under the {@code resilience4j} block in
 * {@code application.yml}.
 * <p>
 * The retry absorbs transient transport blips within one attempt; the breaker opens
 * after repeated failures so the rest of a pass's records are left untouched
 * (design.md D11). The original exception propagates on final failure — classifying
 * it (outage / retryable / terminal) is
 * {@link com.klabis.sync.domain.FailureClassifier}'s job, not this executor's.
 */
@Component
class ResilientAdapterExecutor {

    static final String INSTANCE_NAME = "sync-adapter";

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    ResilientAdapterExecutor(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE_NAME);
        this.retry = retryRegistry.retry(INSTANCE_NAME);
    }

    <T> T call(Supplier<T> adapterCall) {
        Supplier<T> decorated = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, adapterCall));
        return decorated.get();
    }

    void run(Runnable adapterCall) {
        call(() -> {
            adapterCall.run();
            return null;
        });
    }

    /**
     * Whether the breaker currently refuses calls (design.md D11) — checked before
     * starting a pass so the remaining records in a scan are left untouched rather
     * than each attempting, failing and consuming its retry budget.
     */
    boolean isOpen() {
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN
                || circuitBreaker.getState() == CircuitBreaker.State.FORCED_OPEN;
    }
}
