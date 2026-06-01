package com.klabis.events.eventtype.application;

import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventTypeManagementService tests")
class EventTypeManagementServiceTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @InjectMocks
    private EventTypeManagementService service;

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
}
