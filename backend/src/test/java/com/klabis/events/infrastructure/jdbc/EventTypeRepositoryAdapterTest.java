package com.klabis.events.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.events.DisciplineId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineRepository;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventType JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM events.event_types")
class EventTypeRepositoryAdapterTest {

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Autowired
    private DisciplineRepository disciplineRepository;

    private DisciplineId newDiscipline(String code) {
        Discipline discipline = Discipline.create(new Discipline.CreateDiscipline(code, code));
        return disciplineRepository.save(discipline).getId();
    }

    @Nested
    @DisplayName("save() and findById() round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should persist and load event type with all fields")
        void shouldPersistAndLoad() {
            EventType eventType = EventType.create(
                    new EventType.CreateEventType("Trénink", "#ff0000", 1, null), 1);

            EventType saved = eventTypeRepository.save(eventType);
            Optional<EventType> loaded = eventTypeRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getName()).isEqualTo("Trénink");
            assertThat(loaded.get().getColor()).contains("#ff0000");
            assertThat(loaded.get().getSortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("should persist event type with null color")
        void shouldPersistWithNullColor() {
            EventType eventType = EventType.create(
                    new EventType.CreateEventType("Závod", null, 0, null), 0);

            EventType saved = eventTypeRepository.save(eventType);
            Optional<EventType> loaded = eventTypeRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getColor()).isEmpty();
        }

