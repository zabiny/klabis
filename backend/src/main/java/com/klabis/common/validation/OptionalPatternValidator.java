package com.klabis.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validator for {@link ValidOptionalPattern} constraint.
 * <p>
 * Validates the pattern of an Optional<String> value if present.
 * If the Optional is empty, validation passes.
 * If the Optional contains a value, the value is checked against the regular expression.
 */
class OptionalPatternValidator implements ConstraintValidator<ValidOptionalPattern, Optional<String>> {

    private Pattern pattern;

    @Override
    public void initialize(ValidOptionalPattern constraintAnnotation) {
        this.pattern = Pattern.compile(constraintAnnotation.regexp());
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

        return pattern.matcher(stringValue).matches();
    }
}
