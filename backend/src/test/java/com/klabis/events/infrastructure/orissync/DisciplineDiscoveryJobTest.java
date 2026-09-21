package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisciplineDiscoveryJob")
class DisciplineDiscoveryJobTest {

    @Mock
    private OrisApiClient orisApiClient;

    @Mock
    private SynchronizationPort synchronizationPort;

    private DisciplineDiscoveryJob job;

    @BeforeEach
    void setUp() {
        job = new DisciplineDiscoveryJob(orisApiClient, synchronizationPort);
    }

    @Test
    @DisplayName("enrols only ORIS disciplines not already paired, leaving already-discovered ones untouched")
    void enrolsOnlyUndiscoveredDisciplines() {
        stubOrisDisciplines(orisEntry("1", "OB"), orisEntry("2", "SP"), orisEntry("3", "MTBO"));
        when(synchronizationPort.findByExternalReferences(
                eq(SyncEntityType.DISCIPLINE), eq(ExternalSystem.ORIS), any()))
                .thenReturn(List.of(new SyncedEntityReference(
                        new SyncTarget(SyncEntityType.DISCIPLINE, "existing-discipline-id"),
                        new ExternalReference(ExternalSystem.ORIS, "2"))));

        job.discoverNewDisciplines();

        verify(synchronizationPort).pullAndEnroll(
                SyncEntityType.DISCIPLINE, new ExternalReference(ExternalSystem.ORIS, "1"), null);
        verify(synchronizationPort).pullAndEnroll(
                SyncEntityType.DISCIPLINE, new ExternalReference(ExternalSystem.ORIS, "3"), null);
        verify(synchronizationPort, never()).pullAndEnroll(
                eq(SyncEntityType.DISCIPLINE), eq(new ExternalReference(ExternalSystem.ORIS, "2")), any());
        verify(synchronizationPort, times(2)).pullAndEnroll(any(), any(), any());
    }

    @Test
    @DisplayName("enrols nothing when every ORIS discipline is already paired")
    void enrolsNothingWhenAllDisciplinesAlreadyPaired() {
        stubOrisDisciplines(orisEntry("1", "OB"));
        when(synchronizationPort.findByExternalReferences(
                eq(SyncEntityType.DISCIPLINE), eq(ExternalSystem.ORIS), any()))
                .thenReturn(List.of(new SyncedEntityReference(
                        new SyncTarget(SyncEntityType.DISCIPLINE, "existing-discipline-id"),
                        new ExternalReference(ExternalSystem.ORIS, "1"))));

        job.discoverNewDisciplines();

        verify(synchronizationPort, never()).pullAndEnroll(any(), any(), any());
    }

    @Test
    @DisplayName("does nothing when ORIS reports no disciplines")
    void doesNothingWhenOrisReportsNoDisciplines() {
        OrisApiClient.OrisResponse<Map<String, DisciplineListEntry>> response =
                new OrisApiClient.OrisResponse<>(Map.of(), "JSON", "OK", null, "listDisciplines");
        Mockito.doReturn(response).when(orisApiClient).listDisciplines();

        job.discoverNewDisciplines();

        verify(synchronizationPort, never()).findByExternalReferences(any(), any(), any());
        verify(synchronizationPort, never()).pullAndEnroll(any(), any(), any());
    }

    private static DisciplineListEntry orisEntry(String id, String name) {
        DisciplineListEntry entry = Mockito.mock(DisciplineListEntry.class);
        Mockito.lenient().when(entry.id()).thenReturn(id);
        Mockito.lenient().when(entry.name()).thenReturn(name);
        return entry;
    }

    private void stubOrisDisciplines(DisciplineListEntry... entries) {
        Map<String, DisciplineListEntry> byId = new LinkedHashMap<>();
        for (DisciplineListEntry entry : entries) {
            byId.put(entry.id(), entry);
        }
        OrisApiClient.OrisResponse<Map<String, DisciplineListEntry>> response =
                new OrisApiClient.OrisResponse<>(byId, "JSON", "OK", null, "listDisciplines");
        Mockito.doReturn(response).when(orisApiClient).listDisciplines();
    }
}
