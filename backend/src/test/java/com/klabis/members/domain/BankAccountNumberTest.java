package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BankAccountNumber Value Object Tests")
class BankAccountNumberTest extends ValueObjectTestBase<BankAccountNumber> {

    @Override
    protected BankAccountNumber createValidValue() {
        return BankAccountNumber.of("CZ6508000000192000145399");
    }

    @Override
    protected BankAccountNumber createDifferentValue() {
        return BankAccountNumber.of("123456/0800");
    }

    @Override
    protected String expectedToString() {
        return "CZ6508000000192000145399";
    }

    @Nested
    @DisplayName("Valid IBAN Format Acceptance")
    class ValidIbanFormatAcceptance {

        @Test
        @DisplayName("Should accept valid Czech IBAN")
        void shouldAcceptValidCzechIban() {
            assertThatCode(() -> BankAccountNumber.of("CZ6508000000192000145399"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid IBAN with spaces and normalize it")
        void shouldAcceptValidIbanWithSpacesAndNormalizeIt() {
            BankAccountNumber bankAccount = BankAccountNumber.of("CZ65 0800 0000 1920 0014 5399");
            assertThat(bankAccount.value()).isEqualTo("CZ6508000000192000145399");
        }

        @Test
        @DisplayName("Should accept valid Slovak IBAN")
        void shouldAcceptValidSlovakIban() {
            assertThatCode(() -> BankAccountNumber.of("SK3112000000198742637541"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid German IBAN")
        void shouldAcceptValidGermanIban() {
            assertThatCode(() -> BankAccountNumber.of("DE89370400440532013000"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Valid Domestic Format Acceptance")
    class ValidDomesticFormatAcceptance {

        @Test
        @DisplayName("Should accept valid domestic format with prefix")
        void shouldAcceptValidDomesticFormatWithPrefix() {
            assertThatCode(() -> BankAccountNumber.of("123456/0800"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid domestic format with long account number")
        void shouldAcceptValidDomesticFormatWithLongAccountNumber() {
            assertThatCode(() -> BankAccountNumber.of("1234567890/0800"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid domestic format without prefix")
        void shouldAcceptValidDomesticFormatWithoutPrefix() {
            assertThatCode(() -> BankAccountNumber.of("123456789/0300"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid IBAN Format Rejection")
    class InvalidIbanFormatRejection {

        @Test
        @DisplayName("Should throw exception when IBAN checksum is invalid")
        void shouldThrowWhenIbanChecksumIsInvalid() {
            assertThatThrownBy(() -> BankAccountNumber.of("CZ6508000000192000145390"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid IBAN");
        }

        @Test
        @DisplayName("Should throw exception when IBAN is too short")
        void shouldThrowWhenIbanIsTooShort() {
            assertThatThrownBy(() -> BankAccountNumber.of("CZ123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid IBAN");
        }

        @Test
        @DisplayName("Should throw exception when IBAN country code is lowercase")
        void shouldThrowWhenIbanCountryCodeIsLowercase() {
            BankAccountNumber bankAccount = BankAccountNumber.of("cz6508000000192000145399");
            assertThat(bankAccount.value()).isEqualTo("CZ6508000000192000145399");
        }
    }

    @Nested
    @DisplayName("Invalid Domestic Format Rejection")
    class InvalidDomesticFormatRejection {

        @Test
        @DisplayName("Should throw exception when bank code is not 4 digits")
        void shouldThrowWhenBankCodeIsNot4Digits() {
            assertThatThrownBy(() -> BankAccountNumber.of("123456/080"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid domestic format");

            assertThatThrownBy(() -> BankAccountNumber.of("123456/08000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid domestic format");
        }

        @Test
        @DisplayName("Should throw exception when account number is too long")
        void shouldThrowWhenAccountNumberIsTooLong() {
            assertThatThrownBy(() -> BankAccountNumber.of("12345678901/0800"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid domestic format");
        }

        @Test
        @DisplayName("Should throw exception when domestic format has no slash")
        void shouldThrowWhenDomesticFormatHasNoSlash() {
            assertThatThrownBy(() -> BankAccountNumber.of("1234567890800"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot detect account format");
        }
    }

    @Nested
    @DisplayName("Null and Blank Validation")
    class NullAndBlankValidation {

        @Test
        @DisplayName("Should throw exception when bank account is null")
        void shouldThrowWhenBankAccountIsNull() {
            assertThatThrownBy(() -> BankAccountNumber.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank account number is required");
        }

        @Test
        @DisplayName("Should throw exception when bank account is blank")
        void shouldThrowWhenBankAccountIsBlank() {
            assertThatThrownBy(() -> BankAccountNumber.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank account number is required");
        }
    }

    @Nested
    @DisplayName("Format Detection")
    class FormatDetection {

        @Test
        @DisplayName("Should detect IBAN format")
        void shouldDetectIbanFormat() {
            BankAccountNumber bankAccount = BankAccountNumber.of("CZ6508000000192000145399");
            assertThat(bankAccount.format()).isEqualTo(BankAccountNumber.AccountFormat.IBAN);
        }

        @Test
        @DisplayName("Should detect domestic format")
        void shouldDetectDomesticFormat() {
            BankAccountNumber bankAccount = BankAccountNumber.of("123456/0800");
            assertThat(bankAccount.format()).isEqualTo(BankAccountNumber.AccountFormat.DOMESTIC);
        }
    }

    @Nested
    @DisplayName("Value Access")
    class ValueAccess {

        @Test
        @DisplayName("Should return normalized value")
        void shouldReturnNormalizedValue() {
            BankAccountNumber bankAccount = BankAccountNumber.of("CZ65 0800 0000 1920 0014 5399");
            assertThat(bankAccount.value()).isEqualTo("CZ6508000000192000145399");
        }
    }
}
