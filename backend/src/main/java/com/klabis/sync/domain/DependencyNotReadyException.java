package com.klabis.sync.domain;

/**
 * A retryable failure caused by the state of the data, not by the external system being unreachable - e.g. a
 * record that refers to something another synchronisation has not brought in yet. It is retried like any
 * {@link RetryableSyncFailureException}, but it says nothing about the health of the external system, so it does
 * not count towards the adapter circuit breaker.
 */
public class DependencyNotReadyException extends RetryableSyncFailureException {

    public DependencyNotReadyException(String message) {
        super(message);
    }
}
