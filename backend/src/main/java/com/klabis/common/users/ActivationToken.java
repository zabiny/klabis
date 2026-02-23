package com.klabis.common.users;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing an account activation token.
 *
 * <p>Activation tokens are sent via email to verify email addresses and activate user accounts.
 * They are designed to be:
 * <ul>
 *   <li>Cryptographically secure (generated with SecureRandom)</li>
 *   <li>Time-limited (expire after configured period)</li>
 *   <li>Single-use (invalidated after activation)</li>
 * </ul>
 *
 * <p>This is an immutable value object - use {@link #generate(int)} to create new tokens.
 */
@ValueObject
public class ActivationToken {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String token;
    private final Instant expiresAt;

    /**
     * Creates an activation token with explicit expiration time.
     *
     * @param token     the token string (typically UUID)
     * @param expiresAt when this token expires
     */
    public ActivationToken(String token, Instant expiresAt) {
        this.token = Objects.requireNonNull(token, "Token is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration time is required");
    }

    public static ActivationToken newToken(String token, LocalDateTime expiresAt) {
        Assert.notNull(token, "Token is required");
        Assert.notNull(expiresAt, "Expiration time is required");
        return new ActivationToken(token, expiresAt.toInstant(ZoneOffset.UTC));
    }

    /**
     * Generates a new cryptographically secure activation token.
     *
     * @param validityHours how many hours until the token expires
     * @return new ActivationToken
     */
    public static ActivationToken generate(int validityHours) {
        if (validityHours <= 0) {
            throw new IllegalArgumentException("Validity hours must be positive");
        }

        String tokenValue = UUID.randomUUID().toString();
        Instant expiration = Instant.now().plusSeconds(validityHours * 3600L);

        return new ActivationToken(tokenValue, expiration);
    }

    /**
     * Checks if this token has expired.
     *
     * @return true if the token is past its expiration time
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if this token is still valid (not expired).
     *
     * @return true if the token has not expired yet
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Verifies this token matches the provided token string.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param tokenToVerify the token string to verify
     * @return true if tokens match and this token is not expired
     */
    public boolean verify(String tokenToVerify) {
        if (tokenToVerify == null || isExpired()) {
            return false;
        }

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                token.getBytes(),
                tokenToVerify.getBytes()
        );
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivationToken that = (ActivationToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }

    @Override
    public String toString() {
        return "ActivationToken{" +
               "token='" + token.substring(0, 8) + "...' (redacted), " +
               "expiresAt=" + expiresAt +
               '}';
    }

    /**
     * Helper class for constant-time string comparison.
     * Prevents timing attacks by always comparing all bytes.
     */
    private static class MessageDigest {
        static boolean isEqual(byte[] a, byte[] b) {
            if (a.length != b.length) {
                return false;
            }

            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }

            return result == 0;
        }
    }
}