        @Test
        @DisplayName("should populate audit metadata after save")
        void shouldPopulateAuditMetadata() {
            EventType eventType = EventType.create(
                    new EventType.CreateEventType("Audit Test", null, 0, null), 0);

            EventType saved = eventTypeRepository.save(eventType);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            assertThat(eventTypeRepository.findById(EventTypeId.generate())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllSorted()")
    class FindAllSorted {

        @Test
        @DisplayName("should return types ordered by sort_order")
        void shouldReturnOrderedBySortOrder() {
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("B", null, 2, null), 2));
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("A", null, 1, null), 1));
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("C", null, 3, null), 3));

            List<EventType> sorted = eventTypeRepository.findAllSorted();

            assertThat(sorted).extracting(EventType::getName)
                    .containsExactly("A", "B", "C");
        }
    }

    @Nested
    @DisplayName("findByNameIgnoreCase()")
    class FindByName {

        @Test
        @DisplayName("should find event type case-insensitively")
        void shouldFindIgnoringCase() {
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("Trénink", null, 1, null), 1));

            assertThat(eventTypeRepository.findByNameIgnoreCase("trénink")).isPresent();
            assertThat(eventTypeRepository.findByNameIgnoreCase("TRÉNINK")).isPresent();
        }

        @Test
        @DisplayName("should return empty when name not found")
        void shouldReturnEmptyWhenNotFound() {
            assertThat(eventTypeRepository.findByNameIgnoreCase("Neexistuje")).isEmpty();
        }
    }

    @Nested
    @DisplayName("unique name constraint")
    class UniqueNameConstraint {

        @Test
        @DisplayName("should throw when saving duplicate name (exact case)")
        void shouldThrowOnExactDuplicateName() {
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("Trénink", null, 1, null), 1));

            EventType duplicate = EventType.create(new EventType.CreateEventType("Trénink", null, 2, null), 2);

            assertThatThrownBy(() -> eventTypeRepository.save(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("existsByNameIgnoreCase should detect case-insensitive duplicates")
        void shouldDetectCaseInsensitiveDuplicateViaQuery() {
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("Trénink", null, 1, null), 1));

            assertThat(eventTypeRepository.existsByNameIgnoreCase("trénink")).isTrue();
            assertThat(eventTypeRepository.existsByNameIgnoreCase("TRÉNINK")).isTrue();
            assertThat(eventTypeRepository.existsByNameIgnoreCase("Trénink")).isTrue();
        }
    }

    @Nested
    @DisplayName("findMaxSortOrder()")
    class FindMaxSortOrder {

        @Test
        @DisplayName("should return -1 when table is empty")
        void shouldReturnMinusOneWhenEmpty() {
            assertThat(eventTypeRepository.findMaxSortOrder()).isEqualTo(-1);
        }

        @Test
        @DisplayName("should return max sort order")
        void shouldReturnMaxSortOrder() {
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("A", null, 5, null), 5));
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("B", null, 3, null), 3));

            assertThat(eventTypeRepository.findMaxSortOrder()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteById {

        @Test
        @DisplayName("should delete event type")
        void shouldDelete() {
            EventType saved = eventTypeRepository.save(
                    EventType.create(new EventType.CreateEventType("To Delete", null, 1, null), 1));

            eventTypeRepository.deleteById(saved.getId());

            assertThat(eventTypeRepository.findById(saved.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("disciplineIds persistence")
    class DisciplineIdsPersistence {

        @Test
        @DisplayName("should persist and load disciplineIds")
        void shouldPersistAndLoadDisciplineIds() {
            DisciplineId first = newDiscipline("SP1");
            DisciplineId second = newDiscipline("SP2");
            EventType eventType = EventType.create(
                    new EventType.CreateEventType("Sprint", null, 1, Set.of(first, second)), 1);

            EventType saved = eventTypeRepository.save(eventType);
            Optional<EventType> loaded = eventTypeRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getDisciplineIds()).containsExactlyInAnyOrder(first, second);
        }

        @Test
        @DisplayName("should persist event type with empty disciplineIds")
        void shouldPersistWithEmptyDisciplineIds() {
            EventType eventType = EventType.create(
                    new EventType.CreateEventType("Long", null, 1, null), 1);

            EventType saved = eventTypeRepository.save(eventType);
            Optional<EventType> loaded = eventTypeRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getDisciplineIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDisciplineId()")
    class FindByDisciplineId {

        @Test
        @DisplayName("should find event type by discipline ID")
        void shouldFindByDisciplineId() {
            DisciplineId disciplineId = newDiscipline("SP3");
            EventType saved = eventTypeRepository.save(
                    EventType.create(new EventType.CreateEventType("Sprint", null, 1, Set.of(disciplineId)), 1));

            Optional<EventType> found = eventTypeRepository.findByDisciplineId(disciplineId);

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("should return empty when no event type has the discipline ID")
        void shouldReturnEmptyWhenNotFound() {
            assertThat(eventTypeRepository.findByDisciplineId(DisciplineId.generate())).isEmpty();
        }

        @Test
        @DisplayName("should return all disciplineIds when found by one of them")
        void shouldReturnCompleteDisciplineIds() {
            DisciplineId first = newDiscipline("M1");
            DisciplineId second = newDiscipline("M2");
            DisciplineId third = newDiscipline("M3");
            EventType saved = eventTypeRepository.save(
                    EventType.create(new EventType.CreateEventType("Middle", null, 1, Set.of(first, second, third)), 1));

            Optional<EventType> found = eventTypeRepository.findByDisciplineId(second);

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
            assertThat(found.get().getDisciplineIds()).containsExactlyInAnyOrder(first, second, third);
        }

        @Test
        @DisplayName("should not find deleted event type by discipline ID after cascade delete")
        void shouldNotFindAfterDelete() {
            DisciplineId disciplineId = newDiscipline("SP4");
            EventType saved = eventTypeRepository.save(
                    EventType.create(new EventType.CreateEventType("Sprint", null, 1, Set.of(disciplineId)), 1));

            eventTypeRepository.deleteById(saved.getId());

            assertThat(eventTypeRepository.findByDisciplineId(disciplineId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("reference to an archived discipline")
    class ArchivedDisciplineReference {

        @Test
        @DisplayName("should keep loading and saving an EventType that references an archived discipline")
        void shouldLoadAndSaveWhenReferencedDisciplineIsArchived() {
            DisciplineId disciplineId = newDiscipline("ARC1");
            EventType saved = eventTypeRepository.save(
                    EventType.create(new EventType.CreateEventType("Archived Ref", null, 1, Set.of(disciplineId)), 1));

            Discipline discipline = disciplineRepository.findById(disciplineId).orElseThrow();
            discipline.archive();
            disciplineRepository.save(discipline);

            Optional<EventType> loaded = eventTypeRepository.findById(saved.getId());
            assertThat(loaded).isPresent();
            assertThat(loaded.get().getDisciplineIds()).containsExactly(disciplineId);

            EventType resaved = eventTypeRepository.save(loaded.get());

            assertThat(eventTypeRepository.findById(resaved.getId()))
                    .get()
                    .extracting(EventType::getDisciplineIds)
                    .isEqualTo(Set.of(disciplineId));
            assertThat(disciplineRepository.findById(disciplineId)).get()
                    .extracting(Discipline::isArchived).isEqualTo(true);
        }
    }
}
