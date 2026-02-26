package com.klabis.common.validation;

import com.klabis.common.patch.PatchField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

class PatchFieldPatternValidator implements ConstraintValidator<ValidPatchFieldPattern, PatchField<String>> {

    private Pattern pattern;

    @Override
    public void initialize(ValidPatchFieldPattern constraintAnnotation) {
        this.pattern = Pattern.compile(constraintAnnotation.regexp());
    }

    @Override
    public boolean isValid(PatchField<String> value, ConstraintValidatorContext context) {
        if (value == null || !value.isProvided()) {
            return true;
        }

        String stringValue = value.throwIfNotProvided();
        if (stringValue == null) {
            return true;
        }

        return pattern.matcher(stringValue).matches();
    }
}
