package club.klabis.events.domain;

import club.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static club.klabis.events.domain.EventTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests for Registration entity")
class RegistrationTest {

    @Nested
    @DisplayName("Constructor validation tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Should successfully create registration when all parameters are valid")
        void shouldCreateRegistrationWithValidParameters() {
            // Arrange
            MemberId memberId = MEMBER_1;
            String siNumber = "123456";
            String category = CATEGORY_D12.name();

            // Act
            Registration registration = new Registration(memberId, siNumber, category);

            // Assert
            assertThat(registration).isNotNull();
            assertThat(registration.getMemberId()).isEqualTo(memberId);
            assertThat(registration.getSiNumber()).isEqualTo(siNumber);
            assertThat(registration.getCategory()).isEqualTo(CATEGORY_D12);
        }

        @Test
        @DisplayName("Should throw exception when memberId is null")
        void shouldThrowExceptionWhenMemberIdIsNull() {
            // Arrange
            MemberId memberId = null;
            String siNumber = "123456";
            String category = CATEGORY_D12.name();

            // Act & Assert
            assertThatThrownBy(() -> new Registration(memberId, siNumber, category))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("memberId must not be null");
        }

        @Test
        @DisplayName("Should throw exception when siNumber is null")
        void shouldThrowExceptionWhenSiNumberIsNull() {
            // Arrange
            MemberId memberId = MEMBER_1;
            String siNumber = null;
            String category = CATEGORY_D12.name();

            // Act & Assert
            assertThatThrownBy(() -> new Registration(memberId, siNumber, category))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("siNumber must not be null");
        }

        @Test
        @DisplayName("Should throw exception when category is null")
        void shouldThrowExceptionWhenCategoryIsNull() {
            // Arrange
            MemberId memberId = MEMBER_1;
            String siNumber = "123456";
            String category = null;

            // Act & Assert
            assertThatThrownBy(() -> new Registration(memberId, siNumber, category))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("category must not be null");
        }

        @Test
        @DisplayName("Should set createdAt timestamp when registration is created")
        void shouldSetCreatedAtTimestamp() {
            // Arrange
            ZonedDateTime beforeCreation = ZonedDateTime.now();

            // Act
            Registration registration = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());

            // Assert
            ZonedDateTime afterCreation = ZonedDateTime.now();
            assertThat(registration.getCreatedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(beforeCreation)
                    .isBeforeOrEqualTo(afterCreation);
        }

        @Test
        @DisplayName("Should set updatedAt equal to createdAt when registration is created")
        void shouldSetUpdatedAtEqualToCreatedAt() {
            // Act
            Registration registration = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());

            // Assert
            assertThat(registration.getUpdatedAt())
                    .isNotNull()
                    .isEqualTo(registration.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Update method tests")
    class UpdateMethodTests {

        @Test
        @DisplayName("Should successfully update both category and SI number")
        void shouldUpdateCategoryAndSiNumber() {
            // Arrange
            Registration registration = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());
            String newCategory = CATEGORY_H21.name();
            String newSiNumber = "654321";

            // Act
            registration.update(newCategory, newSiNumber);

            // Assert
            assertThat(registration.getCategory()).isEqualTo(CATEGORY_H21);
            assertThat(registration.getSiNumber()).isEqualTo(newSiNumber);
        }

        @Test
        @DisplayName("Should update only category when SI number remains the same")
        void shouldUpdateOnlyCategory() {
            // Arrange
            Registration registration = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());
            String originalSiNumber = registration.getSiNumber();

            // Act
            registration.update(CATEGORY_H21.name(), originalSiNumber);

            // Assert
            assertThat(registration.getCategory()).isEqualTo(CATEGORY_H21);
            assertThat(registration.getSiNumber()).isEqualTo(originalSiNumber);
        }

