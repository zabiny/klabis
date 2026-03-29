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
    @DisplayName("Should reject alpha-3 code")
    void shouldRejectAlpha3Code() {
        assertThatThrownBy(() -> Nationality.of("CZE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
    }

    @Test
    @DisplayName("Should normalize lowercase code to uppercase")
    void shouldNormalizeLowercaseToUppercase() {
        // Given
        String code = "cz";

        // When
        Nationality nationality = Nationality.of(code);

        // Then
        assertThat(nationality.code()).isEqualTo("CZ");
    }

    @Test
    @DisplayName("Should normalize mixed case code to uppercase")
    void shouldNormalizeMixedCaseToUppercase() {
        // Given
        String code = "Cz";

        // When
        Nationality nationality = Nationality.of(code);

        // Then
        assertThat(nationality.code()).isEqualTo("CZ");
    }

    @Test
    @DisplayName("Should trim whitespace from code")
    void shouldTrimWhitespace() {
        // Given
        String code = "  CZ  ";

        // When
        Nationality nationality = Nationality.of(code);

        // Then
        assertThat(nationality.code()).isEqualTo("CZ");
    }

    @Test
    @DisplayName("Should throw exception when code is null")
    void shouldThrowWhenCodeIsNull() {
        assertThatThrownBy(() -> Nationality.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("Should throw exception when code is blank")
    void shouldThrowWhenCodeIsBlank() {
        assertThatThrownBy(() -> Nationality.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("Should throw exception when code is too short")
    void shouldThrowWhenCodeIsTooShort() {
        assertThatThrownBy(() -> Nationality.of("C"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
    }

    @Test
    @DisplayName("Should throw exception when code is too long")
    void shouldThrowWhenCodeIsTooLong() {
        assertThatThrownBy(() -> Nationality.of("CZE1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
    }

    @Test
    @DisplayName("Should throw exception when code contains numbers")
    void shouldThrowWhenCodeContainsNumbers() {
        assertThatThrownBy(() -> Nationality.of("C1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");

        assertThatThrownBy(() -> Nationality.of("C21"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
    }

    @Test
    @DisplayName("Should throw exception when code contains special characters")
    void shouldThrowWhenCodeContainsSpecialCharacters() {
        assertThatThrownBy(() -> Nationality.of("C-E"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");

        assertThatThrownBy(() -> Nationality.of("C E"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");

        assertThatThrownBy(() -> Nationality.of("C/E"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
    }

    @Test
    @DisplayName("Should implement equality correctly")
    void shouldImplementEqualityCorrectly() {
        // Given
        Nationality nationality1 = Nationality.of("CZ");
        Nationality nationality2 = Nationality.of("CZ");
        Nationality nationality3 = Nationality.of("US");
        Nationality nationality4 = Nationality.of("cz"); // same after normalization

        // Then
        assertThat(nationality1).isEqualTo(nationality2);
        assertThat(nationality1).isEqualTo(nationality4); // same after normalization
        assertThat(nationality1).isNotEqualTo(nationality3);
        assertThat(nationality1.hashCode()).isEqualTo(nationality2.hashCode());
        assertThat(nationality1.hashCode()).isEqualTo(nationality4.hashCode());
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
    @DisplayName("Should return display name")
    void shouldReturnDisplayName() {
        // Given
        Nationality nationality = Nationality.of("CZ");

        // When
        String displayName = nationality.displayName();

        // Then
        assertThat(displayName).isEqualTo("CZ");
    }

    @Test
    @DisplayName("Should accept common alpha-2 codes")
    void shouldAcceptCommonAlpha2Codes() {
        assertThatCode(() -> Nationality.of("US")).doesNotThrowAnyException();
        assertThatCode(() -> Nationality.of("GB")).doesNotThrowAnyException();
        assertThatCode(() -> Nationality.of("DE")).doesNotThrowAnyException();
        assertThatCode(() -> Nationality.of("FR")).doesNotThrowAnyException();
        assertThatCode(() -> Nationality.of("SK")).doesNotThrowAnyException();
        assertThatCode(() -> Nationality.of("PL")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject alpha-3 codes")
    void shouldRejectAlpha3Codes() {
        assertThatThrownBy(() -> Nationality.of("USA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
        assertThatThrownBy(() -> Nationality.of("GBR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
        assertThatThrownBy(() -> Nationality.of("POL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 3166-1");
    }

    @Test
    @DisplayName("Should be immutable as a record")
    void shouldBeImmutable() {
        // Given
        Nationality nationality = Nationality.of("CZ");

        // When & Then - records are immutable by design
        assertThat(nationality.code()).isEqualTo("CZ");
        // No setters available
    }

    @Test
    @DisplayName("Should identify Czech nationality by CZ only")
    void shouldIdentifyCzechNationalityByAlpha2Only() {
        assertThat(Nationality.of("CZ").isCzech()).isTrue();
        assertThat(Nationality.of("SK").isCzech()).isFalse();
        assertThat(Nationality.of("US").isCzech()).isFalse();
    }
}
