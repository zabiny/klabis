package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Nationality Value Object Tests")
class NationalityTest {

    @Test
    @DisplayName("Should create valid nationality with alpha-2 code")
    void shouldCreateValidAlpha2Nationality() {
        // Given
        String code = "CZ";

        // When
        Nationality nationality = Nationality.of(code);

        // Then
        assertThat(nationality.code()).isEqualTo("CZ");
    }

    @Test
    @DisplayName("Should have proper toString")
    void shouldHaveProperToString() {
        // Given
        Nationality nationality = Nationality.of("CZ");

        // When
        String str = nationality.toString();

        // Then
        assertThat(str).isEqualTo("CZ");
    }

    @Test
    @DisplayName("Should identify Czech nationality by CZ only")
    void shouldIdentifyCzechNationalityByAlpha2Only() {
        assertThat(Nationality.of("CZ").isCzech()).isTrue();
        assertThat(Nationality.of("SK").isCzech()).isFalse();
        assertThat(Nationality.of("US").isCzech()).isFalse();
    }

    @Test
    @DisplayName("Should wrap a Country")
    void shouldWrapCountry() {
        assertThat(Nationality.of("cz").country()).isEqualTo(Country.of("CZ"));
        assertThat(new Nationality(Country.of("SK")).code()).isEqualTo("SK");
    }

    @Test
    @DisplayName("Should refuse missing country with IllegalArgumentException")
    void shouldRefuseMissingCountry() {
        assertThatThrownBy(() -> new Nationality(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("Should convert null-tolerantly to and from code")
    void shouldConvertNullTolerantly() {
        assertThat(Nationality.ofNullable(null)).isNull();
        assertThat(Nationality.ofNullable("sk")).isEqualTo(Nationality.of("SK"));
        assertThat(Nationality.codeOf(null)).isNull();
        assertThat(Nationality.codeOf(Nationality.of("SK"))).isEqualTo("SK");
    }
}
