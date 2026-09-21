package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.events.DisciplineId;
import com.klabis.events.application.EventTypeManagementPort;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineRepository;
import com.klabis.events.domain.EventType;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.domain.SyncedEntityReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end scenarios for specs/disciplines' "archiving and restoring reuse the sync
 * engine's existing retire/reactivate lifecycle" requirement (design.md D8, tasks.md
 * 8.3), mirroring {@link DisciplineDiscoverySyncScenarioIntegrationTest}'s structure:
 * real {@link SynchronizationPort}/database, only {@code OrisApiClient} mocked.
 * <p>
 * Group 9 ({@code DisciplineManagementService}/{@code DisciplineController}) does not
 * exist yet, so both scenarios drive {@link Discipline#archive()}/{@link
 * Discipline#restore()} and {@link DisciplineSyncListener} directly, the same way
 * this change's Group 7 tests already exercise the domain layer ahead of its REST
 * surface.
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Disciplines archiving/restoring on the sync engine — end-to-end scenarios (specs/disciplines)")
class DisciplineArchiveRestoreSyncScenarioIntegrationTest {

    @Autowired
    private DisciplineDiscoveryJob disciplineDiscoveryJob;

    @Autowired
    private DisciplineSyncListener disciplineSyncListener;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Autowired
    private EventTypeManagementPort eventTypeManagementPort;

    @MockitoBean
    private OrisApiClient orisApiClient;

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(920_000);

    @Test
    @DisplayName("Manager removes a discipline that is still assigned to an event type")
    void archivingDisciplineStillAssignedToEventTypeRetiresPairingWithoutBreakingReference() {
        String orisId = String.valueOf(ORIS_ID_SEQUENCE.incrementAndGet());
        stubOrisDisciplines(orisEntry(orisId, "OB", "Orientační běh"));
        disciplineDiscoveryJob.discoverNewDisciplines();
        Discipline discipline = disciplineByOrisId(orisId);

        EventType eventType = eventTypeManagementPort.createEventType(
                new EventType.CreateEventType("Assigned Type " + orisId, "#112233", null, Set.of(discipline.getId())));

        discipline.archive();
        disciplineRepository.save(discipline);

        // "any EventType already referencing it keeps working, unchanged" (design.md
        // D8/Glossary) — archiving never touches event_type_oris_disciplines.
        EventType reloaded = eventTypeManagementPort.getEventType(eventType.getId());
        assertThat(reloaded.getDisciplineIds()).contains(discipline.getId());

        SyncRecord record = synchronizationPort.findByTarget(disciplineTarget(discipline.getId())).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(SyncStatus.RETIRED);

        Discipline archived = disciplineRepository.findById(discipline.getId()).orElseThrow();
        assertThat(archived.isArchived()).isTrue();
    }

    @Test
    @DisplayName("Manager restores a removed discipline")
    void restoringArchivedDisciplineReactivatesSyncPairing() {
        String orisId = String.valueOf(ORIS_ID_SEQUENCE.incrementAndGet());
        stubOrisDisciplines(orisEntry(orisId, "SP", "Sprint"));
        disciplineDiscoveryJob.discoverNewDisciplines();
        Discipline discipline = disciplineByOrisId(orisId);

        discipline.archive();
        disciplineRepository.save(discipline);
        SyncRecord retired = synchronizationPort.findByTarget(disciplineTarget(discipline.getId())).orElseThrow();
        assertThat(retired.getStatus()).isEqualTo(SyncStatus.RETIRED);

        Discipline toRestore = disciplineRepository.findById(discipline.getId()).orElseThrow();
        toRestore.restore();
        disciplineRepository.save(toRestore);
        disciplineSyncListener.reactivate(discipline.getId(), "test-user");

        SyncRecord reactivated = synchronizationPort.findByTarget(disciplineTarget(discipline.getId())).orElseThrow();
        assertThat(reactivated.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(disciplineRepository.findById(discipline.getId()).orElseThrow().isArchived()).isFalse();

        // The reactivated pairing keeps working — a later ORIS change is still picked
        // up by an ordinary pass (mirrors DisciplineDiscoverySyncScenarioIntegrationTest's
        // "name change" scenario).
        stubOrisDisciplines(orisEntry(orisId, "SP", "Sprint (Updated)"));
        SyncRecord afterSync = synchronizationPort.synchronizeNow(reactivated.getId(), "test-user");

        assertThat(afterSync.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        Discipline updated = disciplineRepository.findById(discipline.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Sprint (Updated)");
    }

    private static SyncTarget disciplineTarget(DisciplineId disciplineId) {
        return new SyncTarget(SyncEntityType.DISCIPLINE, disciplineId.value().toString());
    }

    private Discipline disciplineByOrisId(String orisId) {
        SyncedEntityReference reference = synchronizationPort
                .findByExternalReferences(SyncEntityType.DISCIPLINE, ExternalSystem.ORIS, List.of(orisId))
                .stream()
                .findFirst()
                .orElseThrow();
        DisciplineId disciplineId = new DisciplineId(UUID.fromString(reference.target().entityId()));
        return disciplineRepository.findById(disciplineId).orElseThrow();
    }

    private static DisciplineListEntry orisEntry(String id, String name, String descriptionCZ) {
        DisciplineListEntry entry = Mockito.mock(DisciplineListEntry.class);
        Mockito.lenient().when(entry.id()).thenReturn(id);
        Mockito.lenient().when(entry.name()).thenReturn(name);
        Mockito.lenient().when(entry.descriptionCZ()).thenReturn(descriptionCZ);
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
