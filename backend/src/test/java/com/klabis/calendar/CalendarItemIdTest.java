package com.klabis.calendar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CalendarItemId Value Object Tests")
class CalendarItemIdTest {

    @Nested
    @DisplayName("UUID Wrapping")
    class UuidWrapping {

        @Test
        @DisplayName("Should create CalendarItemId from valid UUID")
        void shouldCreateCalendarItemIdFromValidUuid() {
            // Given
            UUID uuid = UUID.randomUUID();

            // When
            CalendarItemId calendarItemId = CalendarItemId.of(uuid);

            // Then
            assertThat(calendarItemId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("Should create CalendarItemId from UUID string")
        void shouldCreateCalendarItemIdFromUuidString() {
            // Given
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            UUID uuid = UUID.fromString(uuidString);

            // When
            CalendarItemId calendarItemId = CalendarItemId.of(uuid);

            // Then
            assertThat(calendarItemId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("Should generate new CalendarItemId with random UUID")
        void shouldGenerateNewCalendarItemIdWithRandomUuid() {
            // When
            CalendarItemId calendarItemId = CalendarItemId.generate();

            // Then
            assertThat(calendarItemId.value()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Null Rejection")
    class NullRejection {

        @Test
        @DisplayName("Should throw exception when UUID is null")
        void shouldThrowWhenUuidIsNull() {
            assertThatThrownBy(() -> CalendarItemId.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Calendar item ID is required");
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
            CalendarItemId calendarItemId1 = CalendarItemId.of(uuid);
            CalendarItemId calendarItemId2 = CalendarItemId.of(uuid);

            // Then
            assertThat(calendarItemId1)
                    .isEqualTo(calendarItemId2)
                    .hasSameHashCodeAs(calendarItemId2);
        }

        @Test
        @DisplayName("Should not be equal when UUIDs are different")
        void shouldNotBeEqualWhenUuidsAreDifferent() {
            // Given
            CalendarItemId calendarItemId1 = CalendarItemId.of(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            CalendarItemId calendarItemId2 = CalendarItemId.of(UUID.fromString("660e8400-e29b-41d4-a716-446655440000"));

            // Then
            assertThat(calendarItemId1).isNotEqualTo(calendarItemId2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // Given
            CalendarItemId calendarItemId = CalendarItemId.of(UUID.randomUUID());

            // Then
            assertThat(calendarItemId).isEqualTo(calendarItemId);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            CalendarItemId calendarItemId = CalendarItemId.of(UUID.randomUUID());

            // Then
            assertThat(calendarItemId).isNotEqualTo(null);
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
            CalendarItemId calendarItemId = CalendarItemId.of(uuid);

            // Then
            assertThat(calendarItemId.toString()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }
    }
}
