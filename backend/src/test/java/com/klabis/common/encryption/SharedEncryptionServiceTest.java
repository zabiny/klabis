package com.klabis.common.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SharedEncryptionService")
class SharedEncryptionServiceTest {

    private static final String SALT = "5c0744940b5c369b";

    private final SharedEncryptionService service = new SharedEncryptionService("test-password", SALT);

    @Test
    @DisplayName("should decrypt to original plaintext")
    void shouldRoundTrip() {
        String encrypted = service.encrypt("900101/1235");

        assertThat(encrypted).isNotEqualTo("900101/1235");
        assertThat(service.decrypt(encrypted)).isEqualTo("900101/1235");
    }

    @Test
    @DisplayName("should round-trip non-ASCII text")
    void shouldRoundTripUnicode() {
        assertThat(service.decrypt(service.encrypt("Příliš žluťoučký kůň"))).isEqualTo("Příliš žluťoučký kůň");
    }

    @Test
    @DisplayName("should return null for null input")
    void shouldHandleNull() {
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    @DisplayName("should produce different ciphertext for same plaintext")
    void shouldBeNonDeterministic() {
        assertThat(service.encrypt("same")).isNotEqualTo(service.encrypt("same"));
    }

    @Test
    @DisplayName("should produce hex output")
    void shouldProduceHex() {
        assertThat(service.encrypt("value")).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("should fail to decrypt with a different password")
    void shouldFailWithWrongPassword() {
        String encrypted = service.encrypt("secret");
        SharedEncryptionService other = new SharedEncryptionService("other-password", SALT);

        assertThatThrownBy(() -> other.decrypt(encrypted)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("should fail to decrypt tampered ciphertext")
    void shouldFailOnTamperedCiphertext() {
        String encrypted = service.encrypt("secret");
        String last = encrypted.substring(encrypted.length() - 1);
        String tampered = encrypted.substring(0, encrypted.length() - 1) + (last.equals("0") ? "1" : "0");

        assertThatThrownBy(() -> service.decrypt(tampered)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("should fail to decrypt with the same password but a different salt")
    void shouldFailWithDifferentSalt() {
        String encrypted = service.encrypt("secret");
        SharedEncryptionService other = new SharedEncryptionService("test-password", "00112233445566778899aabbccddeeff");

        assertThatThrownBy(() -> other.decrypt(encrypted)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("should accept a 32 character hex salt")
    void shouldAcceptLongHexSalt() {
        SharedEncryptionService longSalt = new SharedEncryptionService("test-password", "00112233445566778899AABBCCDDEEFF");

        assertThat(longSalt.decrypt(longSalt.encrypt("value"))).isEqualTo("value");
    }

    @ParameterizedTest(name = "should reject invalid salt ''{0}''")
    @ValueSource(strings = {"", "   ", "not-hex-at-all!!", "5c0744940b5c369", "zz"})
    void shouldRejectInvalidSalt(String salt) {
        assertThatThrownBy(() -> new SharedEncryptionService("test-password", salt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("klabis.encryption.salt")
                .hasMessageContaining("hex");
    }

    @Test
    @DisplayName("should reject null salt")
    void shouldRejectNullSalt() {
        assertThatThrownBy(() -> new SharedEncryptionService("test-password", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("klabis.encryption.salt");
    }
}
