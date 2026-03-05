package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Test for RegistrationNumber value object.
 * <p>
 * Tests business rules:
 * - Registration number format XXXYYDD (club code + birth year + sequence)
 * - Value object equality and immutability
 * - Valid format validation
 */
@DisplayName("RegistrationNumber Value Object")
class RegistrationNumberTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("should create registration number with valid format")
        void shouldCreateRegistrationNumberWithValidFormat() {
            // Act
            RegistrationNumber regNumber = new RegistrationNumber("ZBM0501");

            // Assert
            assertThat(regNumber).isNotNull();
            assertThat(regNumber.getValue()).isEqualTo("ZBM0501");
        }

        @Test
        @DisplayName("should fail when registration number is null")
        void shouldFailWhenRegistrationNumberIsNull() {
            assertThatThrownBy(() -> new RegistrationNumber(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Registration number is required");
        }

        @Test
        @DisplayName("should fail when registration number is blank")
        void shouldFailWhenRegistrationNumberIsBlank() {
            assertThatThrownBy(() -> new RegistrationNumber(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Registration number is required");
        }

        @Test
        @DisplayName("should fail when registration number has invalid format")
        void shouldFailWhenRegistrationNumberHasInvalidFormat() {
            assertThatThrownBy(() -> new RegistrationNumber("INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid registration number format");
        }

        @Test
        @DisplayName("should fail when registration number is too short")
        void shouldFailWhenRegistrationNumberIsTooShort() {
            assertThatThrownBy(() -> new RegistrationNumber("ZBM05"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid registration number format");
        }

        @Test
        @DisplayName("should fail when registration number is too long")
        void shouldFailWhenRegistrationNumberIsTooLong() {
            assertThatThrownBy(() -> new RegistrationNumber("ZBM05011"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid registration number format");
        }

        @Test
        @DisplayName("should handle uppercase and lowercase club codes")
        void shouldHandleUppercaseAndLowercaseClubCodes() {
            // Act
            RegistrationNumber regNumber = new RegistrationNumber("zbm0501");

            // Assert - should normalize to uppercase
            assertThat(regNumber.getValue()).isEqualTo("ZBM0501");
            assertThat(regNumber.getClubCode()).isEqualTo("ZBM");
        }

        @Test
        @DisplayName("should support different club code formats")
        void shouldSupportDifferentClubCodeFormats() {
            // Act
            RegistrationNumber regNumber1 = new RegistrationNumber("ABC0501");
            RegistrationNumber regNumber2 = new RegistrationNumber("12A0501");
            RegistrationNumber regNumber3 = new RegistrationNumber("A1B0501");

            // Assert
            assertThat(regNumber1.getClubCode()).isEqualTo("ABC");
            assertThat(regNumber2.getClubCode()).isEqualTo("12A");
            assertThat(regNumber3.getClubCode()).isEqualTo("A1B");
        }
    }

    @Nested
    @DisplayName("getClubCode() method")
    class GetClubCodeMethod {

        @Test
        @DisplayName("should extract club code from registration number")
        void shouldExtractClubCode() {
            // Act
            RegistrationNumber regNumber = new RegistrationNumber("ZBM0501");

            // Assert
            assertThat(regNumber.getClubCode()).isEqualTo("ZBM");
        }
    }

    @Nested
    @DisplayName("getBirthYear() method")
    class GetBirthYearMethod {

        @Test
        @DisplayName("should extract birth year from registration number")
        void shouldExtractBirthYear() {
            // Act
            RegistrationNumber regNumber = new RegistrationNumber("ZBM0501");

            // Assert
            assertThat(regNumber.getBirthYear()).isEqualTo(5); // Year 2005
        }
    }

    @Nested
    @DisplayName("getSequenceNumber() method")
    class GetSequenceNumberMethod {

        @Test
        @DisplayName("should extract sequence number from registration number")
        void shouldExtractSequenceNumber() {
            // Act
            RegistrationNumber regNumber = new RegistrationNumber("ZBM0501");

            // Assert
            assertThat(regNumber.getSequenceNumber()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Equality methods (equals, hashCode)")
    class EqualityMethods {

        @Test
        @DisplayName("should be equal when registration numbers are the same")
        void shouldBeEqualWhenRegistrationNumbersAreTheSame() {
            // Arrange
            RegistrationNumber regNumber1 = new RegistrationNumber("ZBM0501");
            RegistrationNumber regNumber2 = new RegistrationNumber("ZBM0501");

            // Assert
            assertThat(regNumber1).isEqualTo(regNumber2);
            assertThat(regNumber1.hashCode()).isEqualTo(regNumber2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when registration numbers are different")
        void shouldNotBeEqualWhenRegistrationNumbersAreDifferent() {
            // Arrange
            RegistrationNumber regNumber1 = new RegistrationNumber("ZBM0501");
            RegistrationNumber regNumber2 = new RegistrationNumber("ZBM0502");

            // Assert
            assertThat(regNumber1).isNotEqualTo(regNumber2);
        }
    }
}
