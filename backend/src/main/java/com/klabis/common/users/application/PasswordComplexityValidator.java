package com.klabis.common.users.application;

import com.klabis.common.users.domain.PasswordValidationException;
import com.klabis.common.users.domain.PasswordValidationException.Violation;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for password complexity requirements.
 *
 * <p>Ensures passwords meet security requirements:
 * <ul>
 *   <li>Minimum 12 characters</li>
 *   <li>Contains uppercase letter</li>
 *   <li>Contains lowercase letter</li>
 *   <li>Contains digit</li>
 *   <li>Contains special character</li>
 *   <li>Does not contain personal information (registration number, names)</li>
 * </ul>
 *
 * <p>This validator is in the common module as it's a pure validation utility
 * with no dependencies on specific domain models.
 */
@Component
public class PasswordComplexityValidator {

    private static final int MIN_LENGTH = PasswordValidationException.MIN_LENGTH;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`]");

    /**
     * Validates a password against complexity requirements without personal context.
     *
     * @param password the password to validate
     * @throws PasswordValidationException if validation fails
     */
    public void validateBasic(String password) {
        if (password == null || password.isBlank()) {
            throw new PasswordValidationException(Violation.REQUIRED);
        }

        Set<Violation> violations = EnumSet.noneOf(Violation.class);

        if (password.length() < MIN_LENGTH) {
            violations.add(Violation.TOO_SHORT);
        }
        if (!UPPERCASE.matcher(password).find()) {
            violations.add(Violation.MISSING_UPPERCASE);
        }
        if (!LOWERCASE.matcher(password).find()) {
            violations.add(Violation.MISSING_LOWERCASE);
        }
        if (!DIGIT.matcher(password).find()) {
            violations.add(Violation.MISSING_DIGIT);
        }
        if (!SPECIAL.matcher(password).find()) {
            violations.add(Violation.MISSING_SPECIAL_CHARACTER);
        }

        if (!violations.isEmpty()) {
            throw new PasswordValidationException(violations);
        }
    }

    /**
     * Validates a password against complexity requirements with personal information check.
     *
     * @param password           the password to validate
     * @param firstName          the user's first name (may be null)
     * @param lastName           the user's last name (may be null)
     * @param registrationNumber the user's registration number (may be null)
     * @throws PasswordValidationException if validation fails
     */
    public void validate(String password, String firstName, String lastName, String registrationNumber) {
        validateBasic(password);

        Set<Violation> violations = EnumSet.noneOf(Violation.class);
        String passwordLower = password.toLowerCase();

        if (containsIgnoringCase(passwordLower, firstName)) {
            violations.add(Violation.CONTAINS_FIRST_NAME);
        }
        if (containsIgnoringCase(passwordLower, lastName)) {
            violations.add(Violation.CONTAINS_LAST_NAME);
        }
        if (containsIgnoringCase(passwordLower, registrationNumber)) {
            violations.add(Violation.CONTAINS_REGISTRATION_NUMBER);
        }

        if (!violations.isEmpty()) {
            throw new PasswordValidationException(violations);
        }
    }

    private static boolean containsIgnoringCase(String passwordLower, String personalValue) {
        return personalValue != null && !personalValue.isBlank() && passwordLower.contains(personalValue.toLowerCase());
    }
}
