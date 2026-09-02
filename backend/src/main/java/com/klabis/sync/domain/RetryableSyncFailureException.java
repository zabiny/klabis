package com.klabis.sync.domain;

/**
 * An adapter throws this to mark a failure explicitly retryable (design.md D10) when
 * the underlying exception type does not already say so — e.g. an HTTP client
 * exception for a 5xx response, which is a server-side error that may pass, but is
 * not an {@link java.io.IOException}.
 */
public class RetryableSyncFailureException extends RuntimeException {

    public RetryableSyncFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableSyncFailureException(String message) {
        super(message);
    }
}
