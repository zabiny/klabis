package com.klabis.events.application;

import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineNotArchivedException;
import com.klabis.events.domain.DisciplineNotEditableException;
import com.klabis.events.domain.DisciplineNotFoundException;
import com.klabis.events.domain.DisciplineRepository;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisciplineManagementService tests")
class DisciplineManagementServiceTest {

    @Mock
    private DisciplineRepository disciplineRepository;

    @Mock
    private SynchronizationPort synchronizationPort;

    private DisciplineManagementService service;

    @BeforeEach
    void setUp() {
        service = new DisciplineManagementService(disciplineRepository, synchronizationPort);
    }

    private static SyncTarget targetFor(DisciplineId id) {
        return new SyncTarget(SyncEntityType.DISCIPLINE, id.value().toString());
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create and save a new discipline")
        void shouldCreateDiscipline() {
            var command = new Discipline.CreateDiscipline("OB", "Orientační běh");
            when(disciplineRepository.save(any(Discipline.class))).thenAnswer(inv -> inv.getArgument(0));

            Discipline created = service.create(command);

            assertThat(created.getCode()).isEqualTo("OB");
            assertThat(created.getName()).isEqualTo("Orientační běh");
            verify(disciplineRepository).save(any(Discipline.class));
        }
    }

    @Nested
    @DisplayName("get()")
    class GetTests {

        @Test
        @DisplayName("should return the discipline when found")
        void shouldReturnDiscipline() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineRepository.findById(discipline.getId())).thenReturn(Optional.of(discipline));

            Discipline result = service.get(discipline.getId());

