package com.klabis.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Optional;

/**
 * Validator for {@link ValidOptionalSize} constraint.
 * <p>
 * Validates the size of an Optional<String> value if present.
 * If the Optional is empty, validation passes.
 * If the Optional contains a value, the value's length is checked against min/max bounds.
 */
class OptionalSizeValidator implements ConstraintValidator<ValidOptionalSize, Optional<String>> {

    private int min;
    private int max;

    @Override
    public void initialize(ValidOptionalSize constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(Optional<String> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // Empty optional is valid
        }

        String stringValue = value.get();
        if (stringValue == null) {
            return true;
        }

        int length = stringValue.length();
        return length >= min && length <= max;
    }
}
