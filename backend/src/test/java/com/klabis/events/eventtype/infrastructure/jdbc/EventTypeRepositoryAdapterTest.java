package com.klabis.events.eventtype.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.domain.EventType;
import com.klabis.events.eventtype.domain.EventTypeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventType JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM event_types")
class EventTypeRepositoryAdapterTest {

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Nested
    @DisplayName("save() and findById() round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should persist and load event type with all fields")
        void shouldPersistAndLoad() {
            EventType eventType = EventType.create(
                    new EventType.CreateEventType("Trénink", "#ff0000", 1), 1);

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
                    new EventType.CreateEventType("Závod", null, 0), 0);

            EventType saved = eventTypeRepository.save(eventType);
            Optional<EventType> loaded = eventTypeRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getColor()).isEmpty();
        }

        @Test
        @DisplayName("should populate audit metadata after save")
        void shouldPopulateAuditMetadata() {
            EventType eventType = EventType.create(
                    new EventType.CreateEventType("Audit Test", null, 0), 0);

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
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("B", null, 2), 2));
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("A", null, 1), 1));
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("C", null, 3), 3));

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
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("Trénink", null, 1), 1));

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
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("Trénink", null, 1), 1));

            EventType duplicate = EventType.create(new EventType.CreateEventType("Trénink", null, 2), 2);

            assertThatThrownBy(() -> eventTypeRepository.save(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("existsByNameIgnoreCase should detect case-insensitive duplicates")
        void shouldDetectCaseInsensitiveDuplicateViaQuery() {
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("Trénink", null, 1), 1));

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
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("A", null, 5), 5));
            eventTypeRepository.save(EventType.create(new EventType.CreateEventType("B", null, 3), 3));

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
                    EventType.create(new EventType.CreateEventType("To Delete", null, 1), 1));

            eventTypeRepository.deleteById(saved.getId());

            assertThat(eventTypeRepository.findById(saved.getId())).isEmpty();
        }
    }
}
