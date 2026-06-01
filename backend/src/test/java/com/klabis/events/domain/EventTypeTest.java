package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
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
        @DisplayName("should reconstruct with orisDisciplineIds")
        void shouldReconstructWithOrisDisciplineIds() {
            EventType eventType = EventType.reconstruct(EventTypeId.generate(), "Type", null, 0, null, Set.of(1, 2));
            assertThat(eventType.getOrisDisciplineIds()).containsExactlyInAnyOrder(1, 2);
        }

        @Test
        @DisplayName("should reconstruct with empty orisDisciplineIds when null provided")
        void shouldReconstructWithEmptyWhenNullOrisDisciplineIds() {
            EventType eventType = EventType.reconstruct(EventTypeId.generate(), "Type", null, 0, null, null);
            assertThat(eventType.getOrisDisciplineIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("orisDisciplineIds")
    class OrisDisciplineIdsTests {

        @Test
        @DisplayName("create with null orisDisciplineIds defaults to empty set")
        void createWithNullOrisDisciplineIdsDefaultsToEmpty() {
            var command = new EventType.CreateEventType("Trénink", null, null, null);
            EventType eventType = EventType.create(command, 1);
            assertThat(eventType.getOrisDisciplineIds()).isEmpty();
        }

        @Test
        @DisplayName("create with orisDisciplineIds stores them")
        void createWithOrisDisciplineIds() {
            var command = new EventType.CreateEventType("Závod", null, null, Set.of(3, 5));
            EventType eventType = EventType.create(command, 1);
            assertThat(eventType.getOrisDisciplineIds()).containsExactlyInAnyOrder(3, 5);
        }

        @Test
        @DisplayName("update with null orisDisciplineIds clears them")
        void updateWithNullOrisDisciplineIdsClears() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, null, Set.of(1)), 1);
            eventType.update(new EventType.UpdateEventType("Type", null, null, null));
            assertThat(eventType.getOrisDisciplineIds()).isEmpty();
        }

        @Test
        @DisplayName("update with orisDisciplineIds replaces them")
        void updateWithOrisDisciplineIdsReplaces() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, null, Set.of(1)), 1);
            eventType.update(new EventType.UpdateEventType("Type", null, null, Set.of(2, 4)));
            assertThat(eventType.getOrisDisciplineIds()).containsExactlyInAnyOrder(2, 4);
        }

        @Test
        @DisplayName("getOrisDisciplineIds returns an immutable copy")
        void getOrisDisciplineIdsReturnsImmutableCopy() {
            EventType eventType = EventType.create(new EventType.CreateEventType("Type", null, null, Set.of(1, 2)), 1);
            Set<Integer> ids = eventType.getOrisDisciplineIds();
            assertThatThrownBy(() -> ids.add(99)).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
