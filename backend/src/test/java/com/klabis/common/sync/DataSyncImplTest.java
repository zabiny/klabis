package com.klabis.common.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DataSyncImpl")
class DataSyncImplTest {

    private static final SyncId LOCAL_ID = SyncId.localId(SyncType.EVENT, "local-1");
    private static final SyncId EXTERNAL_ID = SyncId.externalId(SyncType.EVENT, "external-1");

    private SyncLine<?, ?> syncLine;
    private FakeSyncRecordRepository syncRecords;
    private DataSyncImpl testedInstance;

    @BeforeEach
    void setUp() {
        syncLine = mock(SyncLine.class);
        lenient().when(syncLine.matches(any())).thenReturn(true);
        lenient().when(syncLine.push(LOCAL_ID)).thenReturn(EXTERNAL_ID);
        lenient().when(syncLine.pull(EXTERNAL_ID)).thenReturn(LOCAL_ID);
        syncRecords = new FakeSyncRecordRepository();
        testedInstance = new DataSyncImpl(List.of(syncLine), syncRecords);
    }

    @Test
    @DisplayName("PUSH on a local id pushes and persists a SYNCED record")
    void push_localId_returnsAndPersistsSyncedRecord() {
        SyncRecord result = testedInstance.sync(LOCAL_ID, DataSync.Direction.PUSH);

        assertThat(result.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
        assertThat(result.failureCause()).isNull();
        assertThat(syncRecords.findById(LOCAL_ID)).contains(result);
        assertThat(syncRecords.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("PULL on an external id pulls and persists a SYNCED record")
    void pull_externalId_returnsAndPersistsSyncedRecord() {
        SyncRecord result = testedInstance.sync(EXTERNAL_ID, DataSync.Direction.PULL);

        assertThat(result.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
        assertThat(syncRecords.findById(EXTERNAL_ID)).contains(result);
        assertThat(syncRecords.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("PULL on a local id resolves the external id from the existing SyncRecord and pulls with it")
    void pull_localId_resolvesExternalIdFromSyncRecord() {
        SyncRecord existing = syncRecords.save(SyncRecord.success(LOCAL_ID, EXTERNAL_ID));

        SyncRecord result = testedInstance.sync(LOCAL_ID, DataSync.Direction.PULL);

        verify(syncLine).pull(EXTERNAL_ID);
        assertThat(result.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
        assertThat(result.id()).isEqualTo(existing.id());
        assertThat(syncRecords.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("PUSH on an external id resolves the local id from the existing SyncRecord and pushes with it")
    void push_externalId_resolvesLocalIdFromSyncRecord() {
        SyncRecord existing = syncRecords.save(SyncRecord.success(LOCAL_ID, EXTERNAL_ID));

        SyncRecord result = testedInstance.sync(EXTERNAL_ID, DataSync.Direction.PUSH);

        verify(syncLine).push(LOCAL_ID);
        assertThat(result.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
        assertThat(result.id()).isEqualTo(existing.id());
        assertThat(syncRecords.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("resyncing the same pair upserts the existing record instead of adding a duplicate")
    void resync_samePair_upsertsExistingRecord() {
        SyncRecord first = testedInstance.sync(LOCAL_ID, DataSync.Direction.PUSH);

        SyncRecord second = testedInstance.sync(LOCAL_ID, DataSync.Direction.PUSH);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(syncRecords.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("PULL on a local id with no matching SyncRecord returns an ERROR record and does not call the line")
    void pull_localId_noSyncRecord_returnsErrorRecord() {
        SyncRecord result = testedInstance.sync(LOCAL_ID, DataSync.Direction.PULL);

        verify(syncLine, never()).pull(any());
        assertThat(result.result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(result.failureCause()).isEqualTo("No sync record found for " + LOCAL_ID);
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isNull();
    }

    @Test
    @DisplayName("PUSH on an external id with no matching SyncRecord returns an ERROR record and does not call the line")
    void push_externalId_noSyncRecord_returnsErrorRecord() {
        SyncRecord result = testedInstance.sync(EXTERNAL_ID, DataSync.Direction.PUSH);

        verify(syncLine, never()).push(any());
        assertThat(result.result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(result.failureCause()).isEqualTo("No sync record found for " + EXTERNAL_ID);
        assertThat(result.localId()).isNull();
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
    }

    @Test
    @DisplayName("PUSH on an external id whose SyncRecord has no local id reports the missing side distinctly")
    void push_externalId_syncRecordWithoutLocalId_returnsErrorRecord() {
        syncRecords.save(SyncRecord.failure(null, EXTERNAL_ID, "previous pull failed"));

        SyncRecord result = testedInstance.sync(EXTERNAL_ID, DataSync.Direction.PUSH);

        verify(syncLine, never()).push(any());
        assertThat(result.result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(result.failureCause()).isEqualTo("Sync record for " + EXTERNAL_ID + " has no local ID");
    }

    @Test
    @DisplayName("PULL on a local id whose SyncRecord has no external id reports the missing side distinctly")
    void pull_localId_syncRecordWithoutExternalId_returnsErrorRecord() {
        syncRecords.save(SyncRecord.failure(LOCAL_ID, null, "previous push failed"));

        SyncRecord result = testedInstance.sync(LOCAL_ID, DataSync.Direction.PULL);

        verify(syncLine, never()).pull(any());
        assertThat(result.result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(result.failureCause()).isEqualTo("Sync record for " + LOCAL_ID + " has no external ID");
    }

    @Test
    @DisplayName("when the line throws after the counterpart id was resolved, the failure record keeps both ids")
    void push_externalId_lineThrowsAfterResolution_failureKeepsBothIds() {
        syncRecords.save(SyncRecord.success(LOCAL_ID, EXTERNAL_ID));
        when(syncLine.push(LOCAL_ID)).thenThrow(new IllegalStateException("boom"));

        SyncRecord result = testedInstance.sync(EXTERNAL_ID, DataSync.Direction.PUSH);

        assertThat(result.result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(result.failureCause()).isEqualTo("boom");
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
        assertThat(syncRecords.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("when the line throws on a pull after resolution, the failure record keeps both ids")
    void pull_localId_lineThrowsAfterResolution_failureKeepsBothIds() {
        syncRecords.save(SyncRecord.success(LOCAL_ID, EXTERNAL_ID));
        when(syncLine.pull(EXTERNAL_ID)).thenThrow(new IllegalStateException("kaboom"));

        SyncRecord result = testedInstance.sync(LOCAL_ID, DataSync.Direction.PULL);

        assertThat(result.result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(result.failureCause()).isEqualTo("kaboom");
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
        assertThat(syncRecords.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("returns an ERROR record with failureCause populated when the line throws")
    void push_lineThrows_returnsErrorRecordWithCause() {
        when(syncLine.push(LOCAL_ID)).thenThrow(new IllegalStateException("boom"));

        SyncRecord result = testedInstance.sync(LOCAL_ID, DataSync.Direction.PUSH);

        assertThat(result.result()).isEqualTo(DataSync.SyncResult.ERROR);
        assertThat(result.failureCause()).isEqualTo("boom");
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isNull();
        assertThat(syncRecords.findById(LOCAL_ID)).contains(result);
    }

    @Test
    @DisplayName("sync(SyncId) infers PUSH for a local id")
    void syncConvenience_localId_infersPush() {
        SyncRecord result = testedInstance.sync(LOCAL_ID);

        assertThat(result.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
    }

    @Test
    @DisplayName("sync(SyncId) infers PULL for an external id")
    void syncConvenience_externalId_infersPull() {
        SyncRecord result = testedInstance.sync(EXTERNAL_ID);

        assertThat(result.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
    }

    @Test
    @DisplayName("no matching sync line throws IllegalArgumentException")
    void noMatchingSyncLine_throws() {
        lenient().when(syncLine.matches(any())).thenReturn(false);

        assertThatThrownBy(() -> testedInstance.sync(LOCAL_ID, DataSync.Direction.PUSH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
