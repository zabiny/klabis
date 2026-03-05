package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PhoneNumber Value Object Tests")
class PhoneNumberTest extends ValueObjectTestBase<PhoneNumber> {

    @Override
    protected PhoneNumber createValidValue() {
        return PhoneNumber.of("+420123456789");
    }

    @Override
    protected PhoneNumber createDifferentValue() {
        return PhoneNumber.of("+420987654321");
    }

    @Override
    protected String expectedToString() {
        return "+420123456789";
    }

    @Test
    @DisplayName("Should create valid E.164 phone number")
    void shouldCreateValidE164PhoneNumber() {
        // Given
        String phone = "+420123456789";

        // When
        PhoneNumber phoneNumber = PhoneNumber.of(phone);

        // Then
        assertThat(phoneNumber.value()).isEqualTo(phone);
    }

    @Nested
    @DisplayName("Basic Validation")
    class BasicValidation {

        @Test
        @DisplayName("Should throw exception when phone is null")
        void shouldThrowWhenPhoneIsNull() {
            assertThatThrownBy(() -> PhoneNumber.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phone number is required");
        }

        @Test
        @DisplayName("Should throw exception when phone is blank")
        void shouldThrowWhenPhoneIsBlank() {
            assertThatThrownBy(() -> PhoneNumber.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phone number is required");
        }

        @Test
        @DisplayName("Should throw exception when phone doesn't start with +")
        void shouldThrowWhenPhoneDoesntStartWithPlus() {
            assertThatThrownBy(() -> PhoneNumber.of("420123456789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must start with +");

            assertThatThrownBy(() -> PhoneNumber.of("00420123456789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must start with +");
        }

        @Test
        @DisplayName("Should reject phone with only + sign")
        void shouldRejectPhoneWithOnlyPlusSign() {
            assertThatThrownBy(() -> PhoneNumber.of("+"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain at least one digit");
        }

        @Test
        @DisplayName("Should reject phone with + and only spaces")
        void shouldRejectPhoneWithPlusAndOnlySpaces() {
            assertThatThrownBy(() -> PhoneNumber.of("+   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain at least one digit");
        }
    }

    @Nested
    @DisplayName("E.164 Format Validation")
    class E164FormatValidation {

        @Test
        @DisplayName("Should throw exception when phone contains letters")
        void shouldThrowWhenPhoneContainsLetters() {
            assertThatThrownBy(() -> PhoneNumber.of("+420abc456789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain only digits");

            assertThatThrownBy(() -> PhoneNumber.of("+420 123 abc 789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain only digits");
        }

        @Test
        @DisplayName("Should throw exception when phone contains special characters")
        void shouldThrowWhenPhoneContainsSpecialCharacters() {
            assertThatThrownBy(() -> PhoneNumber.of("+420-123-456-789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain only digits");

            assertThatThrownBy(() -> PhoneNumber.of("+420.123.456.789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain only digits");

            assertThatThrownBy(() -> PhoneNumber.of("(+420) 123 456 789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must start with +");
        }

        @Test
        @DisplayName("Should reject phone with multiple + symbols")
        void shouldRejectPhoneWithMultiplePlusSymbols() {
            assertThatThrownBy(() -> PhoneNumber.of("++420123456789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain only digits");

            assertThatThrownBy(() -> PhoneNumber.of("+420+123456789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must contain only digits");
        }
    }

    @Nested
    @DisplayName("Valid Format Acceptance")
    class ValidFormatAcceptance {

        @Test
        @DisplayName("Should create valid E.164 phone number with spaces")
        void shouldCreateValidE164PhoneNumberWithSpaces() {
            // Given
            String phone = "+420 123 456 789";

            // When
            PhoneNumber phoneNumber = PhoneNumber.of(phone);

            // Then
            assertThat(phoneNumber.value()).isEqualTo(phone);
        }

        @Test
        @DisplayName("Should accept various E.164 formats with spaces")
        void shouldAcceptVariousE164FormatsWithSpaces() {
            assertThatCode(() -> PhoneNumber.of("+1 555 123 4567"))
                    .doesNotThrowAnyException();

            assertThatCode(() -> PhoneNumber.of("+44 20 7946 0958"))
                    .doesNotThrowAnyException();

            assertThatCode(() -> PhoneNumber.of("+49 30 12345678"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept phone with only + and digits")
        void shouldAcceptPhoneWithOnlyPlusAndDigits() {
            assertThatCode(() -> PhoneNumber.of("+420123456789"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept phone with +, digits, and spaces only")
        void shouldAcceptPhoneWithPlusDigitsAndSpacesOnly() {
            assertThatCode(() -> PhoneNumber.of("+420 123 456 789"))
                    .doesNotThrowAnyException();

            assertThatCode(() -> PhoneNumber.of("+1 800 555 1234"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept common international formats")
        void shouldAcceptCommonInternationalFormats() {
            assertThatCode(() -> PhoneNumber.of("+15551234567"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> PhoneNumber.of("+442079460958"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> PhoneNumber.of("+493012345678"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> PhoneNumber.of("+33123456789"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Phone Number Normalization")
    class PhoneNumberNormalization {

        @Test
        @DisplayName("Should trim whitespace from phone")
        void shouldTrimWhitespaceFromPhone() {
            PhoneNumber phoneNumber = PhoneNumber.of("  +420 123 456 789  ");
            assertThat(phoneNumber.value()).isEqualTo("+420 123 456 789");
        }
    }
}
