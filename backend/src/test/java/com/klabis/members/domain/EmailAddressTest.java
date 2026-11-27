package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("EmailAddress Value Object Tests")
class EmailAddressTest {

    @Test
    @DisplayName("Should create valid email address")
    void shouldCreateValidEmailAddress() {
        EmailAddress emailAddress = EmailAddress.of("john@example.com");

        assertThat(emailAddress.value()).isEqualTo("john@example.com");
    }

    static Stream<Arguments> invalidEmails() {
        return Stream.of(
                arguments(named("null", (String) null), "Email address is required"),
                arguments(named("blank", "   "), "Email address is required"),
                arguments(named("no @ symbol with dot", "invalidemail.com"), "Invalid email format"),
                arguments(named("no @ symbol", "invalidemail"), "Invalid email format"),
                arguments(named("plain address", "plainaddress"), "Invalid email format"),
                arguments(named("no domain", "user@"), "Invalid email format"),
                arguments(named("no local part", "@example.com"), "Invalid email format")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidEmails")
    @DisplayName("Should reject invalid email")
    void shouldRejectInvalidEmail(String email, String expectedMessage) {
        assertThatThrownBy(() -> EmailAddress.of(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "user@mail.example.com",
            "user123@example123.com",
            "user.name+tag@example.com",
            "user_name@example.com",
            "user-name@example.com",
            "john.doe.smith@example.co.uk",
            "test@example.com",
            "first.last@example.co.uk",
            "user+tag@example.org",
            "user_name@example.net"
    })
    @DisplayName("Should accept valid email formats")
    void shouldAcceptValidEmailFormats(String email) {
        assertThatCode(() -> EmailAddress.of(email)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimWhitespaceFromEmail() {
        EmailAddress emailAddress = EmailAddress.of("  john@example.com  ");
        assertThat(emailAddress.value()).isEqualTo("john@example.com");
    }
}
