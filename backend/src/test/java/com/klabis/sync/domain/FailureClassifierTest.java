package com.klabis.sync.domain;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

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
    void classify_dataIntegrityViolation_terminal() {
        // design.md D9: a database constraint refusing a creation (createLocal) is not
        // a transient fault — retrying it repeats the same rejection. Must be terminal,
        // never retryable, so a rejected creation stops the pairing instead of being
        // retried against the same rejection.
        assertThat(FailureClassifier.classify(new DataIntegrityViolationException("constraint violation")))
                .isEqualTo(FailureCategory.TERMINAL);
    }

    @Test
    void classify_duplicateKey_terminal() {
        // DuplicateKeyException is the more specific subtype JDBC drivers raise for a
        // unique-constraint violation — the exact shape createLocal's rejection takes
        // (design.md D9).
        assertThat(FailureClassifier.classify(new DuplicateKeyException("unique constraint violated")))
                .isEqualTo(FailureCategory.TERMINAL);
    }

    @Test
    void classify_dataIntegrityViolationWrappingUnrelatedCause_stillTerminal() {
        // Refactor-phase guard for D9: classification must key off the failure's own
        // shape (falling through to TERMINAL because it is not IOException- or
        // outage-shaped), never off something unrelated buried in its cause chain — a
        // DataIntegrityViolationException wrapping, say, a plain RuntimeException must
        // not accidentally read as retryable just because a cause happens to be
        // IOException-shaped further down. Here the cause chain has nothing retryable
        // in it at all, so this simply confirms the classification is not order- or
        // cause-dependent for this exception family.
        DataIntegrityViolationException wrapping = new DataIntegrityViolationException(
                "unique constraint violated", new IllegalStateException("duplicate external id"));

        assertThat(FailureClassifier.classify(wrapping)).isEqualTo(FailureCategory.TERMINAL);
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
