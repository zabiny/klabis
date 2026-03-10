package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BirthNumber Value Object Tests")
class BirthNumberTest extends ValueObjectTestBase<BirthNumber> {

    @Override
    protected BirthNumber createValidValue() {
        return BirthNumber.of("900101/1235");  // 9001011235 % 11 = 0
    }

    @Override
    protected BirthNumber createDifferentValue() {
        return BirthNumber.of("851231/5679");  // 8512315679 % 11 = 0
    }

    @Override
    protected String expectedToString() {
        return "900101/1235";
    }

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

        @Test
        @DisplayName("Should throw exception when birth number is null")
        void shouldThrowWhenBirthNumberIsNull() {
            assertThatThrownBy(() -> BirthNumber.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Birth number is required");
        }

        @Test
        @DisplayName("Should throw exception when birth number is blank")
        void shouldThrowWhenBirthNumberIsBlank() {
            assertThatThrownBy(() -> BirthNumber.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Birth number is required");
        }

        @Test
        @DisplayName("Should throw exception when birth number is too short")
        void shouldThrowWhenBirthNumberIsTooShort() {
            assertThatThrownBy(() -> BirthNumber.of("90101/123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid birth number format");
        }

        @Test
        @DisplayName("Should throw exception when birth number is too long")
        void shouldThrowWhenBirthNumberIsTooLong() {
            assertThatThrownBy(() -> BirthNumber.of("90010112/345"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid birth number format");
        }

        @Test
        @DisplayName("Should throw exception when birth number contains invalid characters")
        void shouldThrowWhenBirthNumberContainsInvalidCharacters() {
            assertThatThrownBy(() -> BirthNumber.of("90O1O1/1234"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid birth number format");
        }

        @Test
        @DisplayName("Should throw exception when month is invalid")
        void shouldThrowWhenMonthIsInvalid() {
            assertThatThrownBy(() -> BirthNumber.of("901301/1235"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid month");

            // month 83 is outside all valid ranges (01-12, 21-32, 51-62, 71-82)
            assertThatThrownBy(() -> BirthNumber.of("908301/1236"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid month");
        }

        @Test
        @DisplayName("Should throw exception when day is invalid")
        void shouldThrowWhenDayIsInvalid() {
            assertThatThrownBy(() -> BirthNumber.of("900100/1237"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid day");

            assertThatThrownBy(() -> BirthNumber.of("900132/1238"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid day");
        }

        @Test
        @DisplayName("Should throw exception when slash is in wrong position")
        void shouldThrowWhenSlashIsInWrongPosition() {
            assertThatThrownBy(() -> BirthNumber.of("90010/11235"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid birth number format");

            assertThatThrownBy(() -> BirthNumber.of("9001011/236"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid birth number format");
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
