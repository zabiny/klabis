package com.klabis.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EventId Value Object Tests")
class EventIdTest {

    @Nested
    @DisplayName("UUID Wrapping")
    class UuidWrapping {

        @Test
        @DisplayName("Should create EventId from valid UUID")
        void shouldCreateEventIdFromValidUuid() {
            // Given
            UUID uuid = UUID.randomUUID();

            // When
            EventId eventId = EventId.of(uuid);

            // Then
            assertThat(eventId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("Should create EventId from UUID string")
        void shouldCreateEventIdFromUuidString() {
            // Given
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            UUID uuid = UUID.fromString(uuidString);

            // When
            EventId eventId = EventId.of(uuid);

            // Then
            assertThat(eventId.value()).isEqualTo(uuid);
        }
    }

    @Nested
    @DisplayName("Null Rejection")
    class NullRejection {

        @Test
        @DisplayName("Should throw exception when UUID is null")
        void shouldThrowWhenUuidIsNull() {
            assertThatThrownBy(() -> EventId.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event ID is required");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("Should be equal when UUIDs are same")
        void shouldBeEqualWhenUuidsAreSame() {
            // Given
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            EventId eventId1 = EventId.of(uuid);
            EventId eventId2 = EventId.of(uuid);

            // Then
            assertThat(eventId1)
                    .isEqualTo(eventId2)
                    .hasSameHashCodeAs(eventId2);
        }

        @Test
        @DisplayName("Should not be equal when UUIDs are different")
        void shouldNotBeEqualWhenUuidsAreDifferent() {
            // Given
            EventId eventId1 = EventId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            EventId eventId2 = EventId.of(UUID.fromString("660e8400-e29b-41d4-a716-446655440000"));

            // Then
            assertThat(eventId1).isNotEqualTo(eventId2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // Given
            EventId eventId = EventId.of(UUID.randomUUID());

            // Then
            assertThat(eventId).isEqualTo(eventId);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            EventId eventId = EventId.of(UUID.randomUUID());

            // Then
            assertThat(eventId).isNotEqualTo(null);
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {

        @Test
        @DisplayName("Should return UUID string representation")
        void shouldReturnUuidStringRepresentation() {
            // Given
            UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            EventId eventId = EventId.of(uuid);

            // Then
            assertThat(eventId.toString()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }
    }
}
