package com.klabis.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates the size of an Optional<String> value if present.
 * <p>
 * This constraint is used for Optional<String> fields in records
 * where Jakarta Bean Validation's @Size annotation doesn't work directly.
 * The validation is performed by {@link OptionalSizeValidator}.
 *
 * @see OptionalSizeValidator
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = OptionalSizeValidator.class)
@Documented
public @interface ValidOptionalSize {

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
