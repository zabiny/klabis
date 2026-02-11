package com.klabis.members;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD Test for RegistrationNumberGenerator domain service.
 * <p>
 * Tests registration number generation logic:
 * - Format: XXXYYDD (club code + birth year + sequence)
 * - Sequence numbers start at 01 for each birth year
 * - Sequence numbers increment for same birth year
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationNumberGenerator Domain Service")
class RegistrationNumberGeneratorTest {

    @Mock
    private Members membersMock;

    private RegistrationNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RegistrationNumberGenerator("ZBM", membersMock);
    }

    @Test
    @DisplayName("should generate registration number with sequence 01 for first member of birth year")
    void shouldGenerateRegistrationNumberWithSequence01ForFirstMember() {
        // Arrange
        LocalDate dateOfBirth = LocalDate.of(2005, 3, 15);
        when(membersMock.countByBirthYear(2005)).thenReturn(0);

        // Act
        RegistrationNumber regNumber = generator.generate(dateOfBirth);

        // Assert
        assertThat(regNumber.getValue()).isEqualTo("ZBM0501");
        assertThat(regNumber.getClubCode()).isEqualTo("ZBM");
        assertThat(regNumber.getBirthYear()).isEqualTo(5);
        assertThat(regNumber.getSequenceNumber()).isEqualTo(1);
        verify(membersMock).countByBirthYear(2005);
    }

    @Test
    @DisplayName("should generate registration number with incremented sequence for second member")
    void shouldGenerateRegistrationNumberWithIncrementedSequence() {
        // Arrange
        LocalDate dateOfBirth = LocalDate.of(2005, 8, 22);
        when(membersMock.countByBirthYear(2005)).thenReturn(1);

        // Act
        RegistrationNumber regNumber = generator.generate(dateOfBirth);

        // Assert
        assertThat(regNumber.getValue()).isEqualTo("ZBM0502");
        assertThat(regNumber.getSequenceNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("should handle different birth years independently")
    void shouldHandleDifferentBirthYearsIndependently() {
        // Arrange
        LocalDate dateOfBirth2004 = LocalDate.of(2004, 5, 10);
        LocalDate dateOfBirth2005 = LocalDate.of(2005, 5, 10);

        when(membersMock.countByBirthYear(2004)).thenReturn(5);
        when(membersMock.countByBirthYear(2005)).thenReturn(0);

        // Act
        RegistrationNumber regNumber2004 = generator.generate(dateOfBirth2004);
        RegistrationNumber regNumber2005 = generator.generate(dateOfBirth2005);

        // Assert
        assertThat(regNumber2004.getValue()).isEqualTo("ZBM0406");
        assertThat(regNumber2005.getValue()).isEqualTo("ZBM0501");
    }

    @Test
    @DisplayName("should handle year 2000+ correctly")
    void shouldHandleYear2000PlusCorrectly() {
        // Arrange
        LocalDate dateOfBirth = LocalDate.of(2023, 1, 1);
        when(membersMock.countByBirthYear(2023)).thenReturn(0);

        // Act
        RegistrationNumber regNumber = generator.generate(dateOfBirth);

        // Assert
        assertThat(regNumber.getValue()).isEqualTo("ZBM2301");
        assertThat(regNumber.getBirthYear()).isEqualTo(23);
    }

    @Test
    @DisplayName("should handle year 1999 and below correctly")
    void shouldHandleYear1999AndBelowCorrectly() {
        // Arrange
        LocalDate dateOfBirth = LocalDate.of(1995, 6, 15);
        when(membersMock.countByBirthYear(1995)).thenReturn(0);

        // Act
        RegistrationNumber regNumber = generator.generate(dateOfBirth);

        // Assert
        assertThat(regNumber.getValue()).isEqualTo("ZBM9501");
        assertThat(regNumber.getBirthYear()).isEqualTo(95);
    }

    @Test
    @DisplayName("should handle sequence numbers up to 99")
    void shouldHandleSequenceNumbersUpTo99() {
        // Arrange
        LocalDate dateOfBirth = LocalDate.of(2010, 4, 20);
        when(membersMock.countByBirthYear(2010)).thenReturn(98);

        // Act
        RegistrationNumber regNumber = generator.generate(dateOfBirth);

        // Assert
        assertThat(regNumber.getValue()).isEqualTo("ZBM1099");
        assertThat(regNumber.getSequenceNumber()).isEqualTo(99);
    }

    @Test
    @DisplayName("should fail when sequence number exceeds 99")
    void shouldFailWhenSequenceNumberExceeds99() {
        // Arrange
        LocalDate dateOfBirth = LocalDate.of(2010, 4, 20);
        when(membersMock.countByBirthYear(2010)).thenReturn(99);

        // Act & Assert
        assertThatThrownBy(() -> generator.generate(dateOfBirth))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot generate registration number")
                .hasMessageContaining("maximum sequence number");
    }

    @Test
    @DisplayName("should fail when date of birth is null")
    void shouldFailWhenDateOfBirthIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> generator.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date of birth is required");
    }

    @Test
    @DisplayName("should use configured club code")
    void shouldUseConfiguredClubCode() {
        // Arrange
        RegistrationNumberGenerator customGenerator =
                new RegistrationNumberGenerator("ABC", membersMock);
        LocalDate dateOfBirth = LocalDate.of(2005, 3, 15);
        when(membersMock.countByBirthYear(2005)).thenReturn(0);

        // Act
        RegistrationNumber regNumber = customGenerator.generate(dateOfBirth);

        // Assert
        assertThat(regNumber.getValue()).isEqualTo("ABC0501");
        assertThat(regNumber.getClubCode()).isEqualTo("ABC");
    }

    @Test
    @DisplayName("should fail when club code is null")
    void shouldFailWhenClubCodeIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new RegistrationNumberGenerator(null, membersMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Club code is required");
    }

    @Test
    @DisplayName("should fail when club code is blank")
    void shouldFailWhenClubCodeIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> new RegistrationNumberGenerator("", membersMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Club code is required");
    }

    @Test
    @DisplayName("should fail when club code is not exactly 3 characters")
    void shouldFailWhenClubCodeIsNot3Characters() {
        // Act & Assert
        assertThatThrownBy(() -> new RegistrationNumberGenerator("AB", membersMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Club code must be exactly 3 characters");

        assertThatThrownBy(() -> new RegistrationNumberGenerator("ABCD", membersMock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Club code must be exactly 3 characters");
    }
}
