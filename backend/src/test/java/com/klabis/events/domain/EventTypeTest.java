package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.DisciplineId;
import com.klabis.events.EventTypeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EventType domain tests")
class EventTypeTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create event type with name and sort order")
        void shouldCreateWithNameAndSortOrder() {
            var command = new EventType.CreateEventType("Trénink", null, null, null);

            EventType eventType = EventType.create(command, 1);

            assertThat(eventType.getId()).isNotNull();
            assertThat(eventType.getName()).isEqualTo("Trénink");
            assertThat(eventType.getSortOrder()).isEqualTo(1);
            assertThat(eventType.getColor()).isEmpty();
        }

        @Test
        @DisplayName("should create event type with color")
        void shouldCreateWithColor() {
            var command = new EventType.CreateEventType("Závod", "#ff0000", null, null);

            EventType eventType = EventType.create(command, 2);

            assertThat(eventType.getColor()).contains("#ff0000");
        }

        @Test
        @DisplayName("should use provided sort order over resolved one")
        void shouldUseProvidedSortOrder() {
            var command = new EventType.CreateEventType("Závod", null, 5, null);

            EventType eventType = EventType.create(command, 99);

            assertThat(eventType.getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("should use resolved sort order when none provided")
        void shouldUseResolvedSortOrderWhenNoneProvided() {
            var command = new EventType.CreateEventType("Závod", null, null, null);

            EventType eventType = EventType.create(command, 42);

            assertThat(eventType.getSortOrder()).isEqualTo(42);
        }

        @Test
        @DisplayName("should throw when name is blank")
        void shouldThrowWhenNameIsBlank() {
            assertThatThrownBy(() -> EventType.create(new EventType.CreateEventType("  ", null, null, null), 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when name is null")
        void shouldThrowWhenNameIsNull() {
            assertThatThrownBy(() -> EventType.create(new EventType.CreateEventType(null, null, null, null), 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when name exceeds 100 characters")
        void shouldThrowWhenNameTooLong() {
            String longName = "A".repeat(101);
            assertThatThrownBy(() -> EventType.create(new EventType.CreateEventType(longName, null, null, null), 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("should accept name of exactly 100 characters")
        void shouldAcceptNameOf100Characters() {
            String name = "A".repeat(100);
            EventType eventType = EventType.create(new EventType.CreateEventType(name, null, null, null), 1);
            assertThat(eventType.getName()).hasSize(100);
        }

        @Test
        @DisplayName("should throw when color has invalid format")
        void shouldThrowWhenColorIsInvalid() {
            assertThatThrownBy(() -> EventType.create(new EventType.CreateEventType("Závod", "red", null, null), 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hex");
        }

        @Test
        @DisplayName("should throw when color is missing #")
        void shouldThrowWhenColorMissingHash() {
            assertThatThrownBy(() -> EventType.create(new EventType.CreateEventType("Závod", "ff0000", null, null), 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should accept all valid hex color formats")
        void shouldAcceptValidHexColors() {
            assertThat(EventType.create(new EventType.CreateEventType("A", "#aabbcc", null, null), 1).getColor()).contains("#aabbcc");
            assertThat(EventType.create(new EventType.CreateEventType("B", "#AABBCC", null, null), 1).getColor()).contains("#AABBCC");
            assertThat(EventType.create(new EventType.CreateEventType("C", "#123456", null, null), 1).getColor()).contains("#123456");
        }

        @Test
        @DisplayName("should start with null audit metadata")
        void shouldStartWithNullAuditMetadata() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Trénink", null, null, null), 1);
            assertThat(eventType.getAuditMetadata()).isNull();
        }

        @Test
        @DisplayName("should generate unique ids")
        void shouldGenerateUniqueIds() {
            var cmd = new EventType.CreateEventType("Trénink", null, null, null);
            assertThat(EventType.create(cmd, 1).getId()).isNotEqualTo(EventType.create(cmd, 1).getId());
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("should update name and color and sort order")
        void shouldUpdateAllFields() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Old", null, null, null), 1);

            eventType.update(new EventType.UpdateEventType("New", "#00ff00", 5, null));

            assertThat(eventType.getName()).isEqualTo("New");
            assertThat(eventType.getColor()).contains("#00ff00");
            assertThat(eventType.getSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("should clear color when null provided")
        void shouldClearColorWhenNull() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", "#ff0000", null, null), 1);

            eventType.update(new EventType.UpdateEventType("Type", null, null, null));

            assertThat(eventType.getColor()).isEmpty();
        }

        @Test
        @DisplayName("should not change sort order when null provided in update")
        void shouldNotChangeSortOrderWhenNull() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, 3, null), 0);

            eventType.update(new EventType.UpdateEventType("Type", null, null, null));

            assertThat(eventType.getSortOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("should throw when updated name is blank")
        void shouldThrowWhenUpdatedNameBlank() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, null, null), 1);
            assertThatThrownBy(() -> eventType.update(new EventType.UpdateEventType("", null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when updated color is invalid")
        void shouldThrowWhenUpdatedColorInvalid() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, null, null), 1);
            assertThatThrownBy(() -> eventType.update(new EventType.UpdateEventType("Type", "notacolor", null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class ReconstructTests {

        @Test
        @DisplayName("should reconstruct with all fields including audit")
        void shouldReconstructWithAudit() {
            EventTypeId id = EventTypeId.generate();
            AuditMetadata audit = new AuditMetadata(Instant.now(), "admin", Instant.now(), "admin", 2L);

            EventType eventType = EventType.reconstruct(id, "Trénink", "#aabbcc", 3, audit, null);

            assertThat(eventType.getId()).isEqualTo(id);
            assertThat(eventType.getName()).isEqualTo("Trénink");
            assertThat(eventType.getColor()).contains("#aabbcc");
            assertThat(eventType.getSortOrder()).isEqualTo(3);
            assertThat(eventType.getAuditMetadata()).isEqualTo(audit);
        }

        @Test
        @DisplayName("should reconstruct with null color")
        void shouldReconstructWithNullColor() {
            EventType eventType = EventType.reconstruct(EventTypeId.generate(), "Type", null, 0, null, null);
            assertThat(eventType.getColor()).isEmpty();
        }

        @Test
        @DisplayName("should reconstruct with disciplineIds")
        void shouldReconstructWithDisciplineIds() {
            DisciplineId first = DisciplineId.generate();
            DisciplineId second = DisciplineId.generate();
            EventType eventType = EventType.reconstruct(EventTypeId.generate(), "Type", null, 0, null, Set.of(first, second));
            assertThat(eventType.getDisciplineIds()).containsExactlyInAnyOrder(first, second);
        }

        @Test
        @DisplayName("should reconstruct with empty disciplineIds when null provided")
        void shouldReconstructWithEmptyWhenNullDisciplineIds() {
            EventType eventType = EventType.reconstruct(EventTypeId.generate(), "Type", null, 0, null, null);
            assertThat(eventType.getDisciplineIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("disciplineIds")
    class DisciplineIdsTests {

        @Test
        @DisplayName("create with null disciplineIds defaults to empty set")
        void createWithNullDisciplineIdsDefaultsToEmpty() {
            var command = new EventType.CreateEventType("Trénink", null, null, null);
            EventType eventType = EventType.create(command, 1);
            assertThat(eventType.getDisciplineIds()).isEmpty();
        }

        @Test
        @DisplayName("create with disciplineIds stores them")
        void createWithDisciplineIds() {
            DisciplineId first = DisciplineId.generate();
            DisciplineId second = DisciplineId.generate();
            var command = new EventType.CreateEventType("Závod", null, null, Set.of(first, second));
            EventType eventType = EventType.create(command, 1);
            assertThat(eventType.getDisciplineIds()).containsExactlyInAnyOrder(first, second);
        }

        @Test
        @DisplayName("update with null disciplineIds clears them")
        void updateWithNullDisciplineIdsClears() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, null, Set.of(DisciplineId.generate())), 1);
            eventType.update(new EventType.UpdateEventType("Type", null, null, null));
            assertThat(eventType.getDisciplineIds()).isEmpty();
        }

        @Test
        @DisplayName("update with disciplineIds replaces them")
        void updateWithDisciplineIdsReplaces() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, null, Set.of(DisciplineId.generate())), 1);
            DisciplineId replacementFirst = DisciplineId.generate();
            DisciplineId replacementSecond = DisciplineId.generate();
            eventType.update(new EventType.UpdateEventType("Type", null, null, Set.of(replacementFirst, replacementSecond)));
            assertThat(eventType.getDisciplineIds()).containsExactlyInAnyOrder(replacementFirst, replacementSecond);
        }

        @Test
        @DisplayName("getDisciplineIds returns an immutable copy")
        void getDisciplineIdsReturnsImmutableCopy() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, null, Set.of(DisciplineId.generate(), DisciplineId.generate())), 1);
            Set<DisciplineId> ids = eventType.getDisciplineIds();
            assertThatThrownBy(() -> ids.add(DisciplineId.generate())).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
