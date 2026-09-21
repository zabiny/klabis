package com.klabis.events.infrastructure.orissync;

import com.klabis.events.DisciplineArchivedEvent;
import com.klabis.events.DisciplineId;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisciplineSyncListener")
class DisciplineSyncListenerTest {

    @Mock
    private SynchronizationPort synchronizationPort;

    private DisciplineSyncListener listener;

    private DisciplineId disciplineId;
    private SyncTarget target;

    @BeforeEach
    void setUp() {
        listener = new DisciplineSyncListener(synchronizationPort);
        disciplineId = new DisciplineId(UUID.randomUUID());
        target = new SyncTarget(SyncEntityType.DISCIPLINE, disciplineId.value().toString());
    }

    @Test
    @DisplayName("retires the paired record on DisciplineArchivedEvent (task 8.1)")
    void retiresOnDisciplineArchived() {
        DisciplineArchivedEvent event = new DisciplineArchivedEvent(UUID.randomUUID(), disciplineId, Instant.now());
        SyncRecordId recordId = SyncRecordId.newId();
        SyncRecord record = SyncRecord.enroll(recordId, target, new ExternalReference(ExternalSystem.ORIS, "100"));
        when(synchronizationPort.findByTarget(target)).thenReturn(Optional.of(record));

        listener.handle(event);

        verify(synchronizationPort).retire(recordId);
    }

    @Test
    @DisplayName("does nothing when the archived discipline was never paired to ORIS (task 8.1)")
    void doesNothingWhenNotEnrolled() {
        DisciplineArchivedEvent event = new DisciplineArchivedEvent(UUID.randomUUID(), disciplineId, Instant.now());
        when(synchronizationPort.findByTarget(target)).thenReturn(Optional.empty());

        listener.handle(event);

        verify(synchronizationPort).findByTarget(target);
        verifyNoMoreInteractions(synchronizationPort);
    }

    @Test
    @DisplayName("reactivating a paired discipline re-enrols it, starting over from ORIS's current values (task 8.2)")
    void reactivatesPairedDiscipline() {
        ExternalReference externalReference = new ExternalReference(ExternalSystem.ORIS, "100");
        SyncRecord retired = SyncRecord.enroll(SyncRecordId.newId(), target, externalReference);
        when(synchronizationPort.findByTarget(target)).thenReturn(Optional.of(retired));

        listener.reactivate(disciplineId, "test-user");

        verify(synchronizationPort).pullAndEnroll(SyncEntityType.DISCIPLINE, externalReference, "test-user");
    }

    @Test
    @DisplayName("reactivating a discipline that was never paired is a pure domain-only flag flip — pullAndEnroll is never called (task 8.2)")
    void doesNotReactivateUnpairedDiscipline() {
        when(synchronizationPort.findByTarget(target)).thenReturn(Optional.empty());

        listener.reactivate(disciplineId, "test-user");

        verify(synchronizationPort).findByTarget(target);
        verifyNoMoreInteractions(synchronizationPort);
    }
}
