package com.klabis.sync.domain;

/**
 * How a failed pass attempt is classified (design.md D10, D11).
 */
public enum FailureCategory {
    /**
     * The external system is unavailable — the circuit breaker refusing the call, a
     * connection failure, a timeout. Counts toward nothing: neither the derived
     * failure count nor the backoff delay grows from an outage (design.md D11).
     */
    OUTAGE,
    /**
     * A transport or server-side error that may pass on its own. Counts toward the
     * derived failure count and the growing backoff delay (design.md D10).
     */
    RETRYABLE,
    /**
     * Anything else — a client error, a data problem, a programming error. Terminal on
     * the spot: the record becomes terminally failed on this single attempt without
     * waiting for the retry limit, since retrying an error that will not resolve
     * itself only delays the manager finding out (design.md D10).
     */
    TERMINAL
}
