package com.klabis.common.users.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PasswordValidationException extends InvalidDataException implements UserFacingError {

    public static final int MIN_LENGTH = 12;

    public enum Violation {
        REQUIRED("Password is required"),
        TOO_SHORT("Password must be at least " + MIN_LENGTH + " characters long."),
        MISSING_UPPERCASE("Password must contain at least one uppercase letter."),
        MISSING_LOWERCASE("Password must contain at least one lowercase letter."),
        MISSING_DIGIT("Password must contain at least one digit."),
        MISSING_SPECIAL_CHARACTER("Password must contain at least one special character."),
        CONTAINS_FIRST_NAME("Password cannot contain your first name."),
        CONTAINS_LAST_NAME("Password cannot contain your last name."),
        CONTAINS_REGISTRATION_NUMBER("Password cannot contain your registration number.");

        private final String message;

        Violation(String message) {
            this.message = message;
        }
    }

    private final List<Violation> violations;

    public PasswordValidationException(Set<Violation> violations) {
        super(violations.stream()
                .sorted(Comparator.naturalOrder())
                .map(violation -> violation.message)
                .collect(Collectors.joining(" ")));
        this.violations = violations.stream().sorted(Comparator.naturalOrder()).toList();
    }

    public PasswordValidationException(Violation violation) {
        this(Set.of(violation));
    }

    @Override
    public String errorType() {
        return "password-requirements-not-met";
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of(
                "violations", violations.stream().map(Violation::name).toList(),
                "minLength", MIN_LENGTH);
    }
}
