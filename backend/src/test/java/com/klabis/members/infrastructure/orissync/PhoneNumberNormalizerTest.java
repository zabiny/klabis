package com.klabis.members.infrastructure.orissync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PhoneNumberNormalizer")
class PhoneNumberNormalizerTest {

    @Test
    @DisplayName("a bare CZ national number gains the +420 prefix")
    void bareCzNumberGainsPrefix() {
        String normalized = PhoneNumberNormalizer.normalize("700000001", "CZ");

        assertThat(normalized).isEqualTo("+420700000001");
    }

    @Test
    @DisplayName("a number already starting with + is left untouched")
    void numberWithPlusIsUntouched() {
        String normalized = PhoneNumberNormalizer.normalize("+420700000001", "CZ");

        assertThat(normalized).isEqualTo("+420700000001");
    }

    @Test
    @DisplayName("a number already starting with + is left untouched regardless of country")
    void numberWithPlusIsUntouchedForOtherCountry() {
        String normalized = PhoneNumberNormalizer.normalize("+1234567890", "US");

        assertThat(normalized).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("a bare number for an unhandled country cannot be normalised confidently and becomes null")
    void bareNumberForUnknownCountryBecomesNull() {
        String normalized = PhoneNumberNormalizer.normalize("700000001", "US");

        assertThat(normalized).isNull();
    }

    @Test
    @DisplayName("a bare number with no country context becomes null")
    void bareNumberWithNoCountryBecomesNull() {
        String normalized = PhoneNumberNormalizer.normalize("700000001", null);

        assertThat(normalized).isNull();
    }

    @Test
    @DisplayName("a blank number maps to null")
    void blankNumberMapsToNull() {
        String normalized = PhoneNumberNormalizer.normalize("  ", "CZ");

        assertThat(normalized).isNull();
    }

    @Test
    @DisplayName("a null number maps to null")
    void nullNumberMapsToNull() {
        String normalized = PhoneNumberNormalizer.normalize(null, "CZ");

        assertThat(normalized).isNull();
    }
}
