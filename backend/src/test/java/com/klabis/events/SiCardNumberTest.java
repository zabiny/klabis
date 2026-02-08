package com.klabis.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SiCardNumber Value Object Tests")
class SiCardNumberTest {

    @Nested
    @DisplayName("Valid Numbers")
    class ValidNumbers {

        @ParameterizedTest
        @ValueSource(strings = {"1234", "123456", "12345678"})
        @DisplayName("Should accept valid 4-8 digit numbers")
        void shouldAcceptValid4To8DigitNumbers(String number) {
            // When
            SiCardNumber siCardNumber = SiCardNumber.of(number);

            // Then
            assertThat(siCardNumber.value()).isEqualTo(number);
        }

        @Test
        @DisplayName("Should accept 4-digit number")
        void shouldAccept4DigitNumber() {
            // Given
            String number = "1234";

            // When
            SiCardNumber siCardNumber = SiCardNumber.of(number);

            // Then
            assertThat(siCardNumber.value()).isEqualTo(number);
        }

        @Test
        @DisplayName("Should accept 6-digit number")
        void shouldAccept6DigitNumber() {
            // Given
            String number = "123456";

            // When
            SiCardNumber siCardNumber = SiCardNumber.of(number);

            // Then
            assertThat(siCardNumber.value()).isEqualTo(number);
        }

        @Test
        @DisplayName("Should accept 8-digit number")
        void shouldAccept8DigitNumber() {
            // Given
            String number = "12345678";

            // When
            SiCardNumber siCardNumber = SiCardNumber.of(number);

            // Then
            assertThat(siCardNumber.value()).isEqualTo(number);
        }
    }

    @Nested
    @DisplayName("Invalid Characters")
    class InvalidCharacters {

        @Test
        @DisplayName("Should reject number with letters")
        void shouldRejectNumberWithLetters() {
            assertThatThrownBy(() -> SiCardNumber.of("12A456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number must contain only digits");
        }

        @Test
        @DisplayName("Should reject number with special characters")
        void shouldRejectNumberWithSpecialCharacters() {
            assertThatThrownBy(() -> SiCardNumber.of("123-456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number must contain only digits");

            assertThatThrownBy(() -> SiCardNumber.of("123.456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number must contain only digits");
        }

        @Test
        @DisplayName("Should reject number with spaces")
        void shouldRejectNumberWithSpaces() {
            assertThatThrownBy(() -> SiCardNumber.of("123 456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number must contain only digits");
        }
    }

    @Nested
    @DisplayName("Invalid Length")
    class InvalidLength {

        @Test
        @DisplayName("Should reject too short number (3 digits)")
        void shouldRejectTooShortNumber() {
            assertThatThrownBy(() -> SiCardNumber.of("123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number must be between 4 and 8 digits");
        }

        @Test
        @DisplayName("Should reject too long number (9 digits)")
        void shouldRejectTooLongNumber() {
            assertThatThrownBy(() -> SiCardNumber.of("123456789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number must be between 4 and 8 digits");
        }

        @Test
        @DisplayName("Should reject 1-digit number")
        void shouldReject1DigitNumber() {
            assertThatThrownBy(() -> SiCardNumber.of("1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number must be between 4 and 8 digits");
        }

        @Test
        @DisplayName("Should reject 10-digit number")
        void shouldReject10DigitNumber() {
            assertThatThrownBy(() -> SiCardNumber.of("1234567890"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number must be between 4 and 8 digits");
        }
    }

    @Nested
    @DisplayName("Null and Blank Rejection")
    class NullAndBlankRejection {

        @Test
        @DisplayName("Should reject null")
        void shouldRejectNull() {
            assertThatThrownBy(() -> SiCardNumber.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number is required");
        }

        @Test
        @DisplayName("Should reject blank string")
        void shouldRejectBlankString() {
            assertThatThrownBy(() -> SiCardNumber.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number is required");
        }

        @Test
        @DisplayName("Should reject empty string")
        void shouldRejectEmptyString() {
            assertThatThrownBy(() -> SiCardNumber.of(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SI card number is required");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("Should be equal when numbers are same")
        void shouldBeEqualWhenNumbersAreSame() {
            // Given
            SiCardNumber number1 = SiCardNumber.of("123456");
            SiCardNumber number2 = SiCardNumber.of("123456");

            // Then
            assertThat(number1)
                    .isEqualTo(number2)
                    .hasSameHashCodeAs(number2);
        }

        @Test
        @DisplayName("Should not be equal when numbers are different")
        void shouldNotBeEqualWhenNumbersAreDifferent() {
            // Given
            SiCardNumber number1 = SiCardNumber.of("123456");
            SiCardNumber number2 = SiCardNumber.of("654321");

            // Then
            assertThat(number1).isNotEqualTo(number2);
        }
    }

    @Nested
    @DisplayName("Normalization")
    class Normalization {

        @Test
        @DisplayName("Should trim whitespace from number")
        void shouldTrimWhitespaceFromNumber() {
            // Given
            String number = "  123456  ";

            // When
            SiCardNumber siCardNumber = SiCardNumber.of(number);

            // Then
            assertThat(siCardNumber.value()).isEqualTo("123456");
        }
    }
}
