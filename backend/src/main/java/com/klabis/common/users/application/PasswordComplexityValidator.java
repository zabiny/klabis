package com.klabis.common.users.application;

import com.klabis.common.users.domain.PasswordValidationException;
import org.springframework.stereotype.Component;

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

    private static final int MIN_LENGTH = 12;
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
            throw new PasswordValidationException("Password is required");
        }

        StringBuilder errors = new StringBuilder();

        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            errors.append("Password must be at least ").append(MIN_LENGTH).append(" characters long. ");
        }

        // Check uppercase
        if (!UPPERCASE.matcher(password).find()) {
            errors.append("Password must contain at least one uppercase letter. ");
        }

        // Check lowercase
        if (!LOWERCASE.matcher(password).find()) {
            errors.append("Password must contain at least one lowercase letter. ");
        }

        // Check digit
        if (!DIGIT.matcher(password).find()) {
            errors.append("Password must contain at least one digit. ");
        }

        // Check special character
        if (!SPECIAL.matcher(password).find()) {
            errors.append("Password must contain at least one special character. ");
        }

        if (errors.length() > 0) {
            throw new PasswordValidationException(errors.toString().trim());
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
        // First do basic validation
        validateBasic(password);

        StringBuilder errors = new StringBuilder();
        String passwordLower = password.toLowerCase();

        // Check first name
        if (firstName != null && !firstName.isBlank() &&
            passwordLower.contains(firstName.toLowerCase())) {
            errors.append("Password cannot contain your first name. ");
        }

        // Check last name
        if (lastName != null && !lastName.isBlank() &&
            passwordLower.contains(lastName.toLowerCase())) {
            errors.append("Password cannot contain your last name. ");
        }

        // Check registration number
        if (registrationNumber != null && !registrationNumber.isBlank() &&
            passwordLower.contains(registrationNumber.toLowerCase())) {
            errors.append("Password cannot contain your registration number. ");
        }

        if (errors.length() > 0) {
            throw new PasswordValidationException(errors.toString().trim());
        }
    }

}
