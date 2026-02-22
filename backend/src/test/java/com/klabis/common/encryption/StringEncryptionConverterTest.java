package com.klabis.common.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EncryptedString Converter Tests")
class StringEncryptionConverterTest {

    private SharedEncryptionService encryptionService;
    private EncryptedStringToStringConverter encryptionConverter;
    private StringToEncryptedStringConverter decryptionConverter;

    @BeforeEach
    void setUp() {
        encryptionService = new SharedEncryptionService("test-password-for-unit-tests", "PBEWithMD5AndDES");
        encryptionConverter = new EncryptedStringToStringConverter(encryptionService);
        decryptionConverter = new StringToEncryptedStringConverter(encryptionService);
    }

    @Nested
    @DisplayName("Encryption")
    class Encryption {

        @Test
        @DisplayName("should encrypt non-null string")
        void shouldEncryptNonNullString() {
            String plaintext = "900101/1235";
            EncryptedString source = EncryptedString.of(plaintext);

            String encrypted = encryptionConverter.convert(source);

            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotEqualTo(plaintext);
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            String encrypted = encryptionConverter.convert(null);

            assertThat(encrypted).isNull();
        }

        @Test
        @DisplayName("should produce different encrypted values for same input (due to random IV)")
        void shouldProduceDifferentEncryptedValuesForSameInput() {
            String plaintext = "900101/1235";
            EncryptedString source = EncryptedString.of(plaintext);

            String encrypted1 = encryptionConverter.convert(source);
            String encrypted2 = encryptionConverter.convert(source);

            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("should handle empty string")
        void shouldHandleEmptyString() {
            EncryptedString source = EncryptedString.of("");
            String encrypted = encryptionConverter.convert(source);

            assertThat(encrypted).isNotNull();
        }
    }

    @Nested
    @DisplayName("Decryption")
    class Decryption {

        @Test
        @DisplayName("should decrypt encrypted string to original value")
        void shouldDecryptEncryptedStringToOriginalValue() {
            String plaintext = "900101/1235";
            EncryptedString source = EncryptedString.of(plaintext);

            String encrypted = encryptionConverter.convert(source);
            EncryptedString decrypted = decryptionConverter.convert(encrypted);

            assertThat(decrypted.value()).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            EncryptedString decrypted = decryptionConverter.convert(null);

            assertThat(decrypted).isNull();
        }

        @Test
        @DisplayName("should handle empty string")
        void shouldHandleEmptyString() {
            EncryptedString source = EncryptedString.of("");
            String encrypted = encryptionConverter.convert(source);
            EncryptedString decrypted = decryptionConverter.convert(encrypted);

            assertThat(decrypted.value()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Round-trip")
    class RoundTrip {

        @Test
        @DisplayName("should maintain original value after encrypt-decrypt cycle")
        void shouldMaintainOriginalValueAfterEncryptDecryptCycle() {
            String original = "900101/1235";
            EncryptedString source = EncryptedString.of(original);

            String encrypted = encryptionConverter.convert(source);
            EncryptedString decrypted = decryptionConverter.convert(encrypted);

            assertThat(decrypted.value()).isEqualTo(original);
        }

        @Test
        @DisplayName("should handle Czech birth number format")
        void shouldHandleCzechBirthNumberFormat() {
            String birthNumber = "900101/1235";
            EncryptedString source = EncryptedString.of(birthNumber);

            String encrypted = encryptionConverter.convert(source);
            EncryptedString decrypted = decryptionConverter.convert(encrypted);

            assertThat(decrypted.value()).isEqualTo(birthNumber);
        }

        @Test
        @DisplayName("should handle birth number without slash")
        void shouldHandleBirthNumberWithoutSlash() {
            String birthNumber = "9001011235";
            EncryptedString source = EncryptedString.of(birthNumber);

            String encrypted = encryptionConverter.convert(source);
            EncryptedString decrypted = decryptionConverter.convert(encrypted);

            assertThat(decrypted.value()).isEqualTo(birthNumber);
        }

        @Test
        @DisplayName("should handle birth number for women (month 51-62)")
        void shouldHandleBirthNumberForWomen() {
            String birthNumber = "905101/1239";
            EncryptedString source = EncryptedString.of(birthNumber);

            String encrypted = encryptionConverter.convert(source);
            EncryptedString decrypted = decryptionConverter.convert(encrypted);

            assertThat(decrypted.value()).isEqualTo(birthNumber);
        }

        @Test
        @DisplayName("should handle birth number for women 2000+ (month 21-32)")
        void shouldHandleBirthNumberForWomen2000s() {
            String birthNumber = "002201/1235";
            EncryptedString source = EncryptedString.of(birthNumber);

            String encrypted = encryptionConverter.convert(source);
            EncryptedString decrypted = decryptionConverter.convert(encrypted);

            assertThat(decrypted.value()).isEqualTo(birthNumber);
        }
    }
}
