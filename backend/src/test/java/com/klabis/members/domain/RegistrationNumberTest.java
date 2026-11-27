package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

        static Stream<Arguments> invalidRegistrationNumbers() {
            return Stream.of(
                    arguments(named("null", (String) null), "Registration number is required"),
                    arguments(named("blank", ""), "Registration number is required"),
                    arguments(named("invalid format", "INVALID"), "Invalid registration number format"),
                    arguments(named("too short", "ZBM05"), "Invalid registration number format"),
                    arguments(named("too long", "ZBM05011"), "Invalid registration number format")
            );
        }

        @ParameterizedTest
        @MethodSource("invalidRegistrationNumbers")
        @DisplayName("should reject invalid registration number")
        void shouldRejectInvalidRegistrationNumber(String value, String expectedMessage) {
            assertThatThrownBy(() -> new RegistrationNumber(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessage);
        }

        @Test
        @DisplayName("should normalize lowercase club code to uppercase")
        void shouldNormalizeLowercaseClubCodeToUppercase() {
            assertThat(new RegistrationNumber("zbm0501").getValue()).isEqualTo("ZBM0501");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"ABC0501", "12A0501", "A1B0501"})
        @DisplayName("should support different club code formats")
        void shouldSupportDifferentClubCodeFormats(String value) {
            assertThat(new RegistrationNumber(value).getValue()).isEqualTo(value);
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
