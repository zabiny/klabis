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
    private DataSyncImpl testedInstance;

    @BeforeEach
    void setUp() {
        syncLine = mock(SyncLine.class);
        lenient().when(syncLine.matches(any())).thenReturn(true);
        lenient().when(syncLine.push(LOCAL_ID)).thenReturn(EXTERNAL_ID);
        lenient().when(syncLine.pull(EXTERNAL_ID)).thenReturn(LOCAL_ID);
        testedInstance = new DataSyncImpl(List.of(syncLine));
    }

    @Test
    @DisplayName("PUSH on a local id returns a SYNCED record when the line succeeds")
    void push_lineSucceeds_returnsSyncedRecord() {
        SyncRecord result = testedInstance.sync(LOCAL_ID, DataSync.Direction.PUSH);

        assertThat(result.result()).isEqualTo(DataSync.SyncResult.SYNCED);
        assertThat(result.localId()).isEqualTo(LOCAL_ID);
        assertThat(result.externalId()).isEqualTo(EXTERNAL_ID);
        assertThat(result.failureCause()).isNull();
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
