package com.klabis.events.application;

import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.events.DisciplineId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineRepository;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.events.domain.OrisDisciplineAlreadyMappedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventTypeManagementService tests")
class EventTypeManagementServiceTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private DisciplineRepository disciplineRepository;

    private EventTypeManagementService service;

    @BeforeEach
    void setUp() {
        service = new EventTypeManagementService(eventTypeRepository, disciplineRepository);
    }

    @Nested
    @DisplayName("createEventType() — orisDisciplineId uniqueness")
    class CreateEventTypeDisciplineValidation {

        @Test
        @DisplayName("should throw OrisDisciplineAlreadyMappedException when discipline ID is mapped to another event type")
        void shouldThrowWhenDisciplineAlreadyMappedToAnotherType() {
            DisciplineId disciplineId = DisciplineId.generate();
            var command = new EventType.CreateEventType("New Type", null, null, Set.of(disciplineId));
            EventType existing = EventType.create(new EventType.CreateEventType("Existing", null, null, Set.of(disciplineId)), 0);

            when(eventTypeRepository.existsByNameIgnoreCase("New Type")).thenReturn(false);
            when(eventTypeRepository.findByDisciplineId(disciplineId)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.createEventType(command))
                    .isInstanceOf(OrisDisciplineAlreadyMappedException.class);
        }

        @Test
        @DisplayName("should create successfully when no discipline ID conflicts")
        void shouldCreateWhenNoDisciplineConflict() {
            var command = new EventType.CreateEventType("New Type", null, null, Set.of(DisciplineId.generate(), DisciplineId.generate()));

            when(eventTypeRepository.existsByNameIgnoreCase("New Type")).thenReturn(false);
            when(eventTypeRepository.findMaxSortOrder()).thenReturn(0);
            when(eventTypeRepository.findByDisciplineId(any(DisciplineId.class))).thenReturn(Optional.empty());
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
            DisciplineId disciplineId = DisciplineId.generate();
            EventType target = EventType.create(new EventType.CreateEventType("Target", null, null, Set.of()), 0);
            EventType other = EventType.create(new EventType.CreateEventType("Other", null, null, Set.of(disciplineId)), 1);

            var command = new EventType.UpdateEventType("Target Updated", null, null, Set.of(disciplineId));

            when(eventTypeRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(eventTypeRepository.findByDisciplineId(disciplineId)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.updateEventType(targetId, command))
                    .isInstanceOf(OrisDisciplineAlreadyMappedException.class);
        }

        @Test
        @DisplayName("should succeed when discipline ID already belongs to the same event type being updated")
        void shouldSucceedWhenDisciplineAlreadyBelongsToSameType() {
            EventTypeId id = EventTypeId.generate();
            DisciplineId disciplineId = DisciplineId.generate();
            EventType eventType = EventType.reconstruct(id, "Type", null, 0, null, Set.of(disciplineId));

            var command = new EventType.UpdateEventType("Type Updated", null, null, Set.of(disciplineId));

            when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
            when(eventTypeRepository.findByDisciplineId(disciplineId)).thenReturn(Optional.of(eventType));
            when(eventTypeRepository.save(any())).thenReturn(eventType);

            service.updateEventType(id, command);
        }

        @Test
        @DisplayName("should succeed when no discipline ID conflicts on update")
        void shouldSucceedWhenNoDisciplineConflictOnUpdate() {
            EventTypeId id = EventTypeId.generate();
            EventType eventType = EventType.reconstruct(id, "Type", null, 0, null, Set.of());
            DisciplineId disciplineId = DisciplineId.generate();

            var command = new EventType.UpdateEventType("Type", null, null, Set.of(disciplineId));

            when(eventTypeRepository.findById(id)).thenReturn(Optional.of(eventType));
            when(eventTypeRepository.findByDisciplineId(disciplineId)).thenReturn(Optional.empty());
            when(eventTypeRepository.save(any())).thenReturn(eventType);

            service.updateEventType(id, command);
        }
    }

    @Nested
    @DisplayName("listDisciplineOptions()")
    class ListDisciplineOptionsTests {

        @Test
        @DisplayName("should map local disciplines to value+prompt pairs, preserving repository order")
        void shouldReturnLocalDisciplinesAsOptions() {
            Discipline lob = Discipline.create(new Discipline.CreateDiscipline("LOB", "Lyžařský orientační běh"));
            Discipline ob = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            Discipline sprint = Discipline.create(new Discipline.CreateDiscipline("SPR", "Sprintová orientace"));
            when(disciplineRepository.findAllSorted()).thenReturn(List.of(lob, ob, sprint));

            List<HalFormsInlineOption> options = service.listDisciplineOptions();

            assertThat(options).containsExactly(
                    new HalFormsInlineOption(lob.getId().value().toString(), "Lyžařský orientační běh"),
                    new HalFormsInlineOption(ob.getId().value().toString(), "Orientační běh"),
                    new HalFormsInlineOption(sprint.getId().value().toString(), "Sprintová orientace")
            );
        }

        @Test
        @DisplayName("should return empty list when no local disciplines exist")
        void shouldReturnEmptyListWhenNoDisciplines() {
            when(disciplineRepository.findAllSorted()).thenReturn(List.of());

            List<HalFormsInlineOption> options = service.listDisciplineOptions();

            assertThat(options).isEmpty();
        }
    }
}
