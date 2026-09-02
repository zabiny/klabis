package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncRecordDirectionResolutionTest {

    private record TestProjection(String name) implements SyncProjection {
        @Override
        public SyncEntityType entityType() {
            return SyncEntityType.EVENT;
        }
    }

    /**
     * A deterministic in-memory hasher: fine for testing pure direction-resolution
     * logic, which only cares whether two snapshots' hashes are equal, not about
     * canonical JSON serialisation (covered separately by SyncProjectionCodecTest).
     */
    private static final SyncProjectionHasher HASHER = projection -> SyncHash.of(String.valueOf(projection.hashCode()));

    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-1");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "8123");

    private static final SyncCapabilities BOTH_WRITABLE =
            new SyncCapabilities(true, true, true, true, false, false, false);
    private static final SyncCapabilities INWARD_ONLY =
            new SyncCapabilities(true, true, true, false, false, false, false);

    @Test
    void decide_newlyEnrolledRecordWithNoBaseline_adoptsExternal() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);

        SyncSnapshot local = SyncSnapshot.of(new TestProjection("local value"), HASHER);
        SyncSnapshot external = SyncSnapshot.of(new TestProjection("external value"), HASHER);

        SyncDecision decision = record.decide(local, external, BOTH_WRITABLE);

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.ADOPT_EXTERNAL);
        assertThat(decision.direction()).isEqualTo(SyncDirection.INWARD);
    }

    @Test
    void decide_neitherSideChangedSinceBaseline_nothingToDo() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("agreed value"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncDecision decision = record.decide(agreed, agreed, BOTH_WRITABLE);

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.NOTHING_TO_DO);
    }

    @Test
    void decide_onlyExternalChangedSinceBaseline_writesInward() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("agreed value"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedExternal = SyncSnapshot.of(new TestProjection("new external value"), HASHER);

        SyncDecision decision = record.decide(agreed, changedExternal, BOTH_WRITABLE);

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.WRITE);
        assertThat(decision.direction()).isEqualTo(SyncDirection.INWARD);
    }

    @Test
    void decide_onlyLocalChangedAndOutwardWriteAvailable_writesOutward() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("agreed value"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedLocal = SyncSnapshot.of(new TestProjection("new local value"), HASHER);

        SyncDecision decision = record.decide(changedLocal, agreed, BOTH_WRITABLE);

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.WRITE);
        assertThat(decision.direction()).isEqualTo(SyncDirection.OUTWARD);
    }

    @Test
    void decide_bothSidesChangedToTheSameValue_converges() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("agreed value"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot bothNowAgreeOnThis = SyncSnapshot.of(new TestProjection("independently corrected value"), HASHER);

        SyncDecision decision = record.decide(bothNowAgreeOnThis, bothNowAgreeOnThis, BOTH_WRITABLE);

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.CONVERGED);
        assertThat(decision.direction()).isNull();
    }

    @Test
    void decide_onlyLocalChangedButNoOutwardWriteCapability_conflict() {
        // A local change the integration cannot write outward is a conflict, not a
        // silent overwrite (design.md D6).
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("agreed value"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedLocal = SyncSnapshot.of(new TestProjection("new local value"), HASHER);

        SyncDecision decision = record.decide(changedLocal, agreed, INWARD_ONLY);

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.CONFLICT);
        assertThat(decision.direction()).isEqualTo(SyncDirection.OUTWARD);
    }

    @Test
    void decide_bothSidesChangedToDifferentValues_conflict() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("agreed value"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedLocal = SyncSnapshot.of(new TestProjection("local edit"), HASHER);
        SyncSnapshot changedExternal = SyncSnapshot.of(new TestProjection("external edit"), HASHER);

        SyncDecision decision = record.decide(changedLocal, changedExternal, BOTH_WRITABLE);

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.CONFLICT);
    }

    @Test
    void decide_externalChangedWhileBaselineIsDivergedByAcceptedDivergence_conflict() {
        // A standing accepted divergence (design.md D6) is recorded as a baseline pair
        // whose halves differ. Any further external movement must stop and ask again,
        // rather than silently overwrite the accepted local value (the D4 inward guard).
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot acceptedLocal = SyncSnapshot.of(new TestProjection("accepted local value"), HASHER);
        SyncSnapshot acceptedExternal = SyncSnapshot.of(new TestProjection("accepted external value"), HASHER);
        record.acceptDivergence(acceptedLocal, acceptedExternal);

        SyncSnapshot newExternalChange = SyncSnapshot.of(new TestProjection("yet another external value"), HASHER);

        // Local is unchanged relative to its own baseline half; only external moved —
        // but because the baseline pair is diverged, this must be a conflict, not an
        // ordinary inward write.
        SyncDecision decision = record.decide(acceptedLocal, newExternalChange, BOTH_WRITABLE);

        assertThat(decision.kind()).isEqualTo(SyncDecision.Kind.CONFLICT);
    }
}
