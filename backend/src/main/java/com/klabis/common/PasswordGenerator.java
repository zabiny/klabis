package com.klabis.common;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility for generating secure random passwords.
 */
@Component
public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_+=";
    private static final String ALL = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private static final int DEFAULT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a secure random password with default length (16 characters).
     *
     * @return generated password
     */
    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    /**
     * Generate a secure random password with specified length.
     *
     * @param length password length
     * @return generated password
     */
    public String generate(int length) {
        if (length < 12) {
            throw new IllegalArgumentException("Password length must be at least 12 characters");
        }

        StringBuilder password = new StringBuilder();

        // Ensure at least one character from each category
        password.append(UPPERCASE.charAt(RANDOM.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(RANDOM.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));

        // Fill remaining length with random characters
        for (int i = password.length(); i < length; i++) {
            password.append(ALL.charAt(RANDOM.nextInt(ALL.length())));
        }

        // Shuffle password to avoid predictable pattern
        String shuffled = shuffleString(password.toString());
        return shuffled;
    }

    private String shuffleString(String str) {
        char[] chars = str.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
