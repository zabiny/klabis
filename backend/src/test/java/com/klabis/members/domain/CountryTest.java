package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Country Value Object Tests")
class CountryTest {

    @Test
    @DisplayName("Should create country from valid two-letter code")
    void shouldCreateFromValidCode() {
        assertThat(Country.of("CZ").code()).isEqualTo("CZ");
    }

    @Test
    @DisplayName("Should normalize lower-case and padded code to upper case")
    void shouldNormalizeToUpperCase() {
        assertThat(Country.of("cz").code()).isEqualTo("CZ");
        assertThat(Country.of("  sk ").code()).isEqualTo("SK");
    }

    @Test
    @DisplayName("Should treat codes equal after normalization")
    void shouldBeEqualAfterNormalization() {
        assertThat(Country.of("cz")).isEqualTo(Country.of("CZ"));
        assertThat(Country.of("CZ")).isNotEqualTo(Country.of("SK"));
    }

    @Test
    @DisplayName("Should reject null and blank code")
    void shouldRejectNullAndBlank() {
        assertThatThrownBy(() -> Country.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country is required");
        assertThatThrownBy(() -> Country.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country is required");
    }

    @Test
    @DisplayName("Should reject malformed codes")
    void shouldRejectMalformedCodes() {
        for (String code : new String[]{"C", "CZE", "C1", "12", "C-", "C Z"}) {
            assertThatThrownBy(() -> Country.of(code))
                    .as(code)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ISO 3166-1 alpha-2");
        }
    }

    @Test
    @DisplayName("toString returns the code")
    void toStringReturnsCode() {
        assertThat(Country.of("cz").toString()).isEqualTo("CZ");
    }
}
