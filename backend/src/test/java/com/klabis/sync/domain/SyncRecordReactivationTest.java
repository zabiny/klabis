package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * design.md D6/D7, "Domain Changes": a retired pairing can be deliberately brought
 * back into service. Reactivation discards the baseline it held at retirement —
 * that baseline describes agreement at a moment that may be long past — so the pass
 * that follows treats the pairing exactly like a fresh one (adopts the external
 * side).
 */
class SyncRecordReactivationTest {

    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-1");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "8123");
    private static final SyncProjectionHasher HASHER = projection -> SyncHash.of(String.valueOf(projection.hashCode()));

    @Test
    void reactivate_retiredRecord_clearsRetirementDiscardsBaselineAndMakesRecordDueAgain() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("agreed"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
        record.retire(Instant.now());

        ScheduleEffect effect = record.reactivate(Instant.now());

        assertThat(record.getStatus()).isEqualTo(SyncStatus.NEW);
        assertThat(record.getRetiredAt()).isNull();
        assertThat(record.getBaseline()).isNull();
        assertThat(effect.kind()).isNotEqualTo(ScheduleEffect.Kind.NO_CHANGE);
    }

    @Test
    void reactivate_recordNotRetired_rejected() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);

        assertThatThrownBy(() -> record.reactivate(Instant.now())).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Proves the baseline was genuinely discarded, not merely ignored: after
     * reactivation, {@code decide} must adopt the external side exactly as it would
     * for a brand-new pairing — even though local and external now hold values that
     * both differ from what the discarded baseline recorded. If the baseline had
     * survived, this would resolve to a conflict instead.
     */
    @Test
    void decide_afterReactivation_adoptsExternalLikeAFreshPairing() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot originalAgreed = SyncSnapshot.of(new TestProjection("original agreed"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, originalAgreed, originalAgreed, Instant.now());
        record.retire(Instant.now());

        record.reactivate(Instant.now());

        SyncSnapshot movedLocal = SyncSnapshot.of(new TestProjection("moved local"), HASHER);
        SyncSnapshot movedExternal = SyncSnapshot.of(new TestProjection("moved external"), HASHER);
        SyncDecision decision = record.decide(movedLocal, movedExternal, SyncCapabilities.pullOnlyCreating());

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.ADOPT_EXTERNAL);
    }

    private record TestProjection(String name) implements SyncProjection {
        @Override
        public SyncEntityType entityType() {
            return SyncEntityType.EVENT;
        }
    }
}
