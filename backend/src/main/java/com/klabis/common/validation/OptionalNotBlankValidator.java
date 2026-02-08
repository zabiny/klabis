package com.klabis.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Optional;

/**
 * Validator for {@link ValidOptionalNotBlank} constraint.
 * <p>
 * Validates that an Optional<String> value is not blank if present.
 * An Optional that is empty passes validation (null is allowed).
 * An Optional that contains a blank value (null, empty, or only whitespace) fails validation.
 */
class OptionalNotBlankValidator implements ConstraintValidator<ValidOptionalNotBlank, Optional<String>> {

    @Override
    public boolean isValid(Optional<String> value, ConstraintValidatorContext context) {
        // Empty Optional is valid (field is not required)
        if (value == null || value.isEmpty()) {
            return true;
        }

        // If value is present, it must not be blank
        String stringValue = value.get();
        return stringValue != null && !stringValue.trim().isEmpty();
    }
}
