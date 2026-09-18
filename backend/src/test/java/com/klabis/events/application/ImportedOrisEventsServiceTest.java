package com.klabis.events.application;

import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.domain.SyncedEntityReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportedOrisEventsService")
class ImportedOrisEventsServiceTest {

    @Mock
    private SynchronizationPort synchronizationPort;

    private ImportedOrisEventsService service;

    @BeforeEach
    void setUp() {
        service = new ImportedOrisEventsService(synchronizationPort);
    }

    @Test
    @DisplayName("should delegate to SynchronizationPort and return imported IDs from candidates")
    void shouldDelegateToSynchronizationPortAndReturnImportedIds() {
        List<Integer> candidates = List.of(101, 102, 999);
        when(synchronizationPort.findByExternalReferences(SyncEntityType.EVENT, ExternalSystem.ORIS, Set.of("101", "102", "999")))
                .thenReturn(List.of(
                        syncedReference("101"),
                        syncedReference("102")
                ));

        Set<Integer> result = service.findImportedOrisIds(candidates);

        assertThat(result).containsExactlyInAnyOrder(101, 102);
        verify(synchronizationPort).findByExternalReferences(SyncEntityType.EVENT, ExternalSystem.ORIS, Set.of("101", "102", "999"));
    }

    @Test
    @DisplayName("should still exclude a candidate whose sync record is RETIRED")
    void shouldStillExcludeCandidateWithRetiredSyncRecord() {
        List<Integer> candidates = List.of(1001, 1002);
        when(synchronizationPort.findByExternalReferences(any(), any(), any()))
                .thenReturn(List.of(syncedReference("1001")));

        Set<Integer> result = service.findImportedOrisIds(candidates);

        assertThat(result).containsExactly(1001);
    }

    @Test
    @DisplayName("should return empty set when no candidates are imported")
    void shouldReturnEmptySetWhenNoCandidatesImported() {
        List<Integer> candidates = List.of(777, 888);
        when(synchronizationPort.findByExternalReferences(any(), any(), any()))
                .thenReturn(List.of());

        Set<Integer> result = service.findImportedOrisIds(candidates);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty set for empty candidate collection without calling the port")
    void shouldReturnEmptySetForEmptyCandidates() {
        Set<Integer> result = service.findImportedOrisIds(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(synchronizationPort);
    }

    private static SyncedEntityReference syncedReference(String externalId) {
        return new SyncedEntityReference(
                new SyncTarget(SyncEntityType.EVENT, "00000000-0000-0000-0000-000000000001"),
                new ExternalReference(ExternalSystem.ORIS, externalId)
        );
    }
}
