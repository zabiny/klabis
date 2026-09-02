package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SyncRecordFieldAttributionTest {

    private record TestProjection(String name, String location, String organizer) implements SyncProjection {
        @Override
        public SyncEntityType entityType() {
            return SyncEntityType.EVENT;
        }
    }

    private static final SyncProjectionHasher HASHER = projection -> SyncHash.of(String.valueOf(projection.hashCode()));

    /**
     * A field reader that reflects a {@link TestProjection}'s record components into a
     * name→value map — enough to test the attribution logic without pulling in
     * Jackson (that's SyncProjectionCodec's job, exercised elsewhere).
     */
    private static final SyncProjectionFieldReader FIELD_READER = projection -> {
        TestProjection p = (TestProjection) projection;
        return Map.of("name", p.name(), "location", p.location(), "organizer", p.organizer());
    };

    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-1");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "8123");

    @Test
    void changedSides_fieldOnlyKlabisMoved_attributedToLocal() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno", "SK Brno"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedLocal = SyncSnapshot.of(new TestProjection("Sprint Corrected", "Brno", "SK Brno"), HASHER);
        SyncSnapshot changedExternal = SyncSnapshot.of(new TestProjection("Sprint", "Ostrava", "SK Brno"), HASHER);
        record.recordConflict(changedLocal, changedExternal, null);

        Map<String, ChangedSide> changedSides = record.changedSides(FIELD_READER);

        assertThat(changedSides).containsEntry("name", ChangedSide.LOCAL);
    }

    @Test
    void changedSides_fieldOnlyExternalMoved_attributedToExternal() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno", "SK Brno"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedLocal = SyncSnapshot.of(new TestProjection("Sprint Corrected", "Brno", "SK Brno"), HASHER);
        SyncSnapshot changedExternal = SyncSnapshot.of(new TestProjection("Sprint", "Ostrava", "SK Brno"), HASHER);
        record.recordConflict(changedLocal, changedExternal, null);

        Map<String, ChangedSide> changedSides = record.changedSides(FIELD_READER);

        assertThat(changedSides).containsEntry("location", ChangedSide.EXTERNAL);
    }

    @Test
    void changedSides_fieldBothMoved_attributedToBoth() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno", "SK Brno"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedLocal = SyncSnapshot.of(new TestProjection("Sprint", "Brno", "TJ Sokol"), HASHER);
        SyncSnapshot changedExternal = SyncSnapshot.of(new TestProjection("Sprint", "Brno", "SK Znojmo"), HASHER);
        record.recordConflict(changedLocal, changedExternal, null);

        Map<String, ChangedSide> changedSides = record.changedSides(FIELD_READER);

        assertThat(changedSides).containsEntry("organizer", ChangedSide.BOTH);
    }

    @Test
    void changedSides_unchangedField_notInResult() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno", "SK Brno"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedLocal = SyncSnapshot.of(new TestProjection("Sprint Corrected", "Brno", "SK Brno"), HASHER);
        SyncSnapshot changedExternal = SyncSnapshot.of(new TestProjection("Sprint", "Ostrava", "SK Brno"), HASHER);
        record.recordConflict(changedLocal, changedExternal, null);

        Map<String, ChangedSide> changedSides = record.changedSides(FIELD_READER);

        assertThat(changedSides).doesNotContainKey("organizer");
    }

    @Test
    void divergedFields_namesAllChangedFields() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno", "SK Brno"), HASHER);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

        SyncSnapshot changedLocal = SyncSnapshot.of(new TestProjection("Sprint Corrected", "Brno", "TJ Sokol"), HASHER);
        SyncSnapshot changedExternal = SyncSnapshot.of(new TestProjection("Sprint", "Ostrava", "SK Znojmo"), HASHER);
        record.recordConflict(changedLocal, changedExternal, null);

        assertThat(record.divergedFields(FIELD_READER)).containsExactlyInAnyOrder("name", "location", "organizer");
    }
}