        @Test
        @DisplayName("Should update only SI number when category remains the same")
        void shouldUpdateOnlySiNumber() {
            // Arrange
            Registration registration = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());
            String newSiNumber = "999999";

            // Act
            registration.update(CATEGORY_D12.name(), newSiNumber);

            // Assert
            assertThat(registration.getCategory()).isEqualTo(CATEGORY_D12);
            assertThat(registration.getSiNumber()).isEqualTo(newSiNumber);
        }

        @Test
        @DisplayName("Should update updatedAt timestamp when registration is modified")
        void shouldUpdateUpdatedAtTimestamp() throws InterruptedException {
            // Arrange
            Registration registration = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());
            ZonedDateTime originalUpdatedAt = registration.getUpdatedAt();

            // Small delay to ensure timestamp difference
            Thread.sleep(10);

            // Act
            registration.update(CATEGORY_H21.name(), "654321");

            // Assert
            assertThat(registration.getUpdatedAt())
                    .isAfter(originalUpdatedAt);
        }

        @Test
        @DisplayName("Should NOT change createdAt timestamp when registration is updated")
        void shouldNotChangeCreatedAtTimestamp() throws InterruptedException {
            // Arrange
            Registration registration = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());
            ZonedDateTime originalCreatedAt = registration.getCreatedAt();

            // Small delay
            Thread.sleep(10);

            // Act
            registration.update(CATEGORY_H21.name(), "654321");

            // Assert
            assertThat(registration.getCreatedAt()).isEqualTo(originalCreatedAt);
        }

        @Test
        @DisplayName("Should update timestamp on each consecutive update")
        void shouldUpdateTimestampOnEachUpdate() throws InterruptedException {
            // Arrange
            Registration registration = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());

            // Act - First update
            Thread.sleep(10);
            registration.update(CATEGORY_H21.name(), "111111");
            ZonedDateTime firstUpdateTime = registration.getUpdatedAt();

            // Act - Second update
            Thread.sleep(10);
            registration.update(CATEGORY_D21.name(), "222222");
            ZonedDateTime secondUpdateTime = registration.getUpdatedAt();

            // Assert
            assertThat(secondUpdateTime).isAfter(firstUpdateTime);
        }
    }

    @Nested
    @DisplayName("Equals and hashCode tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when two registrations have the same memberId (identity-based equality)")
        void shouldBeEqualWhenSameMemberId() {
            // Arrange
            Registration registration1 = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());
            Registration registration2 = new Registration(MEMBER_1, "999999", CATEGORY_H21.name());

            // Assert
            assertThat(registration1).isEqualTo(registration2);
        }

        @Test
        @DisplayName("Should NOT be equal when registrations have different memberIds")
        void shouldNotBeEqualWhenDifferentMemberId() {
            // Arrange
            Registration registration1 = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());
            Registration registration2 = new Registration(MEMBER_2, "123456", CATEGORY_D12.name());

            // Assert
            assertThat(registration1).isNotEqualTo(registration2);
        }

        @Test
        @DisplayName("Should be equal even when category and SI number are different (state doesn't affect equality)")
        void shouldBeEqualEvenWhenDifferentCategoryAndSi() {
            // Arrange
            Registration registration1 = new Registration(MEMBER_1, "111111", CATEGORY_D12.name());
            Registration registration2 = new Registration(MEMBER_1, "222222", CATEGORY_H21.name());

            // Assert - Only memberId matters for equality
            assertThat(registration1).isEqualTo(registration2);
        }

        @Test
        @DisplayName("Should have same hashCode when memberIds are equal")
        void shouldHaveSameHashCodeWhenSameMemberId() {
            // Arrange
            Registration registration1 = new Registration(MEMBER_1, "123456", CATEGORY_D12.name());
            Registration registration2 = new Registration(MEMBER_1, "654321", CATEGORY_H21.name());

            // Assert
            assertThat(registration1.hashCode()).isEqualTo(registration2.hashCode());
        }
    }
}
