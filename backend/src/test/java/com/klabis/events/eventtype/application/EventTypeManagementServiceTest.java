package com.klabis.events.eventtype.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventTypeManagementService tests")
class EventTypeManagementServiceTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private OrisApiClient orisApiClient;

    private EventTypeManagementService service;

    @BeforeEach
    void setUp() {
        service = new EventTypeManagementService(eventTypeRepository, Optional.of(orisApiClient));
    }

    @Nested
    @DisplayName("createEventType() — orisDisciplineId uniqueness")
    class CreateEventTypeDisciplineValidation {

        @Test
        @DisplayName("should throw OrisDisciplineAlreadyMappedException when discipline ID is mapped to another event type")
        void shouldThrowWhenDisciplineAlreadyMappedToAnotherType() {
            var command = new EventType.CreateEventType("New Type", null, null, Set.of(3));
            EventType existing = EventType.create(new EventType.CreateEventType("Existing", null, null, Set.of(3)), 0);

            when(eventTypeRepository.existsByNameIgnoreCase("New Type")).thenReturn(false);
            when(eventTypeRepository.findByOrisDisciplineId(3)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.createEventType(command))
                    .isInstanceOf(OrisDisciplineAlreadyMappedException.class);
        }

        @Test
        @DisplayName("should create successfully when no discipline ID conflicts")
        void shouldCreateWhenNoDisciplineConflict() {
            var command = new EventType.CreateEventType("New Type", null, null, Set.of(1, 2));

            when(eventTypeRepository.existsByNameIgnoreCase("New Type")).thenReturn(false);
            when(eventTypeRepository.findMaxSortOrder()).thenReturn(0);
            when(eventTypeRepository.findByOrisDisciplineId(anyInt())).thenReturn(Optional.empty());
            when(eventTypeRepository.save(any())).thenReturn(EventType.create(command, 1));

            service.createEventType(command);
        }
    }

    @Nested
    @DisplayName("updateEventType() — orisDisciplineId uniqueness")
    class UpdateEventTypeDisciplineValidation {

        @Test
        @DisplayName("should throw OrisDisciplineAlreadyMappedException when discipline ID is mapped to a different event type")
        void shouldThrowWhenDisciplineAlreadyMappedToOtherType() {
            EventTypeId targetId = EventTypeId.generate();
            EventTypeId otherId = EventTypeId.generate();
            EventType target = EventType.create(new EventType.CreateEventType("Target", null, null, Set.of()), 0);
            EventType other = EventType.create(new EventType.CreateEventType("Other", null, null, Set.of(5)), 1);

            var command = new EventType.UpdateEventType("Target Updated", null, null, Set.of(5));

            when(eventTypeRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(eventTypeRepository.findByOrisDisciplineId(5)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.updateEventType(targetId, command))
                    .isInstanceOf(OrisDisciplineAlreadyMappedException.class);
        }

        @Test
        @DisplayName("should succeed when discipline ID already belongs to the same event type being updated")
        void shouldSucceedWhenDisciplineAlreadyBelongsToSameType() {
            EventTypeId id = EventTypeId.generate();
            EventType eventType = EventType.reconstruct(id, "Type", null, 0, null, Set.of(7));

            var command = new EventType.UpdateEventType("Type Updated", null, null, Set.of(7));

            when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
            when(eventTypeRepository.findByOrisDisciplineId(7)).thenReturn(Optional.of(eventType));
            when(eventTypeRepository.save(any())).thenReturn(eventType);

            service.updateEventType(id, command);
        }

        @Test
        @DisplayName("should succeed when no discipline ID conflicts on update")
        void shouldSucceedWhenNoDisciplineConflictOnUpdate() {
            EventTypeId id = EventTypeId.generate();
            EventType eventType = EventType.reconstruct(id, "Type", null, 0, null, Set.of());

            var command = new EventType.UpdateEventType("Type", null, null, Set.of(9));

            when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
            when(eventTypeRepository.findByOrisDisciplineId(9)).thenReturn(Optional.empty());
            when(eventTypeRepository.save(any())).thenReturn(eventType);

            service.updateEventType(id, command);
        }
    }

    @Nested
    @DisplayName("listDisciplineOptions()")
    class ListDisciplineOptionsTests {

        @Test
        @DisplayName("should return sorted discipline IDs from ORIS as strings")
        void shouldReturnSortedDisciplineIdsWhenOrisAvailable() {
            var disciplineMap = Map.of(
                    "3", new DisciplineListEntry("3", "Orientační běh", null, null, null, null),
                    "1", new DisciplineListEntry("1", "LOB", null, null, null, null),
                    "7", new DisciplineListEntry("7", "Sprint", null, null, null, null)
            );
            when(orisApiClient.listDisciplines())
                    .thenReturn(new OrisApiClient.OrisResponse<>(disciplineMap, "JSON", "OK", null, "getList"));

            List<String> options = service.listDisciplineOptions();

            assertThat(options).containsExactly("1", "3", "7");
        }

        @Test
        @DisplayName("should sort discipline IDs numerically, not lexicographically")
        void shouldReturnNumericallyOrderedDisciplineIds() {
            var disciplineMap = Map.of(
                    "10", new DisciplineListEntry("10", "Noc", null, null, null, null),
                    "2", new DisciplineListEntry("2", "LOB", null, null, null, null),
                    "9", new DisciplineListEntry("9", "Sprint", null, null, null, null)
            );
            when(orisApiClient.listDisciplines())
                    .thenReturn(new OrisApiClient.OrisResponse<>(disciplineMap, "JSON", "OK", null, "getList"));

            List<String> options = service.listDisciplineOptions();

            assertThat(options).containsExactly("2", "9", "10");
        }

        @Test
        @DisplayName("should return empty list when ORIS returns non-OK status")
        void shouldReturnEmptyListWhenOrisReturnsNonOkStatus() {
            when(orisApiClient.listDisciplines())
                    .thenReturn(new OrisApiClient.OrisResponse<>(null, "JSON", "ERR", null, "getList"));

            List<String> options = service.listDisciplineOptions();

            assertThat(options).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when ORIS call throws RuntimeException")
        void shouldReturnEmptyListWhenOrisCallThrows() {
            when(orisApiClient.listDisciplines())
                    .thenThrow(new RuntimeException("ORIS connection refused"));

            List<String> options = service.listDisciplineOptions();

            assertThat(options).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when ORIS is not available")
        void shouldReturnEmptyListWhenOrisNotAvailable() {
            EventTypeManagementService serviceWithoutOris =
                    new EventTypeManagementService(eventTypeRepository, Optional.empty());

            List<String> options = serviceWithoutOris.listDisciplineOptions();

            assertThat(options).isEmpty();
        }
    }
}
