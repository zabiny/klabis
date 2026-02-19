package com.klabis.members;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

            assertThatThrownBy(() -> BirthNumber.of("907101/1236"))
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
}
