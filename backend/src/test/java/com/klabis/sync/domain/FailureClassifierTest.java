package com.klabis.sync.domain;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class FailureClassifierTest {

    @Test
    void classify_circuitBreakerOpen_outage() {
        CircuitBreaker breaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        Throwable callNotPermitted = io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException(breaker);

        assertThat(FailureClassifier.classify(callNotPermitted)).isEqualTo(FailureCategory.OUTAGE);
    }

    @Test
    void classify_connectionFailure_outage() {
        assertThat(FailureClassifier.classify(new ConnectException("refused"))).isEqualTo(FailureCategory.OUTAGE);
    }

    @Test
    void classify_unknownHost_outage() {
        assertThat(FailureClassifier.classify(new UnknownHostException("oris.example"))).isEqualTo(FailureCategory.OUTAGE);
    }

    @Test
    void classify_socketTimeout_outage() {
        assertThat(FailureClassifier.classify(new SocketTimeoutException("read timed out"))).isEqualTo(FailureCategory.OUTAGE);
    }

    @Test
    void classify_generalTimeout_outage() {
        assertThat(FailureClassifier.classify(new TimeoutException("timed out"))).isEqualTo(FailureCategory.OUTAGE);
    }

    @Test
    void classify_otherIOException_retryable() {
        assertThat(FailureClassifier.classify(new IOException("malformed response body"))).isEqualTo(FailureCategory.RETRYABLE);
    }

    @Test
    void classify_explicitlyMarkedRetryable_retryable() {
        assertThat(FailureClassifier.classify(new RetryableSyncFailureException("HTTP 503"))).isEqualTo(FailureCategory.RETRYABLE);
    }

    @Test
    void classify_illegalArgument_terminal() {
        assertThat(FailureClassifier.classify(new IllegalArgumentException("bad data"))).isEqualTo(FailureCategory.TERMINAL);
    }

    @Test
    void classify_nullPointer_terminal() {
        assertThat(FailureClassifier.classify(new NullPointerException())).isEqualTo(FailureCategory.TERMINAL);
    }

    @Test
    void classify_illegalState_terminal() {
        assertThat(FailureClassifier.classify(new IllegalStateException("unexpected state"))).isEqualTo(FailureCategory.TERMINAL);
    }

    @Test
    void classify_mixedChain_outageAnywhereInChainWins() {
        // Precedence is deliberately OUTAGE > RETRYABLE > TERMINAL over the WHOLE cause
        // chain, not "whichever category the nearest cause matches" — an outer wrapper
        // that is itself only IOException-shaped (retryable) but wraps a genuine
        // connection failure must still count as an outage, since the root cause is
        // what actually happened talking to the external system.
        Throwable outerRetryableWrappingOutageCause = new IOException("request failed", new ConnectException("refused"));

        assertThat(FailureClassifier.classify(outerRetryableWrappingOutageCause)).isEqualTo(FailureCategory.OUTAGE);
    }

    @Test
    void classify_mixedChain_retryableBeatsTerminalAnywhereInChain() {
        // Symmetric case: a terminal-shaped wrapper (e.g. a generic runtime exception)
        // around a retryable cause is still retryable — the retryable cause is real
        // information the terminal-shaped wrapper does not override.
        Throwable outerTerminalWrappingRetryableCause = new IllegalStateException("mapping failed", new IOException("malformed body"));

        assertThat(FailureClassifier.classify(outerTerminalWrappingRetryableCause)).isEqualTo(FailureCategory.RETRYABLE);
    }
}
