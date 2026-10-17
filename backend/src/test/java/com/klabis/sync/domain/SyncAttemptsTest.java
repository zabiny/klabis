package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SyncAttempts.failedSinceLastSuccess")
class SyncAttemptsTest {

    private static final SyncRecordId RECORD = SyncRecordId.newId();

    private static SyncAttempt attempt(SyncOutcome outcome) {
        return SyncAttempt.record(RECORD, Instant.parse("2026-01-01T10:00:00Z"), SyncTriggerKind.SCHEDULED, null,
                outcome, SyncHash.of("h"), SyncHash.of("h"), null, null);
    }

    @ParameterizedTest
    @EnumSource(value = SyncOutcome.class, names = {"SUCCESS", "RESET"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("every outcome except SUCCESS, RESET and OUTAGE counts as a failure")
    void countsFailureOutcomes(SyncOutcome outcome) {
        int expected = outcome == SyncOutcome.OUTAGE ? 0 : 1;

        assertThat(SyncAttempts.failedSinceLastSuccess(List.of(attempt(outcome)))).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(value = SyncOutcome.class, names = {"SUCCESS", "RESET"})
    @DisplayName("counting stops at a success or reset")
    void stopsAtSuccessOrReset(SyncOutcome boundary) {
        List<SyncAttempt> newestFirst = List.of(
                attempt(SyncOutcome.FAILED), attempt(SyncOutcome.OUTAGE), attempt(SyncOutcome.CONFLICT),
                attempt(boundary), attempt(SyncOutcome.FAILED));

        assertThat(SyncAttempts.failedSinceLastSuccess(newestFirst)).isEqualTo(2);
    }

    @Test
    @DisplayName("an empty history has no failures")
    void emptyHistory() {
        assertThat(SyncAttempts.failedSinceLastSuccess(List.of())).isZero();
    }
}
