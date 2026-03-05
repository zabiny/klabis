package com.klabis.common.validation;

import com.klabis.common.patch.PatchField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link ValidPatchFieldSize} constraint.
 * <p>
 * Validates the size of a PatchField<String> value if provided.
 * If the PatchField is not provided or contains a null value, validation passes.
 * If the PatchField contains a value, the value's length is checked against min/max bounds.
 */
class PatchFieldSizeValidator implements ConstraintValidator<ValidPatchFieldSize, PatchField<String>> {

    private int min;
    private int max;

    @Override
    public void initialize(ValidPatchFieldSize constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(PatchField<String> value, ConstraintValidatorContext context) {
        if (value == null || !value.isProvided()) {
            return true; // Not provided is valid
        }

        String stringValue = value.throwIfNotProvided();
        if (stringValue == null) {
            return true; // Null value is valid
        }

        int length = stringValue.length();
        return length >= min && length <= max;
    }
}