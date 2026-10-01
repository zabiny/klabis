package com.klabis.common.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
