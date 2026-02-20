package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EmailAddress Value Object Tests")
class EmailAddressTest extends ValueObjectTestBase<EmailAddress> {

    @Override
    protected EmailAddress createValidValue() {
        return EmailAddress.of("john@example.com");
    }

    @Override
    protected EmailAddress createDifferentValue() {
        return EmailAddress.of("jane@example.com");
    }

    @Override
    protected String expectedToString() {
        return "john@example.com";
    }

    @Test
    @DisplayName("Should create valid email address")
    void shouldCreateValidEmailAddress() {
        // Given
        String email = "john@example.com";

        // When
        EmailAddress emailAddress = EmailAddress.of(email);

        // Then
        assertThat(emailAddress.value()).isEqualTo(email);
    }

    @Nested
    @DisplayName("Email Format Validation")
    class EmailFormatValidation {

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowWhenEmailIsNull() {
            assertThatThrownBy(() -> EmailAddress.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email address is required");
        }

        @Test
        @DisplayName("Should throw exception when email is blank")
        void shouldThrowWhenEmailIsBlank() {
            assertThatThrownBy(() -> EmailAddress.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email address is required");
        }

        @Test
        @DisplayName("Should throw exception when email has no @ symbol")
        void shouldThrowWhenEmailHasNoAtSymbol() {
            assertThatThrownBy(() -> EmailAddress.of("invalidemail.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");

            assertThatThrownBy(() -> EmailAddress.of("invalidemail"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("Should throw exception when email has no domain")
        void shouldThrowWhenEmailHasNoDomain() {
            assertThatThrownBy(() -> EmailAddress.of("user@"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");

            assertThatThrownBy(() -> EmailAddress.of("@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }
    }

    @Nested
    @DisplayName("Email Format Acceptance")
    class EmailFormatAcceptance {

        @Test
        @DisplayName("Should accept valid email with subdomain")
        void shouldAcceptValidEmailWithSubdomain() {
            assertThatCode(() -> EmailAddress.of("user@mail.example.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid email with numbers")
        void shouldAcceptValidEmailWithNumbers() {
            assertThatCode(() -> EmailAddress.of("user123@example123.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid email with special characters")
        void shouldAcceptValidEmailWithSpecialCharacters() {
            assertThatCode(() -> EmailAddress.of("user.name+tag@example.com"))
                    .doesNotThrowAnyException();

            assertThatCode(() -> EmailAddress.of("user_name@example.com"))
                    .doesNotThrowAnyException();

            assertThatCode(() -> EmailAddress.of("user-name@example.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid email with multiple dots")
        void shouldAcceptValidEmailWithMultipleDots() {
            assertThatCode(() -> EmailAddress.of("john.doe.smith@example.co.uk"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept common email formats")
        void shouldAcceptCommonEmailFormats() {
            assertThatCode(() -> EmailAddress.of("test@example.com"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> EmailAddress.of("first.last@example.co.uk"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> EmailAddress.of("user+tag@example.org"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> EmailAddress.of("user_name@example.net"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Email Normalization")
    class EmailNormalization {

        @Test
        @DisplayName("Should trim whitespace from email")
        void shouldTrimWhitespaceFromEmail() {
            EmailAddress emailAddress = EmailAddress.of("  john@example.com  ");
            assertThat(emailAddress.value()).isEqualTo("john@example.com");
        }
    }

    @Nested
    @DisplayName("Invalid Email Formats")
    class InvalidEmailFormats {

        @Test
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats() {
            assertThatThrownBy(() -> EmailAddress.of("plainaddress"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");

            assertThatThrownBy(() -> EmailAddress.of("@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");

            assertThatThrownBy(() -> EmailAddress.of("user@"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }
    }
}