            assertThat(result).isEqualTo(discipline);
        }

        @Test
        @DisplayName("should throw DisciplineNotFoundException when missing")
        void shouldThrowWhenNotFound() {
            DisciplineId id = DisciplineId.generate();
            when(disciplineRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(id))
                    .isInstanceOf(DisciplineNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("update succeeds for a manually created discipline")
        void shouldUpdateManuallyCreatedDiscipline() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineRepository.findById(discipline.getId())).thenReturn(Optional.of(discipline));
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.empty());
            when(disciplineRepository.save(any(Discipline.class))).thenAnswer(inv -> inv.getArgument(0));

            Discipline updated = service.update(discipline.getId(), "OB2", "Orientační běh 2");

            assertThat(updated.getCode()).isEqualTo("OB2");
            assertThat(updated.getName()).isEqualTo("Orientační běh 2");
            verify(disciplineRepository).save(discipline);
        }

        @Test
        @DisplayName("update is refused for an ORIS-paired discipline (active pairing)")
        void shouldRefuseUpdateForActivelyPairedDiscipline() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), targetFor(discipline.getId()),
                    new ExternalReference(ExternalSystem.ORIS, "100"));
            when(disciplineRepository.findById(discipline.getId())).thenReturn(Optional.of(discipline));
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.of(record));

            assertThatThrownBy(() -> service.update(discipline.getId(), "X", "Y"))
                    .isInstanceOf(DisciplineNotEditableException.class);

            verify(disciplineRepository, never()).save(any());
        }

        @Test
        @DisplayName("update is refused for an ORIS-paired discipline even when the pairing is retired")
        void shouldRefuseUpdateForRetiredlyPairedDiscipline() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), targetFor(discipline.getId()),
                    new ExternalReference(ExternalSystem.ORIS, "100"));
            record.retire(Instant.now());
            when(disciplineRepository.findById(discipline.getId())).thenReturn(Optional.of(discipline));
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.of(record));

            assertThatThrownBy(() -> service.update(discipline.getId(), "X", "Y"))
                    .isInstanceOf(DisciplineNotEditableException.class);

            verify(disciplineRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DisciplineNotFoundException when updating a missing discipline")
        void shouldThrowWhenUpdatingMissingDiscipline() {
            DisciplineId id = DisciplineId.generate();
            when(disciplineRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(id, "X", "Y"))
                    .isInstanceOf(DisciplineNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("archive()")
    class ArchiveTests {

        @Test
        @DisplayName("archive always succeeds even if referenced by an EventType — no reference check exists")
        void shouldAlwaysSucceedRegardlessOfReferences() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineRepository.findById(discipline.getId())).thenReturn(Optional.of(discipline));
            when(disciplineRepository.save(any(Discipline.class))).thenAnswer(inv -> inv.getArgument(0));

            service.archive(discipline.getId());

            assertThat(discipline.isArchived()).isTrue();
            verify(disciplineRepository).save(discipline);
            // No EventTypeRepository / reference lookup exists on this service at all —
            // nothing to verify beyond the plain save succeeding unconditionally.
        }

        @Test
        @DisplayName("should throw DisciplineNotFoundException when archiving a missing discipline")
        void shouldThrowWhenArchivingMissingDiscipline() {
            DisciplineId id = DisciplineId.generate();
            when(disciplineRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.archive(id))
                    .isInstanceOf(DisciplineNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("restore()")
    class RestoreTests {

        @Test
        @DisplayName("restore fails with a 409-equivalent exception when not archived")
        void shouldFailWhenNotArchived() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineRepository.findById(discipline.getId())).thenReturn(Optional.of(discipline));

            assertThatThrownBy(() -> service.restore(discipline.getId(), "test-user"))
                    .isInstanceOf(DisciplineNotArchivedException.class);

            verify(disciplineRepository, never()).save(any());
            verifyNoMoreInteractions(synchronizationPort);
        }

        @Test
        @DisplayName("should throw DisciplineNotFoundException when restoring a missing discipline")
        void shouldThrowWhenRestoringMissingDiscipline() {
            DisciplineId id = DisciplineId.generate();
            when(disciplineRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.restore(id, "test-user"))
                    .isInstanceOf(DisciplineNotFoundException.class);
        }

        @Test
        @DisplayName("restores an archived, never-paired discipline as a pure domain-only flag flip")
        void shouldRestoreUnpairedDisciplineWithoutReactivating() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            discipline.archive();
            when(disciplineRepository.findById(discipline.getId())).thenReturn(Optional.of(discipline));
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.empty());
            when(disciplineRepository.save(any(Discipline.class))).thenAnswer(inv -> inv.getArgument(0));

            service.restore(discipline.getId(), "test-user");

            assertThat(discipline.isArchived()).isFalse();
            verify(disciplineRepository).save(discipline);
            verify(synchronizationPort, never()).pullAndEnroll(any(), any(), any());
        }

        @Test
        @DisplayName("restoring an archived, ORIS-paired discipline reactivates its sync pairing")
        void shouldReactivateSyncPairingOnRestore() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            discipline.archive();
            ExternalReference externalReference = new ExternalReference(ExternalSystem.ORIS, "100");
            SyncRecord retired = SyncRecord.enroll(SyncRecordId.newId(), targetFor(discipline.getId()), externalReference);
            when(disciplineRepository.findById(discipline.getId())).thenReturn(Optional.of(discipline));
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.of(retired));
            when(disciplineRepository.save(any(Discipline.class))).thenAnswer(inv -> inv.getArgument(0));

            service.restore(discipline.getId(), "test-user");

            assertThat(discipline.isArchived()).isFalse();
            verify(disciplineRepository).save(discipline);
            verify(synchronizationPort).pullAndEnroll(SyncEntityType.DISCIPLINE, externalReference, "test-user");
        }
    }

    @Nested
    @DisplayName("list()")
    class ListTests {

        @Test
        @DisplayName("list returns the requested page")
        void shouldReturnRequestedPage() {
            Discipline first = Discipline.create(new Discipline.CreateDiscipline("MTBO", "Orientační běh na kole"));
            Discipline second = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            Pageable pageable = PageRequest.of(0, 2);
            var page = new PageImpl<>(List.of(first, second), pageable, 5);
            when(disciplineRepository.findAll(pageable)).thenReturn(page);

            var result = service.list(pageable);

            assertThat(result.getContent()).containsExactly(first, second);
            assertThat(result.getTotalElements()).isEqualTo(5);
            verify(disciplineRepository, times(1)).findAll(pageable);
        }
    }
}
