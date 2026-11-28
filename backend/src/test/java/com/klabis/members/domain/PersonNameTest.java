package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PersonName Value Object Tests")
class PersonNameTest {

    @Test
    @DisplayName("Should create valid person name")
    void shouldCreateValidPersonName() {
        // Given
        String firstName = "Jan";
        String lastName = "Novák";

        // When
        PersonName name = PersonName.of(firstName, lastName);

        // Then
        assertThat(name.firstName()).isEqualTo(firstName);
        assertThat(name.lastName()).isEqualTo(lastName);
    }

    @Test
    @DisplayName("Should trim whitespace from names")
    void shouldTrimWhitespace() {
        // Given
        String firstName = "  Jan  ";
        String lastName = "  Novák  ";

        // When
        PersonName name = PersonName.of(firstName, lastName);

        // Then
        assertThat(name.firstName()).isEqualTo("Jan");
        assertThat(name.lastName()).isEqualTo("Novák");
    }

    @Test
    @DisplayName("Should throw exception when first name is null")
    void shouldThrowWhenFirstNameIsNull() {
        assertThatThrownBy(() -> PersonName.of(null, "Novák"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First name");
    }

    @Test
    @DisplayName("Should throw exception when first name is blank")
    void shouldThrowWhenFirstNameIsBlank() {
        assertThatThrownBy(() -> PersonName.of("   ", "Novák"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First name")
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("Should throw exception when last name is null")
    void shouldThrowWhenLastNameIsNull() {
        assertThatThrownBy(() -> PersonName.of("Jan", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Last name");
    }

    @Test
    @DisplayName("Should throw exception when last name is blank")
    void shouldThrowWhenLastNameIsBlank() {
        assertThatThrownBy(() -> PersonName.of("Jan", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Last name")
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("Should return full name with single space")
    void shouldReturnFullName() {
        // Given
        PersonName name = PersonName.of("Jan", "Novák");

        // When
        String fullName = name.fullName();

        // Then
        assertThat(fullName).isEqualTo("Jan Novák");
    }

    @Test
    @DisplayName("Should handle names with multiple spaces in fullName")
    void shouldHandleMultipleSpacesInFullName() {
        // Given - PersonName will trim the input, but fullName() should still handle edge cases
        PersonName name = PersonName.of("Jan", "Novák");

        // When
        String fullName = name.fullName();

        // Then
        assertThat(fullName).doesNotContain("  ");
        assertThat(fullName).doesNotStartWith(" ");
        assertThat(fullName).doesNotEndWith(" ");
    }

    @Test
    @DisplayName("Should have proper toString")
    void shouldHaveProperToString() {
        // Given
        PersonName name = PersonName.of("Jan", "Novák");

        // When
        String str = name.toString();

        // Then
        assertThat(str).isEqualTo("Jan Novák");
    }

    @Test
    @DisplayName("Should handle names with diacritics")
    void shouldHandleNamesWithDiacritics() {
        // Given
        PersonName name = PersonName.of("François", "Müller");

        // When & Then
        assertThat(name.firstName()).isEqualTo("François");
        assertThat(name.lastName()).isEqualTo("Müller");
        assertThat(name.fullName()).isEqualTo("François Müller");
    }

    @Test
    @DisplayName("Should handle compound last names")
    void shouldHandleCompoundLastNames() {
        // Given
        PersonName name = PersonName.of("Jean", "de La Fontaine");

        // When
        String fullName = name.fullName();

        // Then
        assertThat(fullName).isEqualTo("Jean de La Fontaine");
    }

    @Test
    @DisplayName("Should handle single character names")
    void shouldHandleSingleCharacterNames() {
        // Given
        PersonName name = PersonName.of("J", "N");

        // When & Then
        assertThat(name.firstName()).isEqualTo("J");
        assertThat(name.lastName()).isEqualTo("N");
    }

    @Test
    @DisplayName("Should handle very long names")
    void shouldHandleVeryLongNames() {
        // Given
        String longFirstName = "A".repeat(100);
        String longLastName = "B".repeat(100);

        // When
        PersonName name = PersonName.of(longFirstName, longLastName);

        // Then
        assertThat(name.firstName()).hasSize(100);
        assertThat(name.lastName()).hasSize(100);
    }
}
