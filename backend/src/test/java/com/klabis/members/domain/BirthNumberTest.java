package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("BirthNumber Value Object Tests")
class BirthNumberTest {

    @Nested
    @DisplayName("Valid Format Acceptance")
    class ValidFormatAcceptance {

        @Test
        @DisplayName("Should accept birth number with slash")
        void shouldAcceptBirthNumberWithSlash() {
            assertThatCode(() -> BirthNumber.of("900101/1235"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept birth number without slash and normalize it")
        void shouldAcceptBirthNumberWithoutSlash() {
            BirthNumber birthNumber = BirthNumber.of("9001011235");
            assertThat(birthNumber.value()).isEqualTo("900101/1235");
        }

        @Test
        @DisplayName("Should accept birth number for years 1900-1999 (00-99)")
        void shouldAcceptBirthNumberFor1900s() {
            assertThatCode(() -> BirthNumber.of("000101/1235")).doesNotThrowAnyException();
            assertThatCode(() -> BirthNumber.of("991231/1236")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept birth number for years 2000-2053 (00-53)")
        void shouldAcceptBirthNumberFor2000s() {
            assertThatCode(() -> BirthNumber.of("000101/1237")).doesNotThrowAnyException();
            assertThatCode(() -> BirthNumber.of("531231/1238")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept birth number for women (month 51-62)")
        void shouldAcceptBirthNumberForWomen() {
            assertThatCode(() -> BirthNumber.of("905101/1239")).doesNotThrowAnyException();
            assertThatCode(() -> BirthNumber.of("906201/1240")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept birth number for women 2000+ (month 21-32)")
        void shouldAcceptBirthNumberForWomen2000s() {
            assertThatCode(() -> BirthNumber.of("002201/1235")).doesNotThrowAnyException();
            assertThatCode(() -> BirthNumber.of("003201/5679")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid Format Rejection")
    class InvalidFormatRejection {

        static Stream<Arguments> invalidBirthNumbers() {
            return Stream.of(
                    arguments(named("null", (String) null), "Birth number is required"),
                    arguments(named("blank", "   "), "Birth number is required"),
                    arguments(named("too short", "90101/123"), "Invalid birth number format"),
                    arguments(named("too long", "90010112/345"), "Invalid birth number format"),
                    arguments(named("invalid characters", "90O1O1/1234"), "Invalid birth number format"),
                    arguments(named("slash after 5 digits", "90010/11235"), "Invalid birth number format"),
                    arguments(named("slash after 7 digits", "9001011/236"), "Invalid birth number format"),
                    arguments(named("month 13", "901301/1235"), "Invalid month"),
                    arguments(named("month 83 outside all valid ranges", "908301/1236"), "Invalid month"),
                    arguments(named("day 00", "900100/1237"), "Invalid day"),
                    arguments(named("day 32", "900132/1238"), "Invalid day")
            );
        }

        @ParameterizedTest
        @MethodSource("invalidBirthNumbers")
        @DisplayName("Should reject invalid birth number")
        void shouldRejectInvalidBirthNumber(String value, String expectedMessage) {
            assertThatThrownBy(() -> BirthNumber.of(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessage);
        }
    }

    @Nested
    @DisplayName("Value Access")
    class ValueAccess {

        @Test
        @DisplayName("Should return normalized value with slash")
        void shouldReturnNormalizedValue() {
            BirthNumber birthNumber = BirthNumber.of("9001011235");
            assertThat(birthNumber.value()).isEqualTo("900101/1235");
        }
    }

    @Nested
    @DisplayName("Date Extraction")
    class DateExtraction {

        @Test
        @DisplayName("Should extract date from male birth number (month 01-12)")
        void shouldExtractDateFromMaleBirthNumber() {
            // 900101/XXXX = year 90, month 01 (male), day 01
            BirthNumber birthNumber = BirthNumber.of("900101/1235");
            assertThat(birthNumber.extractDate(1990)).isEqualTo(java.time.LocalDate.of(1990, 1, 1));
        }

        @Test
        @DisplayName("Should extract date from female birth number (month 51-62)")
        void shouldExtractDateFromFemaleBirthNumber() {
            // 905101/XXXX = year 90, month 51 (female, actual month 01), day 01
            BirthNumber birthNumber = BirthNumber.of("905101/1239");
            assertThat(birthNumber.extractDate(1990)).isEqualTo(java.time.LocalDate.of(1990, 1, 1));
        }

        @Test
        @DisplayName("Should extract date from birth number with extra male range (month 21-32)")
        void shouldExtractDateFromExtraMaleRange() {
            // 002101/XXXX = year 00, month 21 (male extra range, actual month 01), day 01
            BirthNumber birthNumber = BirthNumber.of("002101/1235");
            assertThat(birthNumber.extractDate(2000)).isEqualTo(java.time.LocalDate.of(2000, 1, 1));
        }

        @Test
        @DisplayName("Should extract date from birth number with extra female range (month 71-82)")
        void shouldExtractDateFromExtraFemaleRange() {
            // 007115/XXXX = year 00, month 71 (female extra range, actual month 01), day 15
            BirthNumber birthNumber = BirthNumber.of("007115/1235");
            assertThat(birthNumber.extractDate(2000)).isEqualTo(java.time.LocalDate.of(2000, 1, 15));
        }
    }

    @Nested
    @DisplayName("Gender Indication")
    class GenderIndication {

        @Test
        @DisplayName("Should indicate MALE for month 01-12")
        void shouldIndicateMaleForMonth01to12() {
            assertThat(BirthNumber.of("900101/1235").indicatesGender()).isEqualTo(Gender.MALE);
            assertThat(BirthNumber.of("901201/1235").indicatesGender()).isEqualTo(Gender.MALE);
        }

        @Test
        @DisplayName("Should indicate FEMALE for month 51-62")
        void shouldIndicateFemaleForMonth51to62() {
            assertThat(BirthNumber.of("905101/1239").indicatesGender()).isEqualTo(Gender.FEMALE);
            assertThat(BirthNumber.of("906201/1240").indicatesGender()).isEqualTo(Gender.FEMALE);
        }

        @Test
        @DisplayName("Should indicate MALE for extra range month 21-32")
        void shouldIndicateMaleForMonth21to32() {
            assertThat(BirthNumber.of("002101/1235").indicatesGender()).isEqualTo(Gender.MALE);
            assertThat(BirthNumber.of("003201/5679").indicatesGender()).isEqualTo(Gender.MALE);
        }

        @Test
        @DisplayName("Should indicate FEMALE for extra range month 71-82")
        void shouldIndicateFemaleForMonth71to82() {
            assertThat(BirthNumber.of("007115/1235").indicatesGender()).isEqualTo(Gender.FEMALE);
        }
    }
}
