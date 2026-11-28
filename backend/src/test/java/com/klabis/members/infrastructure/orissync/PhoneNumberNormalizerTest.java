package com.klabis.members.infrastructure.orissync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("PhoneNumberNormalizer")
class PhoneNumberNormalizerTest {

    static Stream<Arguments> normalizations() {
        return Stream.of(
                arguments(named("a bare CZ national number gains the +420 prefix", "700000001"), "CZ", "+420700000001"),
                arguments(named("a number already starting with + is left untouched", "+420700000001"), "CZ", "+420700000001"),
                arguments(named("a number already starting with + is left untouched regardless of country", "+1234567890"), "US", "+1234567890"),
                arguments(named("a bare number for an unhandled country becomes null", "700000001"), "US", null),
                arguments(named("a bare number with no country context becomes null", "700000001"), null, null),
                arguments(named("a blank number maps to null", "  "), "CZ", null),
                arguments(named("a null number maps to null", (String) null), "CZ", null)
        );
    }

    @ParameterizedTest(name = "{0} ({1}) -> {2}")
    @MethodSource("normalizations")
    void normalizesPhoneNumber(String number, String country, String expected) {
        assertThat(PhoneNumberNormalizer.normalize(number, country)).isEqualTo(expected);
    }
}
