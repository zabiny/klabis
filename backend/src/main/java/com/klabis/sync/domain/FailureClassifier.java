package com.klabis.sync.domain;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Classifies a pass failure by exception type (design.md D10, D11).
 * <p>
 * Outage-shaped failures — the circuit breaker refusing the call, connection
 * failures, timeouts — count toward nothing. Other transport and server-side errors
 * are retryable. Everything else is terminal on the spot.
 * <p>
 * Classification walks the cause chain: an HTTP client commonly wraps a checked
 * {@code IOException} in an unchecked wrapper (e.g. {@link java.io.UncheckedIOException}),
 * and the outage-or-retryable distinction lives on the wrapped cause, not the wrapper.
 */
public final class FailureClassifier {

    private static final int MAX_CAUSE_DEPTH = 10;

    private FailureClassifier() {
    }

    public static FailureCategory classify(Throwable failure) {
        if (anyInChain(failure, FailureClassifier::isOutageShaped)) {
            return FailureCategory.OUTAGE;
        }
        if (anyInChain(failure, FailureClassifier::isRetryable)) {
            return FailureCategory.RETRYABLE;
        }
        return FailureCategory.TERMINAL;
    }

    private static boolean anyInChain(Throwable failure, java.util.function.Predicate<Throwable> test) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++, current = current.getCause()) {
            if (test.test(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOutageShaped(Throwable failure) {
        return failure instanceof CallNotPermittedException
                || failure instanceof ConnectException
                || failure instanceof UnknownHostException
                || failure instanceof SocketTimeoutException
                || failure instanceof TimeoutException;
    }

    /**
     * Other transport and server-side errors: the connection was established and the
     * external system responded, but with something that may pass — a 5xx, a
     * malformed response, an I/O hiccup reading the body. Bad-data or client-error
     * shaped failures (design.md: "a client error, a data problem") are anything not
     * covered here, and fall through to {@link FailureCategory#TERMINAL}.
     */
    private static boolean isRetryable(Throwable failure) {
        return failure instanceof IOException || failure instanceof RetryableSyncFailureException;
    }
}
