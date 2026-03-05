package com.klabis.common.encryption;

import java.util.Objects;

/**
 * Value object for encrypted string data.
 * <p>
 * This type is used for fields that should be encrypted at rest in the database.
 * Spring Data JDBC converters handle encryption/decryption transparently.
 */
public record EncryptedString(String value) {

    public EncryptedString {
        Objects.requireNonNull(value, "Encrypted value cannot be null");
    }

    public static EncryptedString of(String value) {
        return new EncryptedString(value);
    }

    @Override
    public String toString() {
        return "***";
    }
}
