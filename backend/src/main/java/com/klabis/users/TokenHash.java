package com.klabis.users;

import org.jmolecules.ddd.annotation.ValueObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Value Object representing a SHA-256 hash of a password setup token.
 *
 * <p>Tokens are never stored in plain text in the database.
 * This value object encapsulates the hashed token and provides
 * secure comparison to prevent timing attacks.
 *
 * <p>SHA-256 is sufficient for token hashing because:
 * <ul>
 *   <li>Tokens are random UUIDs (not user passwords)</li>
 *   <li>No need for key stretching (tokens have short lifetime)</li>
 *   <li>Fast computation (no performance impact)</li>
 * </ul>
 *
 * <p>Immutable value object with secure comparison.
 */
@ValueObject
public final class TokenHash {

    private static final int HASH_LENGTH = 64; // SHA-256 produces 64 hex characters
    private static final String HASH_ALGORITHM = "SHA-256";

    private final String value;

    /**
     * Creates a TokenHash from a pre-hashed string.
     *
     * <p>Use {@link #hash(String)} factory method to hash a plain token.
     *
     * @param value the hashed token value (64 hex characters)
     * @throws IllegalArgumentException if value is invalid
     */
    private TokenHash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Token hash is required");
        }

        String trimmed = value.trim();

        if (trimmed.length() != HASH_LENGTH) {
            throw new IllegalArgumentException(
                    "Token hash must be exactly " + HASH_LENGTH + " characters"
            );
        }

        if (!isValidHex(trimmed)) {
            throw new IllegalArgumentException(
                    "Token hash must contain only hexadecimal characters"
            );
        }

        this.value = trimmed;
    }

    /**
     * Factory method to hash a plain text token.
     *
     * @param plainToken the plain text token to hash
     * @return new TokenHash containing the SHA-256 hash
     * @throws IllegalArgumentException if plainToken is null or empty
     */
    public static TokenHash hash(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException("Plain token is required");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return new TokenHash(hexString.toString());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Creates a TokenHash from an already hashed string.
     *
     * @param hashedValue the hashed token value (64 hex characters)
     * @return new TokenHash
     */
    public static TokenHash fromHashedValue(String hashedValue) {
        return new TokenHash(hashedValue);
    }

    /**
     * Checks if the given plain token matches this hash.
     *
     * <p>Uses constant-time comparison to prevent timing attacks.
     *
     * @param plainToken the plain text token to check
     * @return true if the hash of plainToken equals this hash
     */
    public boolean matches(String plainToken) {
        if (plainToken == null) {
            return false;
        }

        TokenHash candidateHash = hash(plainToken);
        return secureEquals(this.value, candidateHash.value);
    }

    /**
     * Gets the hashed token value.
     *
     * @return 64-character hexadecimal string
     */
    public String getValue() {
        return value;
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * @param a first string
     * @param b second string
     * @return true if strings are equal
     */
    private boolean secureEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }

    /**
     * Checks if a string is a valid hexadecimal string.
     */
    private boolean isValidHex(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c) && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenHash tokenHash = (TokenHash) o;
        return Objects.equals(value, tokenHash.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        // Don't expose the full hash for security
        return "TokenHash[********%s]".formatted(value.substring(value.length() - 8));
    }
}
