package com.klabis.common.validation;

import com.klabis.common.patch.PatchField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class PatchFieldNotBlankValidator implements ConstraintValidator<ValidPatchFieldNotBlank, PatchField<String>> {

    @Override
    public boolean isValid(PatchField<String> value, ConstraintValidatorContext context) {
        if (value == null || !value.isProvided()) {
            return true;
        }

        String stringValue = value.throwIfNotProvided();
        return stringValue != null && !stringValue.trim().isEmpty();
    }
}
