package com.klabis.events;

import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRegistrationCreateEventRegistrationBuilder;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Test for EventRegistration value object (RED phase).
 * <p>
 * Tests business rules:
 * - EventRegistration creation with required fields
 * - Registration timestamp is set automatically
 * - Validation of memberId and siCardNumber
 */
@DisplayName("EventRegistration Value Object")
class EventRegistrationTest {

    @Nested
    @DisplayName("create() factory method")
    class CreateMethod {

        @Test
        @DisplayName("should create registration with all required fields and timestamp")
        void shouldCreateRegistrationWithAllRequiredFieldsAndTimestamp() {
            // Arrange
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            Instant beforeCreation = Instant.now();

            // Act
            EventRegistration registration = EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId)
                            .siCardNumber(siCardNumber)
                            .build());

            // Assert
            assertThat(registration).isNotNull();
            assertThat(registration.id()).isNotNull();
            assertThat(registration.memberId()).isEqualTo(memberId);
            assertThat(registration.siCardNumber()).isEqualTo(siCardNumber);
            assertThat(registration.registeredAt()).isNotNull();
            assertThat(registration.registeredAt()).isAfterOrEqualTo(beforeCreation);
            assertThat(registration.registeredAt()).isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("should generate unique ID for each registration")
        void shouldGenerateUniqueIdForEachRegistration() {
            // Arrange
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            // Act
            EventRegistration registration1 = EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(siCardNumber).build());
            EventRegistration registration2 = EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(siCardNumber).build());

            // Assert
            assertThat(registration1.id()).isNotEqualTo(registration2.id());
        }

        @Test
        @DisplayName("should fail when memberId is null")
        void shouldFailWhenMemberIdIsNull() {
            // Arrange
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            // Act & Assert
            assertThatThrownBy(() -> EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .siCardNumber(siCardNumber).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("memberId");
        }

        @Test
        @DisplayName("should fail when siCardNumber is null")
        void shouldFailWhenSiCardNumberIsNull() {
            // Arrange
            MemberId memberId = new MemberId(UUID.randomUUID());

            // Act & Assert
            assertThatThrownBy(() -> EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("siCardNumber");
        }
    }

    @Nested
    @DisplayName("withChanges() method")
    class WithChanges {

        @Test
        @DisplayName("should return new instance with new siCardNumber and category, preserving id and registeredAt")
        void shouldReturnNewInstancePreservingIdAndRegisteredAt() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            UUID originalId = UUID.randomUUID();
            SiCardNumber originalSiCard = SiCardNumber.of("123456");
            Instant originalRegisteredAt = Instant.now().minusSeconds(60);
            EventRegistration original = EventRegistration.reconstruct(originalId, memberId, originalSiCard, "M21", originalRegisteredAt);

            SiCardNumber newSiCard = SiCardNumber.of("654321");
            EventRegistration updated = original.withChanges(newSiCard, "W35");

            assertThat(updated.id()).isEqualTo(originalId);
            assertThat(updated.registeredAt()).isEqualTo(originalRegisteredAt);
            assertThat(updated.memberId()).isEqualTo(memberId);
            assertThat(updated.siCardNumber()).isEqualTo(newSiCard);
            assertThat(updated.category()).isEqualTo("W35");
        }

        @Test
        @DisplayName("should return a different instance (not the same reference)")
        void shouldReturnDifferentInstance() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            EventRegistration original = EventRegistration.reconstruct(UUID.randomUUID(), memberId, SiCardNumber.of("123456"), null, Instant.now());

            EventRegistration updated = original.withChanges(SiCardNumber.of("654321"), null);

            assertThat(updated).isNotSameAs(original);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when IDs are same")
        void shouldBeEqualWhenIdsAreSame() {
            // Arrange
            UUID id = UUID.randomUUID();
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            Instant registeredAt = Instant.now();

            EventRegistration registration1 = EventRegistration.reconstruct(id, memberId, siCardNumber, null, registeredAt);
            EventRegistration registration2 = EventRegistration.reconstruct(id, memberId, siCardNumber, null, registeredAt);

            // Assert
            assertThat(registration1)
                    .isEqualTo(registration2)
                    .hasSameHashCodeAs(registration2);
        }

        @Test
        @DisplayName("should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            // Arrange
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            Instant registeredAt = Instant.now();

            EventRegistration registration1 = EventRegistration.reconstruct(UUID.randomUUID(),
                    memberId,
                    siCardNumber,
                    null,
                    registeredAt);
            EventRegistration registration2 = EventRegistration.reconstruct(UUID.randomUUID(),
                    memberId,
                    siCardNumber,
                    null,
                    registeredAt);

            // Assert
            assertThat(registration1).isNotEqualTo(registration2);
        }
    }
}
