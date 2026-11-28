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

@DisplayName("PhoneNumber Value Object Tests")
class PhoneNumberTest {

    @Test
    @DisplayName("Should create valid E.164 phone number")
    void shouldCreateValidE164PhoneNumber() {
        PhoneNumber phoneNumber = PhoneNumber.of("+420123456789");

        assertThat(phoneNumber.value()).isEqualTo("+420123456789");
    }

    static Stream<Arguments> invalidPhones() {
        return Stream.of(
                arguments(named("null", (String) null), "Phone number is required"),
                arguments(named("blank", "   "), "Phone number is required"),
                arguments(named("without +", "420123456789"), "must start with +"),
                arguments(named("00 prefix instead of +", "00420123456789"), "must start with +"),
                arguments(named("only + sign", "+"), "must contain at least one digit"),
                arguments(named("+ and only spaces", "+   "), "must contain at least one digit"),
                arguments(named("letters", "+420abc456789"), "must contain only digits"),
                arguments(named("letters with spaces", "+420 123 abc 789"), "must contain only digits"),
                arguments(named("hyphens", "+420-123-456-789"), "must contain only digits"),
                arguments(named("dots", "+420.123.456.789"), "must contain only digits"),
                arguments(named("parentheses", "(+420) 123 456 789"), "must start with +"),
                arguments(named("leading double +", "++420123456789"), "must contain only digits"),
                arguments(named("+ inside number", "+420+123456789"), "must contain only digits")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPhones")
    @DisplayName("Should reject invalid phone number")
    void shouldRejectInvalidPhoneNumber(String phone, String expectedMessage) {
        assertThatThrownBy(() -> PhoneNumber.of(phone))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "+420 123 456 789",
            "+1 555 123 4567",
            "+44 20 7946 0958",
            "+49 30 12345678",
            "+1 800 555 1234",
            "+15551234567",
            "+442079460958",
            "+493012345678",
            "+33123456789"
    })
    @DisplayName("Should accept valid international formats")
    void shouldAcceptValidInternationalFormats(String phone) {
        assertThat(PhoneNumber.of(phone).value()).isEqualTo(phone);
    }

    @Test
    @DisplayName("Should trim whitespace from phone")
    void shouldTrimWhitespaceFromPhone() {
        PhoneNumber phoneNumber = PhoneNumber.of("  +420 123 456 789  ");
        assertThat(phoneNumber.value()).isEqualTo("+420 123 456 789");
    }
}
