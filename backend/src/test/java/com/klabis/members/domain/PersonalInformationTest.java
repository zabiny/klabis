package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PersonalInformation Value Object Tests")
class PersonalInformationTest {

    @Nested
    @DisplayName("of() factory method - validation")
    class OfFactoryMethodValidation {

        @Test
        @DisplayName("Should create valid PersonalInformation")
        void shouldCreateValidPersonalInformation() {
            // Given
            String firstName = "Jan";
            String lastName = "Novák";
            LocalDate dateOfBirth = LocalDate.of(2005, 3, 15);
            Nationality nationality = Nationality.of("CZ");
            Gender gender = Gender.MALE;

            // When
            PersonalInformation info = PersonalInformation.of(
                    firstName, lastName, dateOfBirth, nationality, gender
            );

            // Then
            assertThat(info.getFirstName()).isEqualTo(firstName);
            assertThat(info.getLastName()).isEqualTo(lastName);
            assertThat(info.getDateOfBirth()).isEqualTo(dateOfBirth);
            assertThat(info.getNationality()).isEqualTo(nationality);
            assertThat(info.getGender()).isEqualTo(gender);
        }

        @Test
        @DisplayName("Should throw exception when date of birth is null")
        void shouldThrowWhenDateOfBirthIsNull() {
            assertThatThrownBy(() ->
                    PersonalInformation.of(
                            "Jan", "Novák", null, Nationality.of("CZ"), Gender.MALE
                    )
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Date of birth");
        }

        @Test
        @DisplayName("Should throw exception when date of birth is in the future")
        void shouldThrowWhenDateOfBirthIsInFuture() {
            assertThatThrownBy(() ->
                    PersonalInformation.of(
                            "Jan", "Novák", LocalDate.now().plusDays(1), Nationality.of("CZ"), Gender.MALE
                    )
            ).isInstanceOf(DateOfBirthInFutureException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("Should throw exception when nationality is null")
        void shouldThrowWhenNationalityIsNull() {
            assertThatThrownBy(() ->
                    PersonalInformation.of(
                            "Jan", "Novák", LocalDate.of(2005, 3, 15), null, Gender.MALE
                    )
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nationality is required");
        }

        @Test
        @DisplayName("Should throw exception when gender is null")
        void shouldThrowWhenGenderIsNull() {
            assertThatThrownBy(() ->
                    PersonalInformation.of(
                            "Jan", "Novák", LocalDate.of(2005, 3, 15), Nationality.of("CZ"), null
                    )
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Gender");
        }
    }

    @Nested
    @DisplayName("getAge() method")
    class GetAgeMethod {

        @Test
        @DisplayName("Should calculate age correctly")
        void shouldCalculateAgeCorrectly() {
            // Given
            LocalDate birthDate = LocalDate.of(2005, 3, 15);
            PersonalInformation info = PersonalInformation.of(
                    "Jan", "Novák", birthDate, Nationality.of("CZ"), Gender.MALE
            );

            // When
            int age = info.getAge();

            // Then
            int expectedAge = Period.between(birthDate, LocalDate.now()).getYears();
            assertThat(age).isEqualTo(expectedAge);
        }
    }

    @Nested
    @DisplayName("isMinor() method")
    class IsMinorMethod {

        @Test
        @DisplayName("Should identify minor correctly")
        void shouldIdentifyMinorCorrectly() {
            // Given - person born 15 years ago
            PersonalInformation minor = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.now().minusYears(15), Nationality.of("CZ"), Gender.MALE
            );

            // Then
            assertThat(minor.isMinor()).isTrue();
            assertThat(minor.getAge()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should identify adult correctly")
        void shouldIdentifyAdultCorrectly() {
            // Given - person born 20 years ago
            PersonalInformation adult = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.now().minusYears(20), Nationality.of("CZ"), Gender.MALE
            );

            // Then
            assertThat(adult.isMinor()).isFalse();
            assertThat(adult.getAge()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should identify exactly 18 as adult")
        void shouldIdentifyExactly18AsAdult() {
            // Given - person born exactly 18 years ago
            PersonalInformation adult = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.now().minusYears(18), Nationality.of("CZ"), Gender.MALE
            );

            // Then
            assertThat(adult.isMinor()).isFalse();
            assertThat(adult.getAge()).isGreaterThanOrEqualTo(18);
        }
    }

    @Nested
    @DisplayName("getFullName() method")
    class GetFullNameMethod {

        @Test
        @DisplayName("Should return full name")
        void shouldReturnFullName() {
            // Given
            PersonalInformation info = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(2005, 3, 15), Nationality.of("CZ"), Gender.MALE
            );

            // When
            String fullName = info.getFullName();

            // Then
            assertThat(fullName).isEqualTo("Jan Novák");
        }

        @Test
        @DisplayName("Should trim whitespace from full name")
        void shouldTrimWhitespaceFromFullName() {
            // Given - note: PersonalInformation validates that names are not blank,
            // but we should still handle any edge cases in getFullName()
            PersonalInformation info = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(2005, 3, 15), Nationality.of("CZ"), Gender.MALE
            );

            // When
            String fullName = info.getFullName();

            // Then - no leading/trailing spaces, single space between names
            assertThat(fullName).doesNotStartWith(" ");
            assertThat(fullName).doesNotEndWith(" ");
            assertThat(fullName).doesNotContain("  ");
        }
    }

    @Nested
    @DisplayName("Object methods (equals, hashCode, toString)")
    class ObjectMethods {

        @Test
        @DisplayName("Should implement equality correctly")
        void shouldImplementEqualityCorrectly() {
            // Given
            PersonalInformation info1 = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(2005, 3, 15), Nationality.of("CZ"), Gender.MALE
            );
            PersonalInformation info2 = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(2005, 3, 15), Nationality.of("CZ"), Gender.MALE
            );
            PersonalInformation info3 = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(2005, 3, 16), Nationality.of("CZ"), Gender.MALE
            );

            // Then
            assertThat(info1).isEqualTo(info2);
            assertThat(info1).isNotEqualTo(info3);
            assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
        }

        @Test
        @DisplayName("Should have proper toString")
        void shouldHaveProperToString() {
            // Given
            PersonalInformation info = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(2005, 3, 15), Nationality.of("CZ"), Gender.MALE
            );

            // When
            String str = info.toString();

            // Then
            assertThat(str)
                    .contains("Jan")
                    .contains("Novák")
                    .contains("2005-03-15")
                    .contains("CZ")
                    .contains("MALE");
        }
    }
}
