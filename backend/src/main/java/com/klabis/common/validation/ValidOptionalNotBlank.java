package com.klabis.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates that an Optional<String> value is not blank if present.
 * <p>
 * This constraint is used for Optional<String> fields in records
 * where Jakarta Bean Validation's @NotBlank annotation doesn't work directly.
 * The validation is performed by {@link OptionalNotBlankValidator}.
 *
 * @see OptionalNotBlankValidator
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = OptionalNotBlankValidator.class)
@Documented
public @interface ValidOptionalNotBlank {

    /**
     * @return error message template
     */
    String message() default "Must not be blank";

    /**
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * @return payload
     */
    Class<? extends Payload>[] payload() default {};
}
