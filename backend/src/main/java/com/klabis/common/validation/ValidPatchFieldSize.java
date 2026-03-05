package com.klabis.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates the size of a PatchField<String> value if provided.
 * <p>
 * This constraint is used for PatchField<String> fields where
 * Jakarta Bean Validation's @Size annotation doesn't work directly.
 * The validation is performed by {@link PatchFieldSizeValidator}.
 *
 * @see PatchFieldSizeValidator
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = PatchFieldSizeValidator.class)
@Documented
public @interface ValidPatchFieldSize {

    /**
     * @return maximum allowed length
     */
    int max();

    /**
     * @return minimum allowed length (default 0)
     */
    int min() default 0;

    /**
     * @return error message template
     */
    String message() default "Size must be between {min} and {max}";

    /**
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * @return payload
     */
    Class<? extends Payload>[] payload() default {};
}