package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Address Value Object Tests")
class AddressTest {

    private static Address address(String street, String city, String postalCode) {
        return Address.of(street, city, postalCode, Country.of("CZ"));
    }

    @Test
    @DisplayName("Should create valid address with all fields")
    void shouldCreateValidAddress() {
        Address address = Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ"));

        assertThat(address.street()).isEqualTo("Hlavní 123");
        assertThat(address.city()).isEqualTo("Praha");
        assertThat(address.postalCode()).isEqualTo("11000");
        assertThat(address.country()).isEqualTo(Country.of("CZ"));
    }

    static Stream<Arguments> invalidAddresses() {
        return Stream.of(
                arguments(named("street null", (Runnable) () -> address(null, "Praha", "11000")), "Street is required"),
                arguments(named("street blank", (Runnable) () -> address("   ", "Praha", "11000")), "Street is required"),
                arguments(named("street over 200 characters", (Runnable) () -> address("a".repeat(201), "Praha", "11000")),
                        "Street must not exceed 200 characters"),
                arguments(named("city null", (Runnable) () -> address("Hlavní 123", null, "11000")), "City is required"),
                arguments(named("city blank", (Runnable) () -> address("Hlavní 123", "   ", "11000")), "City is required"),
                arguments(named("city over 100 characters", (Runnable) () -> address("Hlavní 123", "a".repeat(101), "11000")),
                        "City must not exceed 100 characters"),
                arguments(named("postal code null", (Runnable) () -> address("Hlavní 123", "Praha", null)),
                        "Postal code is required"),
                arguments(named("postal code blank", (Runnable) () -> address("Hlavní 123", "Praha", "   ")),
                        "Postal code is required"),
                arguments(named("postal code over 20 characters", (Runnable) () -> address("Hlavní 123", "Praha", "a".repeat(21))),
                        "Postal code must not exceed 20 characters"),
                arguments(named("postal code with @", (Runnable) () -> address("Hlavní 123", "Praha", "110@00")),
                        "Postal code must be alphanumeric"),
                arguments(named("postal code with !", (Runnable) () -> address("Hlavní 123", "Praha", "110!00")),
                        "Postal code must be alphanumeric"),
                arguments(named("country null", (Runnable) () -> Address.of("Hlavní 123", "Praha", "11000", null)),
                        "Country is required")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAddresses")
    @DisplayName("Should reject invalid address")
    void shouldRejectInvalidAddress(Runnable creation, String expectedMessage) {
        assertThatThrownBy(creation::run)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    static Stream<Arguments> validBoundaryAddresses() {
        return Stream.of(
                arguments(named("street at 200 characters", (Runnable) () -> address("a".repeat(200), "Praha", "11000"))),
                arguments(named("city at 100 characters", (Runnable) () -> address("Hlavní 123", "a".repeat(100), "11000"))),
                arguments(named("postal code at 20 characters", (Runnable) () -> address("Hlavní 123", "Praha", "a".repeat(20)))),
                arguments(named("postal code with hyphen", (Runnable) () -> address("Hlavní 123", "Praha", "110-00"))),
                arguments(named("postal code with space", (Runnable) () -> address("Hlavní 123", "Praha", "110 00")))
        );
    }

    @ParameterizedTest
    @MethodSource("validBoundaryAddresses")
    @DisplayName("Should accept valid boundary values")
    void shouldAcceptValidBoundaryValues(Runnable creation) {
        assertThatCode(creation::run).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should trim whitespace from all fields")
    void shouldTrimWhitespaceFromAllFields() {
        Address address = Address.of("  Hlavní 123  ", "  Praha  ", "  11000  ", Country.of("  CZ  "));
        assertThat(address.street()).isEqualTo("Hlavní 123");
        assertThat(address.city()).isEqualTo("Praha");
        assertThat(address.postalCode()).isEqualTo("11000");
        assertThat(address.country().code()).isEqualTo("CZ");
    }
}
