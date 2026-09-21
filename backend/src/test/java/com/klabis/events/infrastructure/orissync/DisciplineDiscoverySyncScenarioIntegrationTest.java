package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineRepository;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end scenarios for specs/disciplines' "Local Discipline Catalog Kept In Step
 * With ORIS" requirement (tasks.md 5.3), mirroring
 * {@link OrisEventSyncScenarioIntegrationTest}'s structure: real
 * {@link SynchronizationPort}/database, only {@code OrisApiClient} mocked.
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Disciplines on the sync engine — end-to-end scenarios (specs/disciplines)")
class DisciplineDiscoverySyncScenarioIntegrationTest {

    @Autowired
    private DisciplineDiscoveryJob disciplineDiscoveryJob;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private DisciplineRepository disciplineRepository;

    @MockitoBean
    private OrisApiClient orisApiClient;

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(900_000);

    @Test
    @DisplayName("a new ORIS discipline appears in the catalog automatically")
    void newOrisDisciplineIsDiscoveredIntoLocalCatalog() {
        String orisId = String.valueOf(ORIS_ID_SEQUENCE.incrementAndGet());
        stubOrisDisciplines(orisEntry(orisId, "OB", "Orientační běh"));

        disciplineDiscoveryJob.discoverNewDisciplines();

        Discipline discovered = disciplineByOrisId(orisId);
        assertThat(discovered.getCode()).isEqualTo("OB");
        assertThat(discovered.getName()).isEqualTo("Orientační běh");

        // "available for a manager to assign to an event type" (spec.md): the picklist
        // (D5) reads this same unpaged, non-archived catalog.
        assertThat(disciplineRepository.findAllSorted())
                .extracting(Discipline::getId)
                .contains(discovered.getId());

        SyncRecord record = synchronizationPort.findByTarget(
                        new SyncTarget(SyncEntityType.DISCIPLINE, discovered.getId().value().toString()))
                .orElseThrow();
        assertThat(record.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
    }

    @Test
    @DisplayName("a discipline's name changes in ORIS, and a later sync pass — not the discovery job — updates the local catalog")
    void disciplineNameChangeInOrisIsPickedUpByLaterSyncPass() {
        String orisId = String.valueOf(ORIS_ID_SEQUENCE.incrementAndGet());
        stubOrisDisciplines(orisEntry(orisId, "SP", "Sprint"));
        disciplineDiscoveryJob.discoverNewDisciplines();

        Discipline discovered = disciplineByOrisId(orisId);
        assertThat(discovered.getName()).isEqualTo("Sprint");
        SyncRecord record = synchronizationPort.findByTarget(
                        new SyncTarget(SyncEntityType.DISCIPLINE, discovered.getId().value().toString()))
                .orElseThrow();

        // ORIS renames the discipline. The discipline is already paired at this point,
        // so the discovery job (which only ever handles undiscovered ids, design.md D4)
        // would skip it entirely — it is the engine's existing scan that must pick this
        // up, exercised here directly via synchronizeNow rather than waiting for
        // SyncScheduler's cron.
        stubOrisDisciplines(orisEntry(orisId, "SP", "Sprint (Updated)"));

        synchronizationPort.synchronizeNow(record.getId(), "test-user");

        Discipline updated = disciplineRepository.findById(discovered.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Sprint (Updated)");
        assertThat(updated.getCode()).isEqualTo("SP");
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
