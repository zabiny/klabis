package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineNotFoundException;
import com.klabis.events.domain.DisciplineRepository;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisciplineSyncAdapter")
class DisciplineSyncAdapterTest {

    @Mock
    private DisciplineRepository disciplineRepository;

    @Mock
    private OrisApiClient orisApiClient;

    private DisciplineSyncAdapter adapter;

    private static final UUID DISCIPLINE_UUID = UUID.randomUUID();
    private static final DisciplineId DISCIPLINE_ID = new DisciplineId(DISCIPLINE_UUID);
    private static final String ORIS_ID = "42";

    @BeforeEach
    void setUp() {
        adapter = new DisciplineSyncAdapter(disciplineRepository, orisApiClient);
    }

    @Test
    @DisplayName("declares pull-only-creating capabilities: no outward write, creates the local side, no sensitive data")
    void declaresPullOnlyCreatingCapabilities() {
        var capabilities = adapter.capabilities();

        assertThat(capabilities.readsLocal()).isTrue();
        assertThat(capabilities.readsExternal()).isTrue();
        assertThat(capabilities.writesLocal()).isTrue();
        assertThat(capabilities.writesExternal()).isFalse();
        assertThat(capabilities.createsLocal()).isTrue();
        assertThat(capabilities.createsExternal()).isFalse();
        assertThat(capabilities.containsSensitiveData()).isFalse();
    }

    @Test
    @DisplayName("entityType is DISCIPLINE and system is ORIS")
    void declaresEntityTypeAndSystem() {
        assertThat(adapter.entityType()).isEqualTo(SyncEntityType.DISCIPLINE);
        assertThat(adapter.system()).isEqualTo(ExternalSystem.ORIS);
    }

    @Nested
    @DisplayName("externalVersion()")
    class ExternalVersionMethod {

        @Test
        @DisplayName("is always empty — oris-client offers no cheap version signal, engine falls back to a full read")
        void alwaysEmpty() {
            assertThat(adapter.externalVersion(ORIS_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("readExternal()")
    class ReadExternalMethod {

        @Test
        @DisplayName("maps the matching ORIS discipline entry into the canonical projection")
        void mapsMatchingEntryIntoProjection() {
            DisciplineListEntry entry = orisEntry(ORIS_ID, "OB", "Orientační běh");
            stubOrisDisciplines(entry);

            SyncProjection projection = adapter.readExternal(ORIS_ID);

            assertThat(projection).isInstanceOf(DisciplineProjection.class);
            assertThat(((DisciplineProjection) projection).code()).isEqualTo("OB");
            assertThat(((DisciplineProjection) projection).name()).isEqualTo("Orientační běh");
        }

        @Test
        @DisplayName("throws DisciplineNotFoundException when no ORIS entry matches the external id")
        void throwsWhenNoEntryMatches() {
            stubOrisDisciplines(orisEntry("99", "SP", "Sprint"));

            assertThatThrownBy(() -> adapter.readExternal(ORIS_ID))
                    .isInstanceOf(DisciplineNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("readLocal()")
    class ReadLocalMethod {

        @Test
        @DisplayName("maps the local Discipline read through DisciplineRepository into the canonical projection")
        void mapsDisciplineIntoProjection() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineRepository.findById(DISCIPLINE_ID)).thenReturn(Optional.of(discipline));

            SyncProjection projection = adapter.readLocal(DISCIPLINE_UUID.toString());

            assertThat(projection).isInstanceOf(DisciplineProjection.class);
            assertThat(((DisciplineProjection) projection).code()).isEqualTo("OB");
            assertThat(((DisciplineProjection) projection).name()).isEqualTo("Orientační běh");
        }

        @Test
        @DisplayName("throws DisciplineNotFoundException when the discipline does not exist")
        void throwsWhenDisciplineNotFound() {
            when(disciplineRepository.findById(DISCIPLINE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.readLocal(DISCIPLINE_UUID.toString()))
                    .isInstanceOf(DisciplineNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("applyToLocal()")
    class ApplyToLocalMethod {

        @Test
        @DisplayName("writes the projection's code/name inward via Discipline.update and saves through DisciplineRepository")
        void writesInwardViaUpdate() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Old name"));
            when(disciplineRepository.findById(DISCIPLINE_ID)).thenReturn(Optional.of(discipline));
            when(disciplineRepository.save(any(Discipline.class))).thenAnswer(inv -> inv.getArgument(0));

            DisciplineProjection incoming = new DisciplineProjection("OB", "New name");

            adapter.applyToLocal(DISCIPLINE_UUID.toString(), incoming);

            assertThat(discipline.getName()).isEqualTo("New name");
            Mockito.verify(disciplineRepository).save(discipline);
        }

        @Test
        @DisplayName("throws DisciplineNotFoundException when the discipline does not exist")
        void throwsWhenDisciplineNotFound() {
            when(disciplineRepository.findById(DISCIPLINE_ID)).thenReturn(Optional.empty());
            DisciplineProjection incoming = new DisciplineProjection("OB", "New name");

            assertThatThrownBy(() -> adapter.applyToLocal(DISCIPLINE_UUID.toString(), incoming))
                    .isInstanceOf(DisciplineNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createLocal()")
    class CreateLocalMethod {

        @Test
        @DisplayName("builds a Discipline from the projection and returns its identifier")
        void buildsDisciplineFromProjectionAndReturnsId() {
            DisciplineProjection projection = new DisciplineProjection("OB", "Orientační běh");
            org.mockito.ArgumentCaptor<Discipline> savedDisciplineCaptor =
                    org.mockito.ArgumentCaptor.forClass(Discipline.class);
            when(disciplineRepository.save(savedDisciplineCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            String entityId = adapter.createLocal(projection);

            Discipline saved = savedDisciplineCaptor.getValue();
            assertThat(entityId).isEqualTo(saved.getId().value().toString());
            assertThat(saved.getCode()).isEqualTo("OB");
            assertThat(saved.getName()).isEqualTo("Orientační běh");
        }
    }

    @Nested
    @DisplayName("applyToExternal()")
    class ApplyToExternalMethod {

        @Test
        @DisplayName("throws — the adapter declares no outward write capability")
        void throwsUnsupported() {
            DisciplineProjection projection = new DisciplineProjection("OB", "Orientační běh");

            assertThatThrownBy(() -> adapter.applyToExternal(ORIS_ID, projection))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private static DisciplineListEntry orisEntry(String id, String name, String descriptionCZ) {
        DisciplineListEntry entry = Mockito.mock(DisciplineListEntry.class);
        Mockito.lenient().when(entry.id()).thenReturn(id);
        Mockito.lenient().when(entry.name()).thenReturn(name);
        Mockito.lenient().when(entry.descriptionCZ()).thenReturn(descriptionCZ);
        return entry;
    }

    private void stubOrisDisciplines(DisciplineListEntry... entries) {
        Map<String, DisciplineListEntry> byId = new java.util.LinkedHashMap<>();
        for (DisciplineListEntry entry : entries) {
            byId.put(entry.id(), entry);
        }
        OrisApiClient.OrisResponse<Map<String, DisciplineListEntry>> response =
                new OrisApiClient.OrisResponse<>(byId, "JSON", "OK", null, "listDisciplines");
        Mockito.doReturn(response).when(orisApiClient).listDisciplines();
    }
}
