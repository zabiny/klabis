package com.klabis.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates the pattern of an Optional<String> value if present.
 * <p>
 * This constraint is used for Optional<String> fields in records
 * where Jakarta Bean Validation's @Pattern annotation doesn't work directly.
 * The validation is performed by {@link OptionalPatternValidator}.
 *
 * @see OptionalPatternValidator
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = OptionalPatternValidator.class)
@Documented
public @interface ValidOptionalPattern {

    /**
     * @return regular expression pattern to match
     */
    String regexp();

    /**
     * @return error message template
     */
    String message() default "Must match \"{regexp}\"";

    /**
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * @return payload
     */
    Class<? extends Payload>[] payload() default {};
}
